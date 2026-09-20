# Technical Proposal: Decoupling Reader Chapter Navigation, Concurrency Guards & Lifecycle Orchestration

**Status:** Complete / Implemented  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling  
**Execution Order:** Reader Track — Phase R1 (Core Navigation & Viewer Architecture), Step R1 (Priority: Critical / Concurrency Backbone)  
**Prerequisites:** None (Phase 1 Complete, Phase 2 in progress)  
**Downstream Dependents:** Step R2 ([`decouple_reader_transition_page_proposal.md`](decouple_reader_transition_page_proposal.md)), Step R3 ([`decouple_reader_compose_viewers_proposal.md`](decouple_reader_compose_viewers_proposal.md))  
**Implementation State:** 🟢 Complete (Migrated chapter navigation into `viewModelScope`, introduced `ReaderNavCommand` and `ReaderChapterTransitionState`, guarded transitions with `navigationMutex`, extracted `ResolveChapterNavTargetUseCase` and `WebtoonScrollGatingPolicy`, and eliminated dual dispatch)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Architectural Baseline:**
> Following the Compose reader migration in Neko 3.7.1 and subsequent hardening for brand-new chapter page jumps:
> - **Activity-Bound Coroutine Lifecycle ([`ReaderActivity.kt#L1161-L1185`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt#L1161-L1185))**:
>   `loadAdjacentChapter(next: Boolean)` launches inside `lifecycleScope.launch`. When the user rotates the device, unfolds a foldable device (e.g. Pixel Fold / Galaxy Fold), or switches apps during a chapter transition, the activity is destroyed/recreated and `lifecycleScope` is cancelled mid-flight. This risks interrupted downloads, incomplete progress commits, or stuck loading states.
> - **Compose Navigation Hoisting & Timer Lock Gap ([`ComposePagerViewer.kt#L370-L384`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt#L370-L384))**:
>   `onNavigateToChapter` and `onRequestPreloadChapter` have been hoisted out of `ComposePagerViewer`. However, until `ReaderChapterTransitionState.Loading` is wired into `ReaderViewModel`, overscroll transitions still rely on an internal `isTransitioning` guard with `delay(500L)`. On high-latency loads (>500ms), the guard releases prematurely; on instantaneous cache hits (<50ms), gestures are blocked unnecessarily.
> - **Compose Recomposition & Touch Slop Stabilization**:
>   `defaultPageIndex` calculation originally performed up to three unmemoized O(N) list scans on every Compose recomposition pass. Additionally, `nestedScrollConnection` previously held `items` in its `remember` keys, resetting `accumulatedOverscroll` to `0f` mid-gesture when background download statuses updated. These are stabilized via `remember(items, ...)` and `rememberUpdatedState(items)`.
> - **Fragile Imperative Event Bus via Mutable State ([`PagerViewer.kt#L35`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt#L35))**:
>   Programmatic page navigation (slider scrub, TOC selection, bookmark jumps) sets `viewer.requestedPagePosition = Pair<Int, Boolean>?`. Compose catches this via `LaunchedEffect(viewer.requestedPagePosition)` ([`ComposePagerViewer.kt#L176-L197`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt#L176-L197)) and clears it in a `finally` block. This mutable side-channel is vulnerable to race conditions, missed frames, and timing-dependent bugs.
> - **Split Concurrency Locks & Dispatch Gaps**:
>   Navigation locks are split between Activity-level state (`isScrollingThroughPagesOrChapters` in [`ReaderActivity.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt#L1165)) and ViewModel-level state (`isLoadingAdjacentChapter` in [`ReaderViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt#L530)), creating dual sources of truth and potential deadlocks under rapid hardware key events (`KEYCODE_N`, `KEYCODE_P`, `KEYCODE_R`, `KEYCODE_L`, volume keys) and gesture flings.
> - **Double Navigation Dispatch on Chapter Load ([`ReaderActivity.kt#L1200`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt#L1200) vs [`PagerViewer.kt#L129`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt#L129))**:
>   When a chapter finishes loading, `viewModel.loadChapter` updates `state.viewerChapters` which triggers `setChapters(viewerChapters)` -> `PagerViewer.moveToPage(pages[requestedIndex], false)`. Concurrently, `ReaderActivity.loadChapter` resumes from its suspension point and calls `moveToPageIndex(targetPage, false, chapterChange = true)` -> `viewer.moveToPage(...)`. Having two separate, uncoordinated pathways setting page positions creates duplicate state writes, unnecessary recompositions, and timing races.
> - **Webtoon Continuous Scrolling & Preload Prepend Race Condition ([Issue #3379](https://github.com/nekomangaorg/Neko/issues/3379))**:
>   In continuous Webtoon vertical mode, background preloading of `prevChapter` finishes and prepends 30–120 newly loaded items directly to the top of `newItems`. Because Compose `LazyColumn` does not automatically shift `firstVisibleItemIndex` when items are prepended asynchronously before layout passes, `lazyListState.firstVisibleItemIndex` lands on the newly prepended chapter. A high-frequency `snapshotFlow` immediately samples this uncompensated index and dispatches `onPageSelected` for the previous chapter, causing `loadNewChapter` to execute and triggering an infinite backward jumping loop across all prior chapters (Repro 3a). Concurrently, continuous forward transitions called `loadChapter` with default `ChapterNavTarget.Resume`, violently jumping forward to stale `last_page_read` values (Repro 3b).
> - **Legacy Hybrid `PagerViewer` / `BaseViewer` Controller**:
>   [`PagerViewer`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt) still holds a direct reference to `ReaderActivity` (`val activity: ReaderActivity`) and injects `DownloadManager` via Injekt, functioning as an obsolete bridge from the pre-Compose View system.

---

## 1. Executive Summary & Vision

The reader is the core surface of Neko. While the Compose migration in 3.7.1 moved rendering to Jetpack Compose (`HorizontalPager`, `VerticalPager`, `LazyColumn`), navigation and lifecycle orchestration remained split across legacy View controllers (`PagerViewer`), Activity `lifecycleScope`, and Compose `LaunchedEffect` hooks.

### The Problem
1. **Configuration-Vulnerable Transitions:** Launching chapter transitions in Activity `lifecycleScope` causes transitions to cancel or corrupt state upon device rotation, multi-window resize, or foldable fold/unfold events.
2. **Tight Coupling to Android Activity:** Compose viewers cannot be previewed, unit-tested, or modularized because they require concrete `activity` and `viewer` instances to navigate.
3. **Fragile Mutable State Synchronization:** Using `requestedPagePosition: Pair<Int, Boolean>?` as an imperative synchronization channel between non-Compose callers and Compose's `PagerState` is brittle and error-prone.
4. **Divided Concurrency Control & Dispatch Gaps:** Concurrency checks (`isScrollingThroughPagesOrChapters` vs `isLoadingAdjacentChapter`) are separated and lack an atomic Mutex, allowing edge-case races during fast hardware key or fling navigation.
5. **Double Navigation Dispatch:** Both `ReaderActivity.loadChapter` (`moveToPageIndex`) and `PagerViewer.setChapters` (`moveToPage`) attempt to synchronize page positioning independently on chapter load.

### The Objective
Elevate reader navigation to a pure **Unidirectional Data Flow (UDF) / MVI architecture**:
1. **Move all chapter navigation and preloading orchestration into [`ReaderViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt)** running in `viewModelScope`, ensuring transitions survive configuration changes.
2. **Establish a robust concurrency state machine (`ReaderChapterTransitionState`)** protected by a coroutine `Mutex` in `ReaderViewModel`.
3. **Make `ComposePagerViewer` completely stateless** by replacing `viewer.activity.*` calls with hoisted event lambdas (`onNavigateToChapter`, `onRequestPreload`).
4. **Replace `requestedPagePosition` with a buffered unidirectional navigation command channel (`ReaderNavCommand`)**, unifying page positioning into a single source of truth and eliminating double dispatch.
5. **Establish a clear deprecation and sunset roadmap for [`PagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt) and [`BaseViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/BaseViewer.kt)**.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Architecture
        RA["ReaderActivity\n(lifecycleScope.launch)"] -->|isScrollingThroughPagesOrChapters| RA
        PV["PagerViewer\n(activity reference)"] -->|requestedPagePosition Pair?| CPV1["ComposePagerViewer"]
        CPV1 -->|viewer.activity.loadChapter()| RA
        CPV1 -->|viewer.activity.requestPreloadChapter()| RA
        RA -->|viewModel.loadChapter()| RVM1["ReaderViewModel\n(isLoadingAdjacentChapter)"]
    end

    subgraph Proposed Decoupled UDF Architecture
        Input["Hardware Keys / Overlays / Gestures"] -->|ReaderUiEvent| RVM2["ReaderViewModel\n(viewModelScope + Mutex)"]
        RVM2 -->|Emits State| UIState["ReaderUiState\n(transitionState, items, activeChapter)"]
        RVM2 -->|Emits One-Time Commands| NavChannel["Channel<ReaderNavCommand>\n(ScrollToPage, SnapToPage)"]
        
        UIState --> CPV2["Stateless ComposePagerViewer"]
        NavChannel --> CPV2
        
        CPV2 -->|onNavigateToChapter(chapter, target)| RVM2
        CPV2 -->|onRequestPreload(chapter)| RVM2
        CPV2 -->|onPageSelected(page)| RVM2
        
        RVM2 -->|One-Time UI Side Effects| SideEffectChannel["Channel<ReaderUiSideEffect>\n(ShowToast, CloseReader)"]
        SideEffectChannel --> RA2["ReaderActivity\n(Presents Toasts / System UI)"]
    end
```

---

## 3. Proposed Domain Models & State Definitions

### 3.1 Reader Navigation Commands (`ReaderNavCommand`)
Replaces the mutable `viewer.requestedPagePosition` with an explicit, one-time command stream consumed deterministically by Compose:

```kotlin
package eu.kanade.tachiyomi.ui.reader.model

/** One-time programmatic navigation command dispatched from ViewModel to Compose Pager/Webtoon. */
sealed interface ReaderNavCommand {
    /** Scroll to a target page index, optionally animated (slider scrub, TOC jump). */
    data class ScrollToPage(
        val pageIndex: Int,
        val animated: Boolean,
    ) : ReaderNavCommand

    /** Instantly snap to a target page index without animation (initial load, chapter swap). */
    data class SnapToPage(
        val pageIndex: Int,
    ) : ReaderNavCommand

    /** Scroll to a specific adjacent direction. */
    data class StepPage(
        val forward: Boolean,
    ) : ReaderNavCommand
}
```

### 3.2 Reader Transition State Machine (`ReaderChapterTransitionState`)
Replaces disjoint boolean flags (`isLoading`, `isScrollingThroughPagesOrChapters`, `isLoadingAdjacentChapter`) with an exhaustive, mutually exclusive state model:

```kotlin
package eu.kanade.tachiyomi.ui.reader.model

/** Represents the current lifecycle and execution state of chapter transitions in the reader. */
sealed interface ReaderChapterTransitionState {
    /** Viewer is idle and ready to process navigation. */
    data object Idle : ReaderChapterTransitionState

    /** Currently resolving and switching to a target chapter. */
    data class Loading(
        val targetChapterId: Long,
        val navTarget: ChapterNavTarget,
    ) : ReaderChapterTransitionState

    /** Target chapter successfully loaded; waiting for initial page composition/settling. */
    data class Settling(
        val targetChapterId: Long,
        val targetPage: Int,
    ) : ReaderChapterTransitionState

    /** Chapter transition failed (e.g. Network error, missing pages). */
    data class Error(
        val targetChapterId: Long,
        val throwable: Throwable,
    ) : ReaderChapterTransitionState
}
```

### 3.3 Navigation Intent Actions (`ReaderNavigationAction`)
Encapsulates all sources of chapter navigation (hardware keys, tap gestures, transition overscroll, slider):

```kotlin
package eu.kanade.tachiyomi.ui.reader.model

sealed interface ReaderNavigationAction {
    data class LoadAdjacent(val forward: Boolean) : ReaderNavigationAction
    data class JumpToChapter(val chapter: Chapter, val navTarget: ChapterNavTarget) : ReaderNavigationAction
    data class PreloadChapter(val chapter: Chapter) : ReaderNavigationAction
    data class SeekPage(val pageIndex: Int, val animated: Boolean) : ReaderNavigationAction
}
```

### 3.4 Directional Navigation Target Resolver (`ResolveChapterNavTargetUseCase`)
Extracts directional chapter navigation resolution out of monolithic ViewModel/Activity classes into a pure, testable Kotlin domain interactor:

```kotlin
package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter

/**
 * Resolves the appropriate navigation target when moving between chapters in reading order.
 * Ensures forward continuous transitions land at the start (page 0), backward transitions
 * land at the end, and initial loads or explicit jumps restore reading progress.
 */
class ResolveChapterNavTargetUseCase {

    operator fun invoke(
        currentChapter: ReaderChapter?,
        selectedChapter: ReaderChapter,
        chapterList: List<ReaderChapter>,
        isContinuousScroll: Boolean = true,
    ): ChapterNavTarget {
        if (currentChapter == null || currentChapter.chapter.id == selectedChapter.chapter.id) {
            return ChapterNavTarget.Resume
        }

        val currentIndex = chapterList.indexOfFirst { it.chapter.id == currentChapter.chapter.id }
        val newIndex = chapterList.indexOfFirst { it.chapter.id == selectedChapter.chapter.id }

        val isForward = if (currentIndex != -1 && newIndex != -1) {
            newIndex > currentIndex
        } else {
            true
        }

        return if (isForward) {
            ChapterNavTarget.Start
        } else {
            ChapterNavTarget.End
        }
    }
}
```
This isolates the root cause of [Issue #3379 (Repro 3b)](https://github.com/nekomangaorg/Neko/issues/3379) into a zero-dependency domain contract, guaranteeing that continuous forward scrolling into a previously read chapter lands on page 0 rather than reusing a stale `last_page_read` index.

---

## 4. ViewModel-Centric Navigation & Concurrency Engine

### 4.1 Orchestration in `ReaderViewModel.kt`
All transition logic is migrated from `ReaderActivity.loadAdjacentChapter` into `ReaderViewModel`. Transitions are guarded with a `Mutex` to eliminate race conditions from rapid input events:

```kotlin
class ReaderViewModel(...) : ViewModel() {

    private val navigationMutex = Mutex()

    private val _navigationCommands = Channel<ReaderNavCommand>(capacity = Channel.BUFFERED)
    val navigationCommands: Flow<ReaderNavCommand> = _navigationCommands.receiveAsFlow()

    private val _transitionState = MutableStateFlow<ReaderChapterTransitionState>(ReaderChapterTransitionState.Idle)
    val transitionState: StateFlow<ReaderChapterTransitionState> = _transitionState.asStateFlow()

    /**
     * Navigates to an adjacent chapter in reading order.
     * Guaranteed to survive configuration changes via viewModelScope and protected against concurrent invocations.
     */
    fun navigateAdjacentChapter(forward: Boolean) {
        viewModelScope.launch {
            if (!navigationMutex.tryLock()) {
                TimberKt.d { "Navigation already in progress; ignoring duplicate adjacent request" }
                return@launch
            }
            try {
                val adjChapter = adjacentChapter(forward)
                if (adjChapter != null) {
                    val target = if (forward) ChapterNavTarget.Start else ChapterNavTarget.End
                    executeChapterTransition(adjChapter, target)
                } else {
                    _sideEffects.send(
                        ReaderUiSideEffect.Notify(
                            if (forward) R.string.theres_no_next_chapter else R.string.theres_no_previous_chapter
                        )
                    )
                }
            } finally {
                navigationMutex.unlock()
            }
        }
    }

    /**
     * Executes chapter loading, saves reading progress for current chapter, and emits navigation command.
     */
    fun navigateToChapter(chapter: Chapter, navTarget: ChapterNavTarget) {
        viewModelScope.launch {
            navigationMutex.withLock {
                val readerChapter = getChapterList().find { it.chapter.id == chapter.id } ?: ReaderChapter(chapter)
                executeChapterTransition(readerChapter, navTarget)
            }
        }
    }

    private suspend fun executeChapterTransition(chapter: ReaderChapter, navTarget: ChapterNavTarget) {
        _transitionState.value = ReaderChapterTransitionState.Loading(chapter.chapter.id, navTarget)
        try {
            val targetPage = loadChapter(chapter, navTarget)
            if (targetPage != null && targetPage >= 0) {
                _transitionState.value = ReaderChapterTransitionState.Settling(chapter.chapter.id, targetPage)
                _navigationCommands.send(ReaderNavCommand.SnapToPage(targetPage))
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            TimberKt.e(e) { "Failed to transition to chapter ${chapter.chapter.url}" }
            _transitionState.value = ReaderChapterTransitionState.Error(chapter.chapter.id, e)
        } finally {
            _transitionState.value = ReaderChapterTransitionState.Idle
        }
    }

    fun requestPreloadChapter(chapter: Chapter) {
        viewModelScope.launch {
            val readerChapter = getChapterList().find { it.chapter.id == chapter.id } ?: ReaderChapter(chapter)
            preload(readerChapter)
        }
    }
}
```

### 4.2 Continuous Scroll Navigation & Directional Target Assignment

In continuous Webtoon mode, page selection is emitted continuously as the user scrolls across chapter boundaries. In [`ReaderViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt), `loadNewChapter` is decoupled from hardcoded `Resume` assumptions and accepts an explicit `navTarget: ChapterNavTarget`:

```kotlin
fun onPageSelected(page: ReaderPage) {
    val selectedChapter = page.chapter
    if (selectedChapter != currentChapter) {
        // Resolve directional progression relative to the current chapter
        val isForward = currentChapter?.let { current ->
            val currentIndex = chapterList.indexOfFirst { it.chapter.id == current.chapter.id }
            val newIndex = chapterList.indexOfFirst { it.chapter.id == selectedChapter.chapter.id }
            newIndex > currentIndex
        } ?: true

        val navTarget = if (isForward) ChapterNavTarget.Start else ChapterNavTarget.End
        TimberKt.d { "Continuous scroll transitioning to chapter ${selectedChapter.chapter.id} with target $navTarget" }
        loadNewChapter(selectedChapter, navTarget)
    }
    // ...
}

fun loadNewChapter(readerChapter: ReaderChapter, navTarget: ChapterNavTarget = ChapterNavTarget.Resume) {
    chapterNavJob?.cancel()
    chapterNavJob = viewModelScope.launch {
        loadChapter(readerChapter, navTarget)
    }
}
```

By ensuring that forward continuous transitions explicitly request `ChapterNavTarget.Start` and backward continuous transitions request `ChapterNavTarget.End`, the reader never jumps to stale `last_page_read` locations when crossing chapter seams.

---

## 5. UI Layer Refactoring: Stateless `ComposePagerViewer`

### 5.1 Removing `viewer.activity` Dependencies
[`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt) is refactored into a stateless composable accepting navigation streams and emitting callbacks:

```kotlin
@Composable
fun ComposePagerViewer(
    items: List<ReaderUiItem>,
    isRtl: Boolean,
    isVertical: Boolean,
    config: PagerViewerConfigUiModel,
    navCommands: Flow<ReaderNavCommand>,
    onPageSelected: (ReaderPage, Boolean) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onNavigateToChapter: (Chapter, ChapterNavTarget) -> Unit,
    onRequestPreload: (Chapter) -> Unit,
    onRetryTransition: (ReaderChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = config.initialPageIndex,
        pageCount = { items.size },
    )

    // Unidirectional command collector
    LaunchedEffect(navCommands) {
        navCommands.collect { command ->
            when (command) {
                is ReaderNavCommand.ScrollToPage -> {
                    if (command.pageIndex in items.indices) {
                        if (command.animated && config.animatedTransitions) {
                            pagerState.animateScrollToPage(
                                page = command.pageIndex,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            )
                        } else {
                            pagerState.scrollToPage(command.pageIndex)
                        }
                    }
                }
                is ReaderNavCommand.SnapToPage -> {
                    if (command.pageIndex in items.indices) {
                        pagerState.scrollToPage(command.pageIndex)
                    }
                }
                is ReaderNavCommand.StepPage -> {
                    val target = if (command.forward) pagerState.currentPage + 1 else pagerState.currentPage - 1
                    if (target in items.indices) {
                        pagerState.animateScrollToPage(target)
                    }
                }
            }
        }
    }

    // Overscroll transition triggering purely via event emission
    // (nestedScrollConnection overscroll threshold triggers onNavigateToChapter instead of viewer.activity.loadChapter)
}
```

### 5.2 Transition State Observation vs. Timer Debounce
In the interim Phase 1 implementation, Compose prevents rapid overscroll re-triggers via a local `isTransitioning` boolean and a fallback `delay(500L)` timer. In Phase 2, this timer hack is completely superseded by observing `ReaderChapterTransitionState` directly from `ReaderViewModel`:

```kotlin
val transitionState by viewModel.transitionState.collectAsStateWithLifecycle()
val isNavigating = transitionState is ReaderChapterTransitionState.Loading

// nestedScrollConnection gates overscroll gestures on live state:
if (isTrigger && !isNavigating) {
    onNavigateToChapter(toChapter.chapter, navTarget)
}
```
This guarantees that overscroll locks match the actual network/disk loading lifecycle without arbitrary timeouts.

### 5.3 Webtoon Continuous Scroll Synchronization & Scroll-Gated Transitions (Issue #3379 Fix)

Continuous vertical scrolling in [`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt) presents unique synchronization challenges when background preloading is active:

1. **Passive Preload Prepend Race Condition ([Issue #3379](https://github.com/nekomangaorg/Neko/issues/3379) Repro 3a):**
   When `prevChapter` finishes background preloading, its pages are prepended to the `LazyColumn` items list. In Jetpack Compose, prepending items increases the total count and shifts existing item indices forward by $N$ positions. Before `LazyColumn` performs a layout re-anchoring pass, `lazyListState.firstVisibleItemIndex` momentarily remains at index $K$, which now belongs to the newly prepended chapter rather than the active chapter.
   A high-frequency `snapshotFlow { lazyListState.firstVisibleItemIndex }` immediately samples this uncompensated index and dispatches `onPageSelected` for the previous chapter. `ReaderViewModel` then initiates `loadNewChapter` for that chapter, causing an infinite backward jumping cascade across all preceding chapters.

   **Resolution via Touch-Scroll Gating:**
   Page selection across adjacent chapter boundaries is strictly gated on active user touch input (`lazyListState.isScrollInProgress`). If the list is idle (no active user gesture/scroll), index changes resulting from background list mutations are rejected:
   ```kotlin
   val isAdjacentChapter = currentActiveChapterId != null && item.chapterId != currentActiveChapterId
   if (isAdjacentChapter && !lazyListState.isScrollInProgress) {
       TimberKt.d { "Ignoring passive chapter switch to ${item.chapterId} during idle list state (preload prepend defense)" }
       return@collect
   }
   ```

2. **Active Chapter Re-positioning in `WebtoonViewer`:**
   In [`WebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonViewer.kt), page positioning was previously guarded by a one-shot `isInitialLoad: Boolean` flag. Once the first chapter loaded, `isInitialLoad` became false permanently, preventing subsequent chapter transitions from re-evaluating target positions.
   Replacing this with `activeChapterId: Long?` tracking ensures that every distinct chapter change properly triggers page positioning:
   ```kotlin
   val hasChapterChanged = activeChapterId != currentChapter?.chapter?.id
   activeChapterId = currentChapter?.chapter?.id
   if (hasChapterChanged || isInitialLoad) {
       // Reliably position to requested target page (e.g. Page 0 on forward transition)
   }
   ```

3. **Destructive Fallback Elimination:**
   During list recompositions, falling back to `defaultPageIndex` when the list was already positioned caused intermittent jumps to page 0 or page 1. The recomposition check was hardened to only execute when `lazyListState.firstVisibleItemIndex == 0` AND the initial index was greater than 0, preserving stable viewport state during background item updates.

---

## 6. Deprecation & Sunset Roadmap for Legacy `PagerViewer` / `BaseViewer`

```mermaid
gantt
    title PagerViewer / BaseViewer Sunset Roadmap
    dateFormat  YYYY-MM-DD
    section Phase 1: Decouple Navigation
    Move loadChapter/preload to ViewModel        :done,    p1_1, 2026-09-10, 2026-09-11
    Hoist onNavigateToChapter out of Compose      :active,  p1_2, 2026-09-11, 2026-09-18
    Eliminate viewer.activity reference          :         p1_3, after p1_2, 5d
    section Phase 2: Domain Extraction
    Extract ReaderPagerController to UseCase     :         p2_1, after p1_3, 7d
    Replace requestedPagePosition with Channel    :         p2_2, after p2_1, 5d
    section Phase 3: Total Sunset
    Deprecate PagerViewer & BaseViewer           :         p3_1, after p2_2, 4d
    Delete PagerViewer and remove from Activity   :         p3_2, after p3_1, 5d
```

### Prerequisites & Sequential Placement
> [!IMPORTANT]
> **Execution Placement:** **Reader Track — Phase R1 (Core Navigation & Viewer Architecture), Step R1**  
> **Prerequisites:** None (Phase 1 Complete; Phase 2 in progress).  
> **Unlocks:** Step R2 ([`decouple_reader_transition_page_proposal.md`](decouple_reader_transition_page_proposal.md)) and Step R3 ([`decouple_reader_compose_viewers_proposal.md`](decouple_reader_compose_viewers_proposal.md)).  
>
> As the reader's central navigation and concurrency engine, moving chapter transitions into `ReaderViewModel.viewModelScope` guarded by `navigationMutex` and streaming `ReaderNavCommand` events is the foundational backbone of the Reader Decoupling track.

### Phase 1: Decouple Navigation & Preloading (Milestone 3.8.0)
- Move `loadAdjacentChapter` execution completely into `ReaderViewModel.viewModelScope`.
- Hoist `onNavigateToChapter` and `onRequestPreload` out of `ComposePagerViewer` and `ComposeWebtoonViewer`.
- Remove `val activity: ReaderActivity` parameter from `PagerViewer` and `WebtoonViewer`.

### Phase 2: Domain Extraction & Command Stream (Milestone 3.8.1)
- Extract page-pairing and split-page calculations from `ReaderPagerController` into pure Kotlin domain Use Cases (`PairPagesUseCase`, `ResolveViewerItemsUseCase`).
- Replace `requestedPagePosition: Pair<Int, Boolean>?` with `Channel<ReaderNavCommand>` emitted by `ReaderViewModel`.
- Convert `ReaderPreferences` collection into `ReaderViewerConfigUiModel` in `ReaderViewModel`.

### Phase 3: Sunset Legacy Viewers (Milestone 3.9.0)
- Mark `BaseViewer`, `PagerViewer`, and `WebtoonViewer` as `@Deprecated(level = DeprecationLevel.ERROR)`.
- Delete `BaseViewer` interface and its implementations.
- Let `ReaderViewModel` expose `items: StateFlow<List<ReaderUiItem>>` directly to the Compose screens.

---

## 7. Technical Footprint & Integration Checklist

| File | Proposed Modifications |
| :--- | :--- |
| [`ChapterNavTarget.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/model/ChapterNavTarget.kt) | Baseline in place. Add `ReaderNavCommand` and `ReaderChapterTransitionState`. |
| [`ReaderViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt) | Add `navigationMutex`, `navigateAdjacentChapter(forward: Boolean)`, and `_navigationCommands` Channel. Pass directional `ChapterNavTarget.Start` / `ChapterNavTarget.End` during continuous scrolling. |
| [`ReaderActivity.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt) | Delegate key events (`KEYCODE_N`, `KEYCODE_P`, `KEYCODE_R`, `KEYCODE_L`) directly to `viewModel.navigateAdjacentChapter(...)`. Remove `lifecycleScope.launch` navigation blocks. |
| [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt) | Replace `viewer.activity.loadChapter(...)` and `viewer.activity.requestPreloadChapter(...)` with hoisted lambdas. Consume `ReaderNavCommand` flow for page scrolling. |
| [`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt) | Gate `onPageSelected` for adjacent chapters on `lazyListState.isScrollInProgress` to prevent passive preload prepend race conditions ([Issue #3379](https://github.com/nekomangaorg/Neko/issues/3379)). |
| [`WebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonViewer.kt) | Replace one-shot `isInitialLoad` with `activeChapterId` tracking so multi-chapter transitions re-evaluate requested positions. Remove `val activity: ReaderActivity`. |
| [`PagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt) | Remove `val activity: ReaderActivity`. Mark class as `@Deprecated`. |
| [`ResolveChapterNavTargetUseCase.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/domain/ResolveChapterNavTargetUseCase.kt) | Pure domain interactor resolving directional navigation targets (`Start`, `End`, `Resume`). |

---

## 8. Verification & Test Strategy

### 8.1 Current Testing Challenges & Decoupling Prerequisites

Writing isolated unit tests for reader navigation has historically been blocked by deep Android framework and architectural coupling:

1. **Android Framework Coupling (`ReaderActivity` & `DisplayMetrics`):**
   `WebtoonViewer` and `PagerViewer` require a concrete `ReaderActivity` instance in their constructors, accessing `activity.resources.displayMetrics` and `activity.viewModel`. This prevents instantiating viewers inside pure JVM unit tests (`testDebugUnitTest`) without heavyweight Robolectric or Android instrumentation.
2. **Global Service Locator (`Injekt`):**
   `ReaderViewModel` and the viewers call `Injekt.get<PreferencesHelper>()`, `Injekt.get<DownloadManager>()`, etc., directly. In headless JVM tests, these calls throw `InjektException` unless an extensive mock registry is initialized.
3. **Embedded Domain Logic:**
   Directional target resolution (`Start` vs `End` vs `Resume`) was originally embedded directly inside `ReaderViewModel.onPageSelected`, entwined with database queries and coroutine launches.
4. **Compose Runtime State Dependencies:**
   Scroll gating and index change detection were tied to `snapshotFlow { lazyListState.firstVisibleItemIndex }`, requiring a live Compose composition to verify.

### 8.2 Decoupled Pure JVM Unit Testing Architecture

By extracting pure domain use cases and state policies, reader navigation can be 100% verified using fast JVM unit tests:

#### 1. Directional Target Resolution Tests (`ResolveChapterNavTargetUseCaseTest`)
Located at `app/src/test/java/eu/kanade/tachiyomi/ui/reader/domain/ResolveChapterNavTargetUseCaseTest.kt`:

```kotlin
class ResolveChapterNavTargetUseCaseTest {

    private val useCase = ResolveChapterNavTargetUseCase()

    private val ch1 = createReaderChapter(id = 1L, url = "/ch1")
    private val ch2 = createReaderChapter(id = 2L, url = "/ch2")
    private val ch3 = createReaderChapter(id = 3L, url = "/ch3")
    private val chapters = listOf(ch1, ch2, ch3)

    @Test
    fun `when scrolling forward to next chapter, target is Start (page 0)`() {
        val target = useCase(currentChapter = ch1, selectedChapter = ch2, chapterList = chapters)
        assertThat(target).isEqualTo(ChapterNavTarget.Start)
    }

    @Test
    fun `when scrolling backward to previous chapter, target is End (last page)`() {
        val target = useCase(currentChapter = ch2, selectedChapter = ch1, chapterList = chapters)
        assertThat(target).isEqualTo(ChapterNavTarget.End)
    }

    @Test
    fun `when opening chapter initially, target is Resume (saved progress)`() {
        val target = useCase(currentChapter = null, selectedChapter = ch2, chapterList = chapters)
        assertThat(target).isEqualTo(ChapterNavTarget.Resume)
    }

    @Test
    fun `when re-selecting current chapter, target is Resume`() {
        val target = useCase(currentChapter = ch2, selectedChapter = ch2, chapterList = chapters)
        assertThat(target).isEqualTo(ChapterNavTarget.Resume)
    }
}
```

#### 2. Scroll Gating Policy Tests (`WebtoonScrollGatingPolicyTest`)
Extracts the transition evaluation policy from Compose into a pure Kotlin evaluator:

```kotlin
class WebtoonScrollGatingPolicyTest {

    @Test
    fun `when list is idle and candidate is from adjacent chapter, transition is rejected`() {
        val result = WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
            activeChapterId = 2L,
            candidateChapterId = 1L,
            isScrollInProgress = false,
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `when list is actively scrolling and candidate is from adjacent chapter, transition is accepted`() {
        val result = WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
            activeChapterId = 2L,
            candidateChapterId = 1L,
            isScrollInProgress = true,
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `when candidate is from current chapter, selection is always accepted even if idle`() {
        val result = WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
            activeChapterId = 2L,
            candidateChapterId = 2L,
            isScrollInProgress = false,
        )
        assertThat(result).isTrue()
    }
}
```

### 8.3 Issue #3379 Regression Verification Matrix

| Test Case | Scenario | Expected Outcome | Verified By |
| :--- | :--- | :--- | :--- |
| **Repro 3a: Preload Prepend Race** | Active on Ch.46; Ch.45 finishes preloading and prepends 60 items to top while `isScrollInProgress == false`. | Prepend does NOT trigger `onPageSelected(Ch.45)`; viewport re-anchors to Ch.46 without backward jumping loop. | `WebtoonScrollGatingPolicyTest` + UI Integration |
| **Repro 3b: Forward Transition Stale Resume** | User scrolls from Ch.32 to Ch.33; Ch.33 has stale `last_page_read = 118`. | Transition dispatches with `ChapterNavTarget.Start`; reader lands on page 0 of Ch.33. | `ResolveChapterNavTargetUseCaseTest` |
| **Backward Continuous Transition** | User scrolls upward from Ch.33 into Ch.32. | Transition dispatches with `ChapterNavTarget.End`; reader lands on the last page of Ch.32. | `ResolveChapterNavTargetUseCaseTest` |
| **Hardware Key Concurrency** | Rapid presses of `KEYCODE_N` during an active chapter transition. | `navigationMutex` suppresses overlapping requests; exactly one transition completes. | `ReaderViewModelConcurrencyTest` |
| **Configuration Change Resilience** | Fold/unfold or rotate device during chapter loading. | `viewModelScope` job completes; new composition collects settling state and snaps to target page. | Instrumentation / Manual Test |

### 8.4 Compose UI Headless Testing

Once `ComposeWebtoonViewer` and `ComposePagerViewer` are fully decoupled from `WebtoonViewer` and `Injekt` as described in [`decouple_reader_compose_viewers_proposal.md`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/docs/proposals/reader/decouple_reader_compose_viewers_proposal.md), Compose tests can run in headless Android/Robolectric environments via `runComposeUiTest`:

```kotlin
@RunWith(AndroidJUnit4::class)
class ComposeWebtoonViewerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenItemsPrependedWhileIdle_doesNotTriggerOnPageSelectedForNewChapter() {
        var pageSelectedChapterId: Long? = null

        composeTestRule.setContent {
            ComposeWebtoonViewer(
                config = WebtoonViewerConfigUiModel(...),
                items = initialItems,
                onPageSelected = { page -> pageSelectedChapterId = page.chapter.chapter.id },
                ...
            )
        }

        // Simulate background prepend without scroll gesture
        // Assert pageSelectedChapterId remains on the original chapter
    }
}
```

### 8.5 Automated Tooling Verification
- Execute `./gradlew testDebugUnitTest` to verify JVM unit test suite.
- Execute `./gradlew ktfmtFormat` to enforce coding style standards.
