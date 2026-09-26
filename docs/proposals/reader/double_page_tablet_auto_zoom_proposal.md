# Technical Proposal: Double-Page Spread Auto-Zoom Disambiguation & Tablet Landscape Stabilization

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling & Dual-Page Engine  
**Execution Order:** Reader Track — Phase R5 (Dual-Page Engine & Layout Scaling), Step R15 (Priority: High / Tablet Experience & Dual-Page Layout)  
**Resolves Issue:** [GitHub Issue #3421](https://github.com/nekomangaorg/Neko/issues/3421) ("Zoomed in reader when in double page")  
**Prerequisites:** Step R5 ([`rock_solid_paged_compose_viewer_proposal.md`](rock_solid_paged_compose_viewer_proposal.md))  
**Downstream Dependents:** Step R16 ([`double_page_fit_height_overflow_proposal.md`](double_page_fit_height_overflow_proposal.md))  
**Implementation Targets:** [`DoublePageLayout.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt), [`PagerViewerConfigUiModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerViewerConfigUiModel.kt), [`ReaderSettingsSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderSettingsSheet.kt)  

---

## 📌 Baseline Audit & Problem Statement

### 1.1 Context & Reproduction
On large-screen devices such as Android tablets (e.g., Lenovo Tab P12, Samsung Galaxy Tab S9) and foldable devices in unfolded landscape mode:
1. The user opens a manga chapter in landscape orientation.
2. The user sets Page Layout to **"Double pages"** to read two portrait pages side-by-side like an open book.
3. Instead of displaying both pages cleanly fitted within the screen, the reader automatically zooms in into one side of the dual page, cutting off the opposite page.

### 1.2 Root Cause Analysis
In [`DoublePageLayout.kt#L132-L182`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt#L132-L182):
```kotlin
if (
    !autoZoomApplied &&
        isReady &&
        config.landscapeZoom &&
        config.imageScaleType == 1 &&
        viewportWidthPx > 0f &&
        viewportHeightPx > 0f
) {
    ...
    if (bounds != null) {
        val isLandscape = bounds.width > bounds.height
        if (isLandscape && bounds.height < viewportHeightPx) {
            val targetScale = (viewportHeightPx / bounds.height).coerceIn(1f, 3f)
            if (targetScale > 1.05f) {
                ...
                zoomableState.zoomTo(
                    zoomFactor = targetScale,
                    centroid = centroid,
                )
            }
        }
    }
    autoZoomApplied = true
}
```

**The Breakdown:**
1. **Semantic Misalignment of `landscapeZoom`**:
   - `landscapeZoom` ("Zoom double-page spreads" in settings) was originally designed for **single-page reading mode**, where a single image is a two-page wide landscape illustration (`width > height`). For single-page readers on phones, auto-zooming into half the wide image and letting the user pan across it provides a better reading experience than displaying tiny letterboxed art.
   - However, in [`DoublePageLayout.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt), the layout pairs two normal portrait pages together. Because two portrait pages side-by-side produce a composite width greater than height ($2w > h$), `isLandscape` is **always true**.
2. **Aggressive Zoom on Tablet Aspect Ratios**:
   - On wide 16:10 or 16:9 tablets, two portrait pages fitted inside the viewport width will not reach the full vertical height of the screen (`bounds.height < viewportHeightPx`).
   - `DoublePageLayout` interprets this as an unzoomed landscape spread and fires `zoomableState.zoomTo(...)` with `targetScale = viewportHeightPx / bounds.height`.
   - This scales the composite layout past the screen width, cropping out half of the spread and forcing the user to pan horizontally back and forth on every single page turn.
3. **Dead Configuration Property**:
   - [`PagerViewerConfigUiModel`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerViewerConfigUiModel.kt) defines `val zoomDoublePageSpreads: Boolean = false`, but `DoublePageLayout.kt` never reads it, instead checking `config.landscapeZoom`.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Input ["Configuration & Device Context"]
        Dual["DoublePageLayout (page1, page2)"]
        Config["PagerViewerConfigUiModel"]
        Device["Device Form Factor / Viewport Aspect Ratio"]
    end

    subgraph Inspection ["Spread Composition Inspection"]
        Dual --> IsSynthetic{"Is Synthetic Pair?\n(Two Standard Portrait Pages)"}
        Dual --> IsTrueSpread{"Is True Wide Spread?\n(Single Split Wide Page)"}
    end

    subgraph Decision ["Zoom Decision Matrix"]
        IsSynthetic -->|Yes| FitScreen["Fit Spread Inside Viewport (Scale = 1.0)\nNo Auto-Zoom; Full Dual-Page Visibility"]
        IsTrueSpread --> CheckPref{"config.zoomDoublePageSpreads\nEnabled?"}
        CheckPref -->|No| FitScreen
        CheckPref -->|Yes| CheckAspect{"Viewport Aspect Ratio\nWide (Tablet/Foldable)?"}
        CheckAspect -->|Tablet (Aspect >= 1.33)| FitScreen
        CheckAspect -->|Phone (Aspect < 1.33)| AutoZoom["Apply Centered/Directional Zoom\nto Active Spread Half"]
    end
```

---

## 3. Technical Specifications

### 3.1 Disambiguate Synthetic Pairs from Wide Spreads

In `DoublePageLayout.kt`, distinguish between:
1. **Synthetic Dual Page**: Two independent standard portrait pages placed side-by-side because the user chose "Double pages" layout mode.
2. **True Dual Spread**: A single landscape image that was split or detected as a continuous double-page illustration.

```kotlin
// In DoublePageLayout.kt
val isTrueSpread = first.fullPage == true || second.fullPage == true || 
                   first.isWide == true || second.isWide == true

val shouldAutoZoom = remember(
    config.zoomDoublePageSpreads,
    config.imageScaleType,
    isTrueSpread,
    viewportWidthPx,
    viewportHeightPx,
) {
    if (!config.zoomDoublePageSpreads) return@remember false
    if (config.imageScaleType != 1) return@remember false
    if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return@remember false
    
    val screenAspectRatio = viewportWidthPx / viewportHeightPx
    // Tablets and wide landscape viewports (>= 16:10 or 4:3 landscape) have sufficient space
    // to display both pages without zooming in.
    val isTabletLandscape = screenAspectRatio >= 1.33f
    
    // Only auto-zoom true wide spreads on narrow/phone screens
    isTrueSpread && !isTabletLandscape
}
```

### 3.2 Wire `zoomDoublePageSpreads` in Reader Config

1. Update [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt):
   Wire `zoomDoublePageSpreads` directly from `ReaderPreferences.landscapeZoom()` or a dedicated `zoomDoublePageSpreads()` preference.
2. In `DoublePageLayout.kt`, replace references to `config.landscapeZoom` with `shouldAutoZoom`:
   ```kotlin
   LaunchedEffect(
       firstSize,
       secondSize,
       contentScale,
       doublePageAlignment,
       shouldAutoZoom,
       isReady,
       viewportWidthPx,
       viewportHeightPx,
   ) {
       ...
       if (!autoZoomApplied && isReady && shouldAutoZoom) {
           ...
       }
   }
   ```

---

## 4. Key Guarantees & Edge Cases

1. **Pristine Tablet Reading Experience**: On tablets in landscape mode with "Double pages" enabled, two standard portrait pages always render at 1.0x scale (fit inside screen), side-by-side with full visibility of both pages.
2. **Phone/Foldable Compatibility**: Foldable devices unfolded in landscape are treated as tablets (aspect $\ge 1.33$), ensuring books and manga spreads display completely without unintended cropping.
3. **No Unwanted Horizontal Panning**: Users no longer have to drag or pan horizontally on every page turn when reading in double-page mode.

---

## 5. Test Specifications

1. **Tablet Landscape Synthetic Double-Page Test**:
   - Viewport: $2944 \times 1840$ (aspect 1.6).
   - Render two portrait pages ($1000 \times 1400$ each).
   - Assert `zoomableState.zoomFactor` remains **1.0f** (no auto-zoom).
   - Assert entire composite width fits inside $2944\text{px}$.
2. **Phone Portrait Split Page Spread Test**:
   - Viewport: $1080 \times 2400$.
   - Render wide spread split page.
   - Assert `zoomableState.zoomFactor` zooms to fit height with appropriate centroid.
