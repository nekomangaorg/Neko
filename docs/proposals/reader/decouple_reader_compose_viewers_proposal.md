# Technical Proposal: Decoupling ComposePagerViewer & ComposeWebtoonViewer from Legacy Android Views & Direct Injections

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Passing legacy View instances, DownloadManager, and Injekt inside Compose Viewers)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt) and [`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt), the core reader viewers pass legacy Android View references, platform managers, and Service Locator lookups directly into Composables:
> - **In `ComposePagerViewer.kt` (lines 53–64 & 147)**:
>   - Passes legacy View/Controller instance directly: `viewer: PagerViewer`.
>   - Passes background service directly: `downloadManager: DownloadManager`.
>   - Calls Service Locator inside Compose: `val readerPreferences: ReaderPreferences = remember { Injekt.get() }`.
>   - Collects multiple preferences individually inside the Composable render body: `readerTheme`, `cropBorders`, `navigateToPan`, `landscapeZoom`.
> - **In `ComposeWebtoonViewer.kt` (lines 84–93 & 128)**:
>   - Passes legacy View/Controller instance directly: `viewer: WebtoonViewer`.
>   - Passes background service directly: `downloadManager: DownloadManager`.
>   - Calls Service Locator inside Compose: `val readerPreferences: ReaderPreferences = remember { Injekt.get() }`.
>   - Collects preferences directly: `webtoonSidePadding`, `animatedTransitions`, `disableGaps`, `enableZoomOut`.
>
> **What This Proposal Solves:**
> Removes all legacy `Viewer` object references, `DownloadManager` parameters, and `Injekt.get()` calls from `ComposePagerViewer` and `ComposeWebtoonViewer`. Viewer configuration is hoisted into immutable UI state models (`PagerViewerConfigUiModel`, `WebtoonViewerConfigUiModel`), making the viewers pure Compose layouts.

---

## 1. Executive Summary & Vision

Jetpack Compose viewers (`HorizontalPager`, `VerticalPager`, `LazyColumn`) should be decoupled from legacy Android `View` implementations and platform service locators. Passing legacy `PagerViewer` and `WebtoonViewer` objects down into Compose creates bidirectional coupling where Compose inspects requested positions from legacy adapters and calls back into invisible view hierarchies.

### The Objective
Decouple [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt) and [`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt) by:
1. Eliminating `viewer: PagerViewer` and `viewer: WebtoonViewer` parameters in favor of pure configuration UI models.
2. Removing `downloadManager: DownloadManager` from viewer parameter lists.
3. Stripping `Injekt.get<ReaderPreferences>()` from both viewers.
4. Hoisting all viewer configuration properties (paddings, gaps, zoom settings, theme background colors) into the screen-level `ReaderUiState`.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Architecture
        UI1["ComposePagerViewer / ComposeWebtoonViewer"] -->|Legacy View Reference| View["viewer: PagerViewer / WebtoonViewer"]
        UI1 -->|Injekt.get()| Prefs["ReaderPreferences"]
        UI1 -->|Direct Dependency| DM["downloadManager: DownloadManager"]
    end

    subgraph Proposed Decoupled Flow
        RVM["ReaderViewModel"] -->|Emits Flow| State["ReaderViewerConfigUiModel\n(themeColor, sidePadding, disableGaps, requestedPage)"]
        State --> UI2["Stateless ComposePagerViewer / ComposeWebtoonViewer"]
        UI2 -->|onPageChanged(index)| RVM
        UI2 -->|onTransitionSelected(transitionId)| RVM
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Viewer Configuration Models

```kotlin
@Immutable
data class PagerViewerConfigUiModel(
    val backgroundColor: Color,
    val cropBorders: Boolean,
    val navigateToPan: Boolean,
    val landscapeZoom: Boolean,
    val requestedPageIndex: Int?,
)

@Immutable
data class WebtoonViewerConfigUiModel(
    val backgroundColor: Color,
    val sidePaddingDp: Int,
    val disableGaps: Boolean,
    val enableZoomOut: Boolean,
    val animatedTransitions: Boolean,
    val requestedItemIndex: Int?,
)
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless `ComposePagerViewer`

```kotlin
@Composable
fun ComposePagerViewer(
    config: PagerViewerConfigUiModel,
    items: List<ReaderUiItem>,
    isRtl: Boolean,
    isVertical: Boolean,
    onPageSelected: (ReaderPage, Boolean) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onRetryTransition: (Long) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 4.2 Stateless `ComposeWebtoonViewer`

```kotlin
@Composable
fun ComposeWebtoonViewer(
    config: WebtoonViewerConfigUiModel,
    items: List<ReaderUiItem>,
    onPageSelected: (ReaderPage) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onRetryTransition: (Long) -> Unit,
    modifier: Modifier = Modifier,
)
```

Both viewers:
- Do not hold references to legacy `Viewer` views.
- Do not call `Injekt.get()`.
- Do not touch `DownloadManager`.

---

## 5. Technical Footprint & Integration

1. **[`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt)**: Replace `viewer` and `downloadManager` parameters with `config: PagerViewerConfigUiModel`; remove `Injekt.get()`.
2. **[`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt)**: Replace `viewer` and `downloadManager` parameters with `config: WebtoonViewerConfigUiModel`; remove `Injekt.get()`.
3. **[`ReaderActivity.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt)**: Pass assembled viewer configurations into Compose content.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `PagerViewerConfigUiModel` and `WebtoonViewerConfigUiModel`.
- [ ] **Step 2**: Hoist preference observation from viewers to `ReaderViewModel`.
- [ ] **Step 3**: Refactor `ComposePagerViewer.kt` to eliminate legacy view and service dependencies.
- [ ] **Step 4**: Refactor `ComposeWebtoonViewer.kt` to eliminate legacy view and service dependencies.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
