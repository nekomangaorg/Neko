# Technical Proposal: High-Performance, Rock-Solid Architecture for ComposePagerViewer

**Status:** Proposed  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling & Paged Engine Stabilization  
**Execution Order:** Reader Track — Phase R1 (Core Navigation, Engine & Viewers), Step R5 (Priority: High / Paged Engine Hardening)  
**Prerequisites:** Step R1 ([`decouple_reader_navigation_and_lifecycle_orchestration_proposal.md`](decouple_reader_navigation_and_lifecycle_orchestration_proposal.md)), Step R2 ([`decouple_reader_transition_page_proposal.md`](decouple_reader_transition_page_proposal.md)), Step R4 ([`reader_preloader_engine_proposal.md`](reader_preloader_engine_proposal.md))  
**Related Proposals:** [`decouple_reader_compose_viewers_proposal.md`](decouple_reader_compose_viewers_proposal.md), [`reader_preloader_engine_proposal.md`](reader_preloader_engine_proposal.md)  
**Implementation Target:** [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt), [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt), [`PagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt)  

---

## 📌 Baseline Audit & Problem Statement

Following the stabilization of [`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt) in `ref/rock-solid-webtoon-compose-viewer`, the horizontal and vertical paginated readers ([`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt) and [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt)) represent the next major architectural bottleneck in Neko's reading pipeline.

Together spanning **1,065 lines of code**, these two composables bundle eight distinct runtime responsibilities into UI rendering:

```mermaid
graph TD
    CPV["ComposePagerViewer.kt (621 lines)\nPagerPageItem.kt (444 lines)"]
    CPV --> R1["1. Subtree Teardown on Chapter Boundary (key(...) reset)"]
    CPV --> R2["2. In-UI Dual Disk/Memory Coil Preloader (130 lines)"]
    CPV --> R3["3. Post-Composition Re-anchoring via LaunchedEffect(items)"]
    CPV --> R4["4. Direct PagerViewer View Mutation & Service Locator (Injekt)"]
    CPV --> R5["5. Preference Flow Flooding in Individual Page Items (5 flows/item)"]
    CPV --> R6["6. Manual NestedScroll Overscroll Physics (87 lines)"]
    CPV --> R7["7. Telephoto Zoomable Gesture & Tap Navigation Tangling (250 lines)"]
    CPV --> R8["8. Double-Page Spread Layout Math in UI Tree"]
```

While [`decouple_reader_compose_viewers_proposal.md`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/docs/proposals/reader/decouple_reader_compose_viewers_proposal.md) drafted basic UI model extraction, it left the destructive subtree teardowns, race-prone post-composition re-anchoring, and internal preloading mechanisms unaddressed. This proposal establishes the technical blueprint to make `ComposePagerViewer` **rock-solid, zero-jitter, and fully decoupled**.

---

## 1. Root-Cause Analysis: The 6 Critical Architectural Hazards

### 1.1 Hazard 1: Subtree Teardown on `currentChapterId` Destroys PagerState & Caches
* **Code Reference:** [`ComposePagerViewer.kt#L87`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt#L87)
  ```kotlin
  key(viewer, currentChapterId, isRtl, isVertical) {
      val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { items.size })
      ...
  }
  ```
* **The Mechanism:** When the user swipes across a chapter transition card into a new chapter, `currentChapterId` changes in the ViewModel. Because the entire viewer body is wrapped in `key(viewer, currentChapterId, ...)`, Compose **completely discards the active composition subtree**, destroys `pagerState`, cancels all active page jobs, and reinstantiates the entire pager from scratch.
* **The Impact:**
  - The smooth swipe transition is interrupted by an abrupt visual freeze/re-creation.
  - Active Telephoto zoom states, Coil image cache bitmaps, and decoded textures are evicted and re-allocated.
  - Generates severe garbage collection pressure and noticeable frame drops at the exact moment the user expects continuous reading flow.

### 1.2 Hazard 2: Dual Disk/Memory Coil Preloader Contaminates the UI Layer
* **Code Reference:** [`ComposePagerViewer.kt#L218-L350`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt#L218-L350)
* **The Mechanism:** `ComposePagerViewer` maintains local state variables:
  ```kotlin
  val preloadedDiskKeys = remember { mutableSetOf<String>() }
  val preloadedMemoryKeys = remember { mutableSetOf<String>() }
  val activeDisposables = remember { mutableMapOf<String, Disposable>() }
  ```
  It manually constructs Coil `ImageRequest` objects, binds them to `context.imageLoader.enqueue()`, invokes `page.chapter.pageLoader?.loadPage(page)`, and tracks `Disposable` cancellations in a `DisposableEffect`.
* **The Impact:**
  - Duplicate code: Identical to the anti-pattern previously purged from `ComposeWebtoonViewer`.
  - Rapid swiping causes cancel/restart storms with arbitrary `delay(50L)` debounce timers.
  - Memory leaks: If the Composable is disposed mid-swipe or rotated, asynchronous Coil requests risk leaking the surrounding Activity context.

### 1.3 Hazard 3: Post-Composition Re-anchoring via `LaunchedEffect(items)` Causes Page Flashing
* **Code Reference:** [`ComposePagerViewer.kt#L156-L182`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt#L156-L182)
* **The Mechanism:** When adjacent chapters are appended or prepended to `items`, `ComposePagerViewer` attempts to re-align the active page by running `pagerState.scrollToPage(newIndex)` inside `LaunchedEffect(items)`.
* **The Impact:**
  - `LaunchedEffect` executes **after** composition and after layout measurement.
  - For 1–2 frames, `HorizontalPager` or `VerticalPager` measures and draws using the old page index (which now points to a different page or transition card).
  - The user sees a jarring 1-frame visual flash to the wrong page before snapping back.
  - Calling `scrollToPage()` abruptly aborts user swipe momentum.

### 1.4 Hazard 4: Preference Flooding and Injekt Resolution in Every Page Item
* **Code Reference:** [`PagerPageItem.kt#L71-L77, L118`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt#L71-L77)
* **The Mechanism:** In [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt), every page item executes:
  ```kotlin
  val readerPreferences: ReaderPreferences = remember { Injekt.get() }
  val imageScaleType by readerPreferences.imageScaleType().collectAsState()
  val doublePageGap by readerPreferences.doublePageGap().collectAsState()
  val invertDoublePages by readerPreferences.invertDoublePages().collectAsState()
  val readerThemePref by readerPreferences.readerTheme().collectAsState()
  val zoomStart by readerPreferences.zoomStart().collectAsState()
  ```
* **The Impact:**
  - In a chapter with 40 double-page spreads, **200 concurrent StateFlow collectors** and Injekt lookups run simultaneously.
  - Uses unconfined `collectAsState()` rather than lifecycle-safe `collectAsStateWithLifecycle()`, violating `.agents/AGENTS.md` rules and wasting CPU cycles while offscreen.

### 1.5 Hazard 5: Manual NestedScroll Overscroll Physics Trapped in Viewer
* **Code Reference:** [`ComposePagerViewer.kt#L419-L507`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt#L419-L507)
* **The Mechanism:** An 88-line anonymous `NestedScrollConnection` object intercepts `onPreScroll`, `onPostScroll`, and `onPreFling` to accumulate pixel deltas, manually checking edge bounds (`currentPage == 0`, `currentPage == pageCount - 1`), computing RTL inversions, and triggering chapter switches.
* **The Impact:**
  - Untestable in JVM unit tests.
  - Fragile coordinate handling: Fails or triggers false positives during two-finger zoom pans.

### 1.6 Hazard 6: Direct Coupling to Legacy `PagerViewer` View Class
* **Code Reference:** [`ComposePagerViewer.kt#L66`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt#L66) and [`PagerPageItem.kt#L66`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt#L66)
* **The Mechanism:** Both composables directly accept `viewer: PagerViewer` and `downloadManager: DownloadManager`, mutating mutable properties (`viewer.currentPagePosition = pageIndex`, `viewer.requestedPagePosition = null`, `viewer.moveToNext()`).
* **The Impact:** Completely prevents previewing composables in Android Studio `@Preview` and tightly binds Compose rendering to deprecated View classes scheduled for deletion in Phase 3.

### 1.7 Hazard 7: Dead-Code Spread Detection & Dual-Page Pairing Desynchronization
* **Code Reference:** [`ReaderPagerController.kt#L131, L219`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/ReaderPagerController.kt#L131), [`PagerPageItem.kt#L445-L615`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt#L445-L615)
* **The Mechanism:** In the legacy View reader, `PagerPageHolder` checked image aspect ratios upon decoding and flagged wide images with `page.fullPage = true`, allowing `ReaderPagerController.joinItems` to isolate two-page spreads. In Compose, `PagerPageHolder` was deleted and `page.fullPage` / `page.longPage` are **never populated dynamically**.
* **The Impact:**
  - **In Dual-Page Mode**: 2-page spreads (landscape images where `width > height`) are blindly chunked alongside single pages into a single viewport. Both images are squished down to half width, and all subsequent two-page spreads across the chapter become permanently desynchronized.
  - **In Single-Page Mode with `splitPages = true`**: Because `page.longPage` is never set, [`ReaderPagerController.kt#L131`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/ReaderPagerController.kt#L131) never runs, leaving split double pages completely broken.
  - **In Scroll Re-anchoring**: Naive identity checks comparing only `this.page.page == target.page.page` break during `shiftDoublePage` or dynamic spread isolation because `extraPage` is ignored.

---

## 2. The 5 Pillars of the Rock-Solid Paged Architecture

```mermaid
flowchart TD
    subgraph Data & Domain Pipeline
        Chapters["ViewerChapters (curr, prev, next)"] --> Controller["BuildPagerItemsUseCase\n(Stateless Pairing & Transitions)"]
        Decoded["Image Stream / Dimensions"] --> WideDetector["CheckWidePageUseCase\n(Spread & Aspect Ratio Isolation)"]
        WideDetector --> Controller
        Controller --> Resolver["PagerScrollAnchorResolver\n(Spread-Aware Re-anchoring)"]
        Controller --> Preloader["ReaderPreloadEngine\n(Headless Disk/Memory Warmer)"]
    end

    subgraph Unidirectional UI State
        Controller --> State["PagerViewerUiState\n(items, config, isRtl, isVertical)"]
        Resolver --> AnchorTarget["AnchorTarget(index, item)"]
    end

    subgraph Pure Stateless Compose Layer
        State --> CPV["Stateless ComposePagerViewer (~180 lines)"]
        AnchorTarget -->|Pre-measure requestScrollToPage| CPV
        CPV --> Overscroll["Modifier.pagerOverscrollNavigation(...)"]
        CPV --> Pager["HorizontalPager / VerticalPager\n(Persistent PagerState, Stable Keys)"]
        Pager --> PPI["Stateless PagerPageItem\n(Zero Injekt, Pre-resolved Config)"]
        PPI --> DPL["Stateless DoublePageLayout\n(Dual Aspect Scaling, Gap, RTL Order)"]
        
        CPV -->|onPageSelected(page)| RVM["ReaderViewModel"]
        CPV -->|onTransitionSelected(transition)| RVM
        CPV -->|onActiveIndexChanged(index)| Preloader
    end
```

---

### Pillar 1: Persistent PagerState & Domain-Driven Scroll Anchor Resolver

**Rule:** *Never tear down `PagerState` across chapter boundaries; resolve page shifts deterministically before layout.*

1. **Eliminate Subtree Key Reset:**
   Remove `key(viewer, currentChapterId, isRtl, isVertical)`. `PagerState` is created once and persists as long as the reading orientation (`isVertical`) and direction (`isRtl`) remain unchanged.
2. **Domain-Pure `PagerScrollAnchorResolver`:**
   Mirroring [`WebtoonScrollAnchorResolver.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonScrollAnchorResolver.kt), extract index resolution into a pure, testable domain object:
   ```kotlin
   object PagerScrollAnchorResolver {
       data class AnchorTarget(
           val index: Int,
           val item: ReaderUiItem,
       )

       fun resolveReanchorTarget(
           items: List<ReaderUiItem>,
           lastActiveItem: ReaderUiItem?,
           currentVisibleIndex: Int,
           previousItems: List<ReaderUiItem>? = null,
       ): AnchorTarget? {
           val currentItem = items.getOrNull(currentVisibleIndex)
           val activeItem = lastActiveItem

           // Fast-path 1: Item at current index is already equivalent
           if (currentItem != null && activeItem != null && currentItem.isEquivalentTo(activeItem)) {
               return null
           }

           // Fast-path 2: Unshifted append (next chapter pages appended to end)
           val prevItemAtCurrent = previousItems?.getOrNull(currentVisibleIndex)
           if (currentItem != null && prevItemAtCurrent != null &&
               currentItem::class == prevItemAtCurrent::class &&
               currentItem.isEquivalentTo(prevItemAtCurrent)
           ) {
               return null
           }

           val targetItem = activeItem ?: currentItem ?: return null
           val newIndex = items.indexOfFirst { it.isEquivalentTo(targetItem) }

           return if (newIndex != -1 && newIndex != currentVisibleIndex) {
               AnchorTarget(newIndex, items[newIndex])
           } else {
               null
           }
       }
   }
   ```
3. **Spread-Aware Identity Equivalence Contract (`isEquivalentTo`):**
   When `shiftDoublePage` or dynamic spread isolation re-chunks items (e.g. from `[p4, p5]` to `[p5, p6]`), comparing only `page.index` fails because the user is viewing `extraPage`. The identity contract must compare all constituent pages and split halves:
   ```kotlin
   fun ReaderUiItem.isEquivalentTo(target: ReaderUiItem?): Boolean {
       if (target == null) return false
       return when {
           this is ReaderUiItem.Page && target is ReaderUiItem.Page -> {
               val sameChapter = this.page.chapter.chapter.id == target.page.chapter.chapter.id
               if (!sameChapter) return false

               val thisPages = listOfNotNull(this.page, this.extraPage)
               val targetPages = listOfNotNull(target.page, target.extraPage)

               // Matches if any constituent page matches across single/dual items,
               // while respecting split half boundaries
               thisPages.any { tp ->
                   targetPages.any { op ->
                       tp.index == op.index &&
                           tp.firstHalf == op.firstHalf &&
                           tp.chapter.chapter.id == op.chapter.chapter.id
                   }
               }
           }
           this is ReaderUiItem.SplitPage && target is ReaderUiItem.SplitPage -> {
               this.page.index == target.page.index &&
                   this.page.chapter.chapter.id == target.page.chapter.chapter.id &&
                   this.split.topOffset == target.split.topOffset
           }
           this is ReaderUiItem.Transition && target is ReaderUiItem.Transition -> {
               this.transition.chapter.chapter.id == target.transition.chapter.chapter.id &&
                   this.transition::class == target.transition::class
           }
           else -> false
       }
   }
   ```
4. **Pre-Measure Composition Re-anchoring:**
   Call `pagerState.requestScrollToPage(target.index)` synchronously during composition when `items !== lastProcessedItems`. This instructs Compose to measure at the correct page index on the very first layout pass, eliminating the 1-frame flash of the wrong page.

---

### Pillar 2: Headless Preload Engine Integration

**Rule:** *Compose renders pages; it does not warm network sockets or track image cache disposables.*

1. **Shared Headless Pipeline:** Extend [`ReaderPreloadEngine`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/ReaderPreloadEngine.kt) to support Paged navigation modes:
   ```kotlin
   fun updateActivePagerIndex(
       activeIndex: Int,
       items: List<ReaderUiItem>,
       preloadAmount: Int,
       isRtl: Boolean,
   ) {
       val preloadIndices = ReaderPagerController.getPreloadIndices(
           currentIndex = activeIndex,
           preloadAmount = preloadAmount,
           totalItems = items.size,
           isRtl = isRtl,
       )
       val memoryIndices = ReaderPagerController.getPreloadIndices(
           currentIndex = activeIndex,
           preloadAmount = 2,
           totalItems = items.size,
           isRtl = isRtl,
       ).toSet()

       for (index in preloadIndices) {
           val item = items.getOrNull(index) ?: continue
           preloadItem(item, preloadMemory = index in memoryIndices)
       }
   }
   ```
2. **Purge Disposable Tracking from Compose:** Strip 130 lines of `Disposable`, `ImageRequest.Builder`, and `loadPage` calls from [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt).

---

### Pillar 3: Pure Stateless Presentation & Hoisted Configuration

**Rule:** *Page items accept plain data models; service locators and preferences are hoisted to the container.*

1. **`PagerViewerConfigUiModel` Definition:**
   ```kotlin
   @Immutable
   data class PagerViewerConfigUiModel(
       val initialIndex: Int = 0,
       val activeChapterId: Long? = null,
       val backgroundColor: Color = Color.Black,
       val isRtl: Boolean = false,
       val isVertical: Boolean = false,
       val animatedTransitions: Boolean = true,
       val imageScaleType: Int = 0,
       val pageLayout: PageLayout = PageLayout.SINGLE_PAGE,
       val doublePages: Boolean = false,
       val shiftDoublePage: Boolean = false,
       val invertDoublePages: Boolean = false,
       val doublePageGap: Int = 0,
       val zoomDoublePageSpreads: Boolean = false,
       val doublePageRotate: Boolean = false,
       val doublePageRotateReverse: Boolean = false,
       val zoomStart: Int = 0,
       val navigator: ViewerNavigation = ViewerNavigation(),
       val preloadPageAmount: Int = 4,
       val onToggleMenu: () -> Unit = {},
       val onNavigateAdjacent: (forward: Boolean) -> Unit = {},
       val onRetryTransition: (ReaderChapter) -> Unit = {},
       val onNavigateToChapter: ((Chapter, ChapterNavTarget) -> Unit)? = null,
   )
   ```
2. **Stateless `PagerPageItem`:**
   Refactor [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt) to accept:
   - `page: ReaderPage`
   - `extraPage: ReaderPage?`
   - `config: PagerViewerConfigUiModel`
   - Lambda callbacks: `onTap: (PointF) -> Unit`, `onLongTap: (ReaderPage) -> Unit`.
   All `Injekt.get()` calls and preference collections are completely removed from the page item.

---

### Pillar 4: Modular Pager Overscroll & Gesture Modifiers

**Rule:** *Complex scroll gestures must be encapsulated into decoupled, reusable Compose modifiers.*

1. **Extract `Modifier.pagerOverscrollNavigation`:**
   Encapsulate overscroll threshold checks, fling cancellation, and chapter navigation into a standalone modifier extension:
   ```kotlin
   fun Modifier.pagerOverscrollNavigation(
       pagerState: PagerState,
       isRtl: Boolean,
       isVertical: Boolean,
       items: List<ReaderUiItem>,
       thresholdPx: Float,
       onNavigateToChapter: (Chapter, ChapterNavTarget) -> Unit,
   ): Modifier
   ```

---

### Pillar 5: Dual-Page Spread Architecture & Wide-Page Splitting Engine

**Rule:** *Two-page spreads, pair shifting, and wide-page splits must be governed by pure domain interactors, not inline view hacks.*

1. **Dynamic Wide-Spread Detection (`CheckWidePageUseCase`):**
   Mirroring `CheckTallPageUseCase` from Webtoon, extract pure domain detection to flag two-page spreads when image dimensions become known:
   ```kotlin
   package eu.kanade.tachiyomi.ui.reader.domain

   import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

   class CheckWidePageUseCase {
       fun isWidePage(width: Int, height: Int): Boolean {
           if (width <= 0 || height <= 0) return false
           return width > height
       }

       operator fun invoke(page: ReaderPage, width: Int, height: Int): Boolean {
           val isWide = isWidePage(width, height)
           if (isWide) {
               page.fullPage = true
               page.longPage = true
           }
           return isWide
       }
   }
   ```
   When `MemoryCacheWarmManager` or Coil decodes an image, `CheckWidePageUseCase` evaluates dimensions and marks `page.fullPage = true`. If this modifies chapter layout topology, `BuildPagerItemsUseCase` is triggered to re-chunk the items.

2. **Pure Domain Item Assembly (`BuildPagerItemsUseCase`):**
   Extract `ReaderPagerController.buildItems` and `joinItems` into a pure Kotlin interactor:
   - Takes `ViewerChapters`, `pageLayout: PageLayout`, `shiftDoublePage: Boolean`, `isRtl: Boolean`, `forceTransition: Boolean`.
   - **Dual-Page Pairing**: In `DOUBLE_PAGES` mode (or landscape `AUTOMATIC`), isolates any `page.fullPage == true` into single-spread items (`ReaderUiItem.Page(page, null)`), preventing squishing and preserving spread pairing for subsequent pages.
   - **Spread Shifting**: When `shiftDoublePage == true`, offsets pairing so that cover pages are displayed alone.
   - **Split Double Pages**: In `SPLIT_PAGES` mode, slices wide pages (`page.longPage == true`) into two consecutive `InsertPage` instances (`firstHalf = true` and `firstHalf = false`).

3. **Stateless `DoublePageLayout.kt`:**
   Extract lines 445–615 of `PagerPageItem.kt` into a standalone composable:
   ```kotlin
   @Composable
   fun DoublePageLayout(
       page: ReaderPage,
       extraPage: ReaderPage,
       config: PagerViewerConfigUiModel,
       zoomableState: ZoomableState,
       modifier: Modifier = Modifier,
   )
   ```
   - Determines left-to-right display ordering via `(!config.isRtl).xor(config.invertDoublePages)`.
   - Applies inter-page spacing via `Arrangement.spacedBy(Size.tiny * config.doublePageGap)`.
   - Computes merged bounding box (`totalWidth = fSize.width + sSize.width`, `maxHeight = maxOf(fSize.height, sSize.height)`) and registers with `zoomableState.setContentLocation(...)`.
   - Coordinates zoom anchor centroid for `zoomDoublePageSpreads` based on reading direction.

4. **Split-Page Rendering (`SplitPageLayout.kt`):**
   For single-page mode with `firstHalf != null`, crops the bitmap to render the designated 50% half without redundant network calls or duplicate memory decoding.

---

## 3. The Target Architecture: ~180-Line `ComposePagerViewer`

```kotlin
package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerScrollAnchorResolver
import kotlinx.coroutines.flow.distinctUntilChanged
import org.nekomanga.presentation.theme.Size

@Composable
fun ComposePagerViewer(
    items: List<ReaderUiItem>,
    config: PagerViewerConfigUiModel,
    onActiveItemChanged: (Int) -> Unit,
    onPageSelected: (ReaderPage, Boolean) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = config.initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
        pageCount = { items.size },
    )

    var lastActiveItem by remember { mutableStateOf(items.getOrNull(config.initialIndex)) }
    var lastProcessedItems by remember { mutableStateOf(items) }

    val currentItems by rememberUpdatedState(items)
    val currentConfig by rememberUpdatedState(config)

    // 1. Immediate pre-measure re-anchor during composition to eliminate 1-frame flashes
    if (items !== lastProcessedItems) {
        val target = PagerScrollAnchorResolver.resolveReanchorTarget(
            items = items,
            lastActiveItem = lastActiveItem,
            currentVisibleIndex = pagerState.currentPage,
            previousItems = lastProcessedItems,
        )
        if (target != null && target.index != pagerState.currentPage) {
            pagerState.requestScrollToPage(target.index)
            lastActiveItem = target.item
        }
    }

    // 2. Fallback post-composition anchor sync
    LaunchedEffect(items) {
        val target = PagerScrollAnchorResolver.resolveReanchorTarget(
            items = items,
            lastActiveItem = lastActiveItem,
            currentVisibleIndex = pagerState.currentPage,
            previousItems = lastProcessedItems,
        )
        try {
            if (target != null && target.index != pagerState.currentPage) {
                pagerState.scrollToPage(target.index)
                lastActiveItem = target.item
            }
        } finally {
            lastProcessedItems = items
        }
    }

    // 3. Track active page index and dispatch selections
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val item = currentItems.getOrNull(pageIndex) ?: return@collect
                lastActiveItem = item
                onActiveItemChanged(pageIndex)

                when (item) {
                    is ReaderUiItem.Page -> onPageSelected(item.page, item.extraPage != null)
                    is ReaderUiItem.SplitPage -> onPageSelected(item.page, false)
                    is ReaderUiItem.Transition -> onTransitionSelected(item.transition)
                }
            }
    }

    val density = LocalDensity.current
    val thresholdPx = with(density) { Size.huge.toPx() }

    val flingBehavior = if (config.animatedTransitions) {
        PagerDefaults.flingBehavior(state = pagerState)
    } else {
        PagerDefaults.flingBehavior(state = pagerState, snapAnimationSpec = snap())
    }

    // 4. Declarative Render Tree
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(config.backgroundColor)
            .pagerOverscrollNavigation(
                pagerState = pagerState,
                isRtl = config.isRtl,
                isVertical = config.isVertical,
                items = items,
                thresholdPx = thresholdPx,
                onNavigateToChapter = { ch, target -> config.onNavigateToChapter?.invoke(ch, target) },
            ),
    ) {
        if (config.isVertical) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                flingBehavior = flingBehavior,
                key = { index -> items.getOrNull(index)?.key("pager") ?: "pager_null_$index" },
            ) { index ->
                val item = items.getOrNull(index) ?: return@VerticalPager
                PagerItemContent(item = item, config = currentConfig)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                flingBehavior = flingBehavior,
                key = { index -> items.getOrNull(index)?.key("pager") ?: "pager_null_$index" },
            ) { index ->
                val item = items.getOrNull(index) ?: return@HorizontalPager
                PagerItemContent(item = item, config = currentConfig)
            }
        }
    }
}
```

---

## 4. Architectural Comparison Matrix

| Quality Metric | Current Baseline | Proposed Decoupling Docs | **Optimal Rock-Solid Solution** |
| :--- | :--- | :--- | :--- |
| **Combined Lines of Code** | 1,065 lines | ~700 lines | **~320 lines (Viewer: ~180, PageItem: ~140)** |
| **Subtree Lifecycle** | Destroyed on chapter boundary (`key(...)`) | Destroyed on chapter boundary | **Persistent `PagerState`, Zero Teardowns** |
| **Scroll Stability on Prepend** | Asynchronous `scrollToPage` post-render | Asynchronous `scrollToPage` | **Pre-measure `requestScrollToPage` in composition** |
| **Preload / Coil Caching** | Trapped in Compose with `Disposable` maps | Trapped in Compose with `Disposable` maps | **Headless `ReaderPreloadEngine` in Domain** |
| **Injekt / Preferences** | 6 lookups per page item (~200 concurrent) | Removed from Viewer, still in items | **Zero Injekt in Viewer or List Items** |
| **Overscroll Physics** | 88-line inline `NestedScrollConnection` | 88-line inline `NestedScrollConnection` | **Modular `Modifier.pagerOverscrollNavigation`** |
| **JVM Unit Testability** | 0% (Requires Android View hierarchy) | ~40% (Partial model extraction) | **100% (Pure Domain Anchor Resolver & Preloader)** |

---

## 5. Critical Edge Cases Matrix

| Category | Scenario / Trigger Condition | Severity | Architectural Resolution |
| :--- | :--- | :--- | :--- |
| **Chapter Boundary Swiping** | User swipes from last page of Ch 1 into Transition card into Ch 2 Page 0. | Critical (Subtree Teardown / Freeze) | Remove `key(viewer, currentChapterId, ...)`. Persistent `PagerState` with `PagerScrollAnchorResolver` maps page indices across boundaries seamlessly. |
| **Prepend Flash / Jitter** | Previous chapter loads while user is on Page 0, expanding list by 40 pages. | High (1-frame flash to wrong page) | `requestScrollToPage(target.index)` executes during composition before layout, preventing Compose from measuring at old index 0. |
| **Unshifted Appending** | Next chapter finishes loading while user is reading stationary on current chapter. | High (Stutter / Momentum Kill) | Fast-path 2 in `PagerScrollAnchorResolver` returns `null` when item at current index did not shift, preventing redundant scroll calls. |
| **Double-Page Spread Inversion** | User toggles "Invert Double Pages" or "Double Page Gap" in settings sheet. | Medium (Layout Misalignment) | Settings hoisted to `PagerViewerConfigUiModel`. Changing config triggers clean recomposition of spreads without resetting pager position. |
| **Wide Double-Page Spreads (`fullPage`)** | Image width > height in dual-page mode. | High (Squished layout & pair desync) | `CheckWidePageUseCase` flags spread dynamically upon decode; `BuildPagerItemsUseCase` isolates spread into single-page item `ReaderUiItem.Page(page, null)` so subsequent spreads stay in sync. |
| **Dual-Page Shift (`shiftDoublePage`) Re-anchoring** | Shifting double pages re-chunks `[p4, p5]` to `[p5, p6]` while reading. | High (Jump to wrong page / flash) | `ReaderUiItem.isEquivalentTo` matches any constituent page across pairs (`page` or `extraPage`), preserving active viewport item across re-chunking. |
| **Split Double Pages (`SPLIT_PAGES`)** | User reads wide spread manga in portrait with split double pages enabled. | Medium (Dead code / no crop) | `BuildPagerItemsUseCase` splits wide spreads into `InsertPage` halves; `SplitPageLayout` crops bitmap at 50% boundary according to reading direction. |
| **Rapid Swiping / Fling** | User flings through 10 pages in rapid succession. | Medium (Coil Request Flooding) | Headless `ReaderPreloadEngine` bounds memory preloading strictly to immediate 2-page window with cancellation of superseded indices. |
| **RTL Edge Drag** | User reaches start/end edge in Japanese (R2L) reading mode. | Medium (Overscroll Inversion Failure) | `Modifier.pagerOverscrollNavigation` encapsulates direction inversion math, isolating it from page rendering. |

---

## 6. Implementation & Migration Roadmap

```mermaid
gantt
    title Rock-Solid Pager Viewer Implementation Roadmap
    dateFormat  YYYY-MM-DD
    section Phase 1: Pure Domain Foundations
    Extract PagerScrollAnchorResolver       :p1_1, 2026-10-01, 2d
    Extract CheckWidePageUseCase            :p1_2, after p1_1, 1d
    Extract BuildPagerItemsUseCase          :p1_3, after p1_2, 2d
    Expand ReaderPreloadEngine for Pager    :p1_4, after p1_3, 2d
    section Phase 2: Configuration & Modifier Decoupling
    Define PagerViewerConfigUiModel         :p2_1, after p1_4, 1d
    Extract Modifier.pagerOverscrollNav     :p2_2, after p2_1, 2d
    section Phase 3: Composable Modernization
    Extract Stateless DoublePageLayout      :p3_1, after p2_2, 2d
    Refactor PagerPageItem to Stateless     :p3_2, after p3_1, 2d
    Refactor ComposePagerViewer (~180 lines):p3_3, after p3_2, 2d
    section Phase 4: Verification & Handoff
    Add JVM Unit Tests for Resolver & Spread:p4_1, after p3_3, 2d
    Validate ktfmt & Handoff to R2/R6       :p4_2, after p4_1, 1d
```

### Phase 1: Pure Domain Foundations
1. Implement [`PagerScrollAnchorResolver.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerScrollAnchorResolver.kt) in `eu.kanade.tachiyomi.ui.reader.viewer.pager`.
2. Implement `CheckWidePageUseCase` to restore dynamic two-page spread detection upon image decode.
3. Extract `BuildPagerItemsUseCase` from `ReaderPagerController` to make dual-page pairing, spread isolation, and split double pages pure, deterministic, and testable.
4. Write pure JVM unit tests in [`PagerScrollAnchorResolverTest.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/test/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerScrollAnchorResolverTest.kt) covering forward seam transitions, backward expansions, unshifted appends, dual-page shift re-anchoring, and clamped index resilience.
5. Generalize [`ReaderPreloadEngine.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/ReaderPreloadEngine.kt) to support pager index calculations (`ReaderPagerController.getPreloadIndices`).

### Phase 2: Configuration & Modifier Decoupling
1. Create [`PagerViewerConfigUiModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerViewerConfigUiModel.kt) containing complete dual-page preferences.
2. Extract overscroll physics into `PagerOverscrollNavigation.kt`.

### Phase 3: Composable Modernization
1. Extract `DoublePageLayout.kt` into a stateless composable handling dual-page aspect ratio scaling, gap spacing, and RTL display ordering.
2. Refactor [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt) into a stateless composable with zero `Injekt.get()` and zero preference collectors.
3. Refactor [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt) to the ~180-line stateless structure, removing `key(viewer, currentChapterId, ...)`.

### Phase 4: Verification & Handoff
1. Verify with unit tests in `app/src/test/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/`.
2. Format all files with `./gradlew ktfmtFormat`.
3. Complete handoff to Step R2 ([`decouple_reader_transition_page_proposal.md`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/docs/proposals/reader/decouple_reader_transition_page_proposal.md)) and Step R6 ([`decouple_reader_compose_viewers_proposal.md`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/docs/proposals/reader/decouple_reader_compose_viewers_proposal.md)).

---

## 7. Post-Implementation Handoff & Downstream Decoupling

Following the completion of this proposal:
1. **Transition Page Decoupling**: Removing `DownloadManager` and `Injekt.get()` from `PagerViewerConfigUiModel` is tracked under [**Step R2 (`decouple_reader_transition_page_proposal.md`)**](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/docs/proposals/reader/decouple_reader_transition_page_proposal.md).
2. **Complete Legacy Viewer Decommission**: Decommissioning `PagerViewer.kt`, `R2LPagerViewer.kt`, `L2RPagerViewer.kt`, and `VerticalPagerViewer.kt` to drive state directly from `ReaderViewModel` is tracked under [**Step R6 (`decouple_reader_compose_viewers_proposal.md`)**](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/docs/proposals/reader/decouple_reader_compose_viewers_proposal.md).
