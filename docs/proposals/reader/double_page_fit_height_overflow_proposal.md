# Technical Proposal: Double-Page Fit Height Overflow & Aspect Ratio Scaling Engine

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling & Dual-Page Engine  
**Execution Order:** Reader Track — Phase R5 (Dual-Page Engine & Layout Scaling), Step R16 (Priority: High / Scale Type Integrity)  
**Resolves Issue:** [GitHub Issue #3364](https://github.com/nekomangaorg/Neko/issues/3364) ("Double pages + fit height crops pages")  
**Prerequisites:** Step R5 ([`rock_solid_paged_compose_viewer_proposal.md`](rock_solid_paged_compose_viewer_proposal.md)), Step R15 ([`double_page_tablet_auto_zoom_proposal.md`](double_page_tablet_auto_zoom_proposal.md))  
**Downstream Dependents:** None  
**Implementation Targets:** [`DoublePageLayout.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt), [`PagerViewerConfigUiModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerViewerConfigUiModel.kt)  

---

## 📌 Baseline Audit & Problem Statement

### 1.1 Context & Reproduction
On Samsung Galaxy Z Fold (e.g. Fold 7, Fold 6) and tablet devices:
1. The user sets Page Layout to **"Double pages"**.
2. The user sets Scale Type to **"Fit height"** to ensure manga art maximizes the full vertical screen height.
3. Instead of scaling the dual pages to match the screen height with horizontal panning for any overflow, the pages are either:
   - Forcibly scaled down to fit inside the horizontal width (defeating "Fit height"), or
   - Cropped on the top, bottom, or sides to fit both dimensions simultaneously.

### 1.2 Root Cause Analysis
In [`DoublePageLayout.kt#L231-L248`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt#L231-L248):
```kotlin
val insideScale =
    if (
        totalWidth > 0f && h > 0f && viewportWidthPx > 0f && viewportHeightPx > 0f
    ) {
        minOf(1f, viewportWidthPx / totalWidth, viewportHeightPx / h)
    } else {
        1f
    }
w1Dp = with(density) { (w1 * insideScale).toDp() }
w2Dp = with(density) { (w2 * insideScale).toDp() }
hDp = with(density) { (h * insideScale).toDp() }
effectiveGapDp = with(density) { (gapPx * insideScale).toDp() }
```

**The Breakdown:**
1. **Hardcoded Scale Clamping**:
   - `DoublePageLayout` computes a single `insideScale` factor via `minOf(1f, viewportWidthPx / totalWidth, viewportHeightPx / h)`.
   - This math enforces a **"Fit Screen" (Center Inside)** constraint regardless of what scale type the user chose in settings.
2. **Failure of "Fit Height" Mode**:
   - In "Fit height" mode (`config.imageScaleType == 2`), the correct behavior is:
     $$\text{scale} = \frac{\text{viewportHeightPx}}{h}$$
   - When two portrait pages are placed side by side, their combined width $\text{totalWidth} \times \text{scale}$ frequently exceeds the screen width $\text{viewportWidthPx}$.
   - Because `DoublePageLayout` applies `minOf(..., viewportWidthPx / totalWidth, ...)`, it clamps the height to a reduced fraction:
     $$h_{\text{rendered}} = h \times \frac{\text{viewportWidthPx}}{\text{totalWidth}} \ll \text{viewportHeightPx}$$
   - The user is left with shrunken pages surrounded by black bars on top and bottom, completely ignoring the "Fit height" preference.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Config ["User Configuration & Viewport"]
        ScaleType["config.imageScaleType (Fit Height, Fit Width, Fit Screen)"]
        Dims["Composite Dimensions: totalWidth, height (h)"]
        Viewport["Viewport: viewportWidthPx, viewportHeightPx"]
    end

    subgraph ScaleEngine ["Dual-Page Scale Calculator"]
        ScaleType --> Calc{"Scale Mode Evaluation"}
        Calc -->|Fit Height (ScaleType == 2)| ScaleH["scale = viewportHeightPx / h\nAllow totalWidth to Overflow Horizontally"]
        Calc -->|Fit Width (ScaleType == 1)| ScaleW["scale = viewportWidthPx / totalWidth\nAllow height to Overflow Vertically"]
        Calc -->|Fit Screen (ScaleType == 0)| ScaleS["scale = minOf(1.0, viewportWidthPx / totalWidth, viewportHeightPx / h)"]
        Calc -->|Original (ScaleType == 3)| ScaleO["scale = 1.0"]
    end

    subgraph Render ["Composable Rendering & Pan Coordination"]
        ScaleH --> RenderRow["Row(modifier = wrapContentSize(unbounded = true))\nw1Dp = w1 * scale, w2Dp = w2 * scale, hDp = viewportHeightDp"]
        RenderRow --> Telephoto["zoomableState.setContentLocation(ComposeSize(totalWidth * scale, viewportHeightPx))"]
        Telephoto --> PanAlignment["Align to Reading Start Edge (LTR: Left, RTL: Right)\nEnable Horizontal Pan Across Overflow"]
    end
```

---

## 3. Technical Specifications

### 3.1 Scale-Type Aware Dimensions Calculator

Replace hardcoded `minOf` scaling in `DoublePageLayout.kt` with a dynamic calculator honoring `config.imageScaleType`:

```kotlin
// In DoublePageLayout.kt
val (effectiveScale, imageContentScale) = remember(
    config.imageScaleType,
    totalWidth,
    h,
    viewportWidthPx,
    viewportHeightPx,
) {
    if (totalWidth <= 0f || h <= 0f || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
        return@remember Pair(1f, ContentScale.Fit)
    }

    when (config.imageScaleType) {
        // Fit Height (ScaleType == 2 or PagerConfig equivalent)
        2 -> {
            val scale = viewportHeightPx / h
            Pair(scale, ContentScale.FillHeight)
        }
        // Fit Width (ScaleType == 1)
        1 -> {
            val scale = viewportWidthPx / totalWidth
            Pair(scale, ContentScale.FillWidth)
        }
        // Original Size (ScaleType == 3)
        3 -> {
            Pair(1f, ContentScale.None)
        }
        // Fit Screen / Center Inside (Default / ScaleType == 0)
        else -> {
            val scale = minOf(1f, viewportWidthPx / totalWidth, viewportHeightPx / h)
            Pair(scale, ContentScale.Fit)
        }
    }
}

val w1Dp = with(density) { (w1 * effectiveScale).toDp() }
val w2Dp = with(density) { (w2 * effectiveScale).toDp() }
val hDp = with(density) { (h * effectiveScale).toDp() }
val effectiveGapDp = with(density) { (gapPx * effectiveScale).toDp() }
```

### 3.2 Telephoto Content Location & Initial Pan Positioning

When "Fit height" causes horizontal overflow ($\text{totalWidth} \times \text{effectiveScale} > \text{viewportWidthPx}$):
1. **Unbounded Row Layout**: The `Row` container must wrap unbounded content:
   ```kotlin
   Row(
       modifier = Modifier.wrapContentSize(align = Alignment.Center, unbounded = true),
       horizontalArrangement = Arrangement.spacedBy(effectiveGapDp),
       verticalAlignment = Alignment.CenterVertically,
   )
   ```
2. **Initial Alignment to Reading Start**:
   - In LTR: Align the composite content to the left (`Alignment.CenterStart`), so the user starts reading page 1 at the left edge and pans right to page 2.
   - In RTL: Align the composite content to the right (`Alignment.CenterEnd`), so the user starts reading page 1 at the right edge and pans left to page 2.
   ```kotlin
   val initialAlignment = remember(config.isRtl, config.invertDoublePages) {
       val isRtl = config.isRtl.xor(config.invertDoublePages)
       if (isRtl) Alignment.CenterEnd else Alignment.CenterStart
   }
   zoomableState.setContentLocation(
       ZoomableContentLocation.unscaledAndAligned(initialAlignment)
   )
   ```

---

## 4. Key Guarantees & Edge Cases

1. **Pixel-Perfect Full-Height Reading**: In "Fit height" mode, pages completely touch the top and bottom screen margins without letterboxing, preserving maximum art clarity on foldables and tablets.
2. **Zero Distortion**: Images retain their native aspect ratios without distortion or clipping.
3. **Seamless Pan Interaction**: Telephoto smoothly scrolls the horizontal overflow without getting trapped or bouncing back.

---

## 5. Test Specifications

1. **Fit Height Overflow Test**:
   - Viewport: $1600 \times 1200$ (width 1600, height 1200).
   - Render two pages with dimensions $1000 \times 1500$ each ($h = 1500$, $totalWidth = 2000$).
   - In "Fit Height" mode:
     - Effective scale should equal $1200 / 1500 = 0.8$.
     - Composite width should equal $2000 \times 0.8 = 1600\text{px}$.
     - Composite height should equal $1500 \times 0.8 = 1200\text{px}$ (exactly fitting viewport height).
2. **Wide Dual Page Overflow Test**:
   - Viewport: $1080 \times 1920$.
   - Two pages $1000 \times 1000$ each ($h = 1000$, $totalWidth = 2000$).
   - In "Fit Height" mode:
     - Scale $= 1920 / 1000 = 1.92$.
     - Rendered height $= 1920\text{px}$.
     - Rendered width $= 3840\text{px}$.
     - Overflow $= 3840 - 1080 = 2760\text{px}$ horizontally scrollable.
