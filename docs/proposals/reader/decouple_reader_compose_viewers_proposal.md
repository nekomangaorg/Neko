# Technical Proposal: Decoupling ComposePagerViewer & ComposeWebtoonViewer from Legacy Android Views & Direct Injections

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Next Release (Neko 3.8.0 / Step R6: Complete Viewer Decommissioning)  
**Execution Order:** Reader Track — Phase R1 (Core Navigation, Engine & Viewers), Step R6 (Priority: Critical / Viewer Decoupling)  
**Prerequisites:** Step R1 ([`decouple_reader_navigation_and_lifecycle_orchestration_proposal.md`](decouple_reader_navigation_and_lifecycle_orchestration_proposal.md)), Step R2 ([`decouple_reader_transition_page_proposal.md`](decouple_reader_transition_page_proposal.md)), Step R4 ([`reader_preloader_engine_proposal.md`](reader_preloader_engine_proposal.md)), Step R5 ([`rock_solid_paged_compose_viewer_proposal.md`](rock_solid_paged_compose_viewer_proposal.md))  
**Downstream Dependents:** Reader Track Phase R2 Auxiliary Proposals (Steps R7–R10), Step R11 ([`native_compose_webtoon_subsampling_renderer_proposal.md`](native_compose_webtoon_subsampling_renderer_proposal.md))  
**Implementation State:** 🟡 Scheduled for Next Release (Current release delivers Step R1, Step R2; Step R6 will decommission legacy `WebtoonViewer.kt` and `PagerViewer.kt` and decouple `ComposePagerViewer`)  

---

## 📌 Codebase Audit & Baseline Notes

> [!IMPORTANT]
> **Release Staging & Separation of Scope:**
> - **Current Release (`ref/rock-solid-webtoon-phase2` / 3.7.x)**: Shipped Step R1 (navigation orchestration), Step R2 (transition page decoupling), and the Rock-Solid Webtoon Compose viewer stabilization (upstream tall page splitting, headless `ReaderPreloadEngine`, isolated gesture modifiers, deterministic scroll re-anchoring).
> - **Next Release (Step R6 / 3.8.x)**: Scheduled to perform the complete decommissioning of legacy `WebtoonViewer.kt` and `PagerViewer.kt` Android View hierarchies, applying `PagerViewerConfigUiModel` hoisting to `ComposePagerViewer.kt`, and wiring both viewers directly from `ReaderViewModel` and `ReaderActivity` without compatibility bridge overloads.

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
> - **Untestable Viewport Synchronization & Re-anchoring ([Issue #3379](https://github.com/nekomangaorg/Neko/issues/3379))**:
>   - Because `WebtoonViewer` requires an active `ReaderActivity` instance and `DisplayMetrics` in its constructor, its item assembly logic (`setChapters`) and list positioning cannot be run or verified in pure headless JVM unit tests.
>   - Item equality and list re-anchoring when items are prepended currently rely on inline property checks in Compose rather than a pure domain model identity contract (`ReaderUiItem.isEquivalentTo`).
>
> **What This Proposal Solves:**
> Removes all legacy `Viewer` object references, `DownloadManager` parameters, and `Injekt.get()` calls from `ComposePagerViewer` and `ComposeWebtoonViewer`. Viewer configuration is hoisted into immutable UI state models (`PagerViewerConfigUiModel`, `WebtoonViewerConfigUiModel`), item generation is extracted into a testable pure Kotlin interactor (`BuildWebtoonItemsUseCase`), and item equivalence is formalized to enable headless JVM unit testing.

---

## 1. Executive Summary & Vision

Jetpack Compose viewers (`HorizontalPager`, `VerticalPager`, `LazyColumn`) should be decoupled from legacy Android `View` implementations, platform service locators, and concrete Activity lifecycles. Passing legacy `PagerViewer` and `WebtoonViewer` objects down into Compose creates bidirectional coupling where Compose inspects requested positions from legacy adapters and calls back into invisible view hierarchies. Furthermore, this coupling blocks writing fast, reliable pure JVM unit tests for page ordering, chapter seams, and scroll re-anchoring.

### The Objective
Decouple [`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt) and [`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt) by:
1. Eliminating `viewer: PagerViewer` and `viewer: WebtoonViewer` parameters in favor of pure configuration UI models.
2. Removing `downloadManager: DownloadManager` from viewer parameter lists.
3. Stripping `Injekt.get<ReaderPreferences>()` from both viewers.
4. Hoisting all viewer configuration properties (paddings, gaps, zoom settings, theme background colors) into the screen-level `ReaderUiState`.
5. Extracting list item assembly into a pure Kotlin domain interactor (`BuildWebtoonItemsUseCase`) with identity equivalence (`ReaderUiItem.isEquivalentTo`) to unlock 100% headless JVM testability.

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
    val pageLayout: PageLayout,
    val doublePages: Boolean,
    val shiftDoublePage: Boolean,
    val invertDoublePages: Boolean,
    val doublePageGap: Int,
    val zoomDoublePageSpreads: Boolean,
    val doublePageRotate: Boolean,
    val doublePageRotateReverse: Boolean,
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

### 3.2 Domain Item Equivalence & Re-anchoring Contract

When background chapter preloading prepends or appends items, or when dual-page spread isolation and shifting (`shiftDoublePage`) re-chunks items across list updates, the list shifts item indices. To reliably re-anchor the viewport to the currently visible item without relying on volatile numeric indices, we define an identity equivalence contract that supports single pages, paired dual-page spreads, and split halves:

```kotlin
package eu.kanade.tachiyomi.ui.reader.model

/**
 * Compares two reader UI items for semantic identity equivalence across list updates,
 * preloads, dual-page pair shifting, and spread isolation.
 * Enables deterministic list re-anchoring and pure JVM test assertions without Android View models.
 */
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

### 4.3 Decoupled Item Generation (`BuildWebtoonItemsUseCase`)

Currently, `WebtoonViewer.setChapters` and `WebtoonViewer.items` assemble `ReaderUiItem` instances, but directly access `activity.updateWebtoonViewerItems()`, `activity.resources.displayMetrics`, and mutable internal state.

We extract this mapping into a pure Kotlin interactor:

```kotlin
package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters

/**
 * Pure Kotlin interactor mapping ViewerChapters into a sequential list of ReaderUiItem instances.
 * Completely free of Android Views, DisplayMetrics, or Injekt lookups.
 */
class BuildWebtoonItemsUseCase {

    operator fun invoke(chapters: ViewerChapters): List<ReaderUiItem> {
        val items = mutableListOf<ReaderUiItem>()

        // 1. Prepend prevChapter transition & pages
        chapters.prevChapter?.let { prev ->
            items.add(ReaderUiItem.Transition(ChapterTransition.Prev(prev)))
            items.addAll(prev.pages?.map { ReaderUiItem.Page(it) }.orEmpty())
        }

        // 2. Add currentChapter pages
        chapters.currentChapter?.let { current ->
            items.addAll(current.pages?.map { ReaderUiItem.Page(it) }.orEmpty())
        }

        // 3. Append nextChapter transition & pages
        chapters.nextChapter?.let { next ->
            items.add(ReaderUiItem.Transition(ChapterTransition.Next(next)))
            items.addAll(next.pages?.map { ReaderUiItem.Page(it) }.orEmpty())
        }

        return items
    }
}
```
This enables unit testing item assembly and index calculation without spinning up Android framework components.

### 4.4 Decoupled Pager Item Generation (`BuildPagerItemsUseCase`)

Currently, `ReaderPagerController.buildItems` and `joinItems` retain mutable fields (`prevTransition`, `nextTransition`, `currentChapter`, `pageToShift`) and mutate `page.shiftedPage` and `page.isolatedPage` inline.

We extract this into a stateless domain interactor:

```kotlin
package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout

/**
 * Pure Kotlin interactor mapping ViewerChapters into a sequential list of ReaderUiItem instances
 * for horizontal and vertical paginated viewers.
 * Handles dual-page spread pairing, wide-spread isolation, pair shifting, and single-page splits.
 */
class BuildPagerItemsUseCase {

    operator fun invoke(
        chapters: ViewerChapters,
        pageLayout: PageLayout,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
        forceTransition: Boolean = false,
    ): List<ReaderUiItem> {
        val isDoublePages = pageLayout == PageLayout.DOUBLE_PAGES
        val isSplitPages = pageLayout == PageLayout.SPLIT_PAGES
        val subItems = mutableListOf<Any>()

        // 1. Previous chapter boundary items
        chapters.prevChapter?.let { prev ->
            val prevPages = prev.pages.orEmpty()
            val fullCount = prevPages.count { it.fullPage == true || it.isolatedPage }
            subItems.addAll(prevPages.takeLast(if ((prevPages.size + fullCount) % 2 == 0) 2 else 3))
            subItems.add(ChapterTransition.Prev(chapters.currChapter, prev))
        }

        // 2. Current chapter pages
        chapters.currChapter.pages?.let { subItems.addAll(it) }

        // 3. Next chapter boundary items
        chapters.nextChapter?.let { next ->
            subItems.add(ChapterTransition.Next(chapters.currChapter, next))
            subItems.addAll(next.pages.orEmpty().take(2))
        }

        // 4. Assemble ReaderUiItems
        val result = mutableListOf<ReaderUiItem>()
        if (!isDoublePages) {
            val processed = if (isSplitPages) splitWidePages(subItems) else subItems
            for (item in processed) {
                when (item) {
                    is ReaderPage -> result.add(ReaderUiItem.Page(item))
                    is ChapterTransition -> result.add(ReaderUiItem.Transition(item))
                }
            }
            if (isRtl) result.reverse()
        } else {
            result.addAll(chunkDualPages(subItems, shiftDoublePage, isRtl))
        }

        return result
    }

    private fun splitWidePages(items: List<Any>): List<Any> {
        val output = mutableListOf<Any>()
        for (item in items) {
            if (item is ReaderPage && item.longPage == true) {
                output.add(InsertPage(item).apply { firstHalf = true })
                output.add(InsertPage(item).apply { firstHalf = false })
            } else {
                output.add(item)
            }
        }
        return output
    }

    private fun chunkDualPages(
        items: List<Any>,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
    ): List<ReaderUiItem> {
        val result = mutableListOf<ReaderUiItem>()
        val pageBuffer = mutableListOf<ReaderPage?>()
        var hasShifted = false

        for (item in items) {
            if (item is ReaderPage) {
                if (item.fullPage == true) {
                    flushBuffer(pageBuffer, result)
                    result.add(ReaderUiItem.Page(item, null))
                } else {
                    pageBuffer.add(item)
                    val targetSize = if (shiftDoublePage && !hasShifted) 1 else 2
                    if (pageBuffer.size == targetSize) {
                        flushBuffer(pageBuffer, result)
                        hasShifted = true
                    }
                }
            } else if (item is ChapterTransition) {
                flushBuffer(pageBuffer, result)
                result.add(ReaderUiItem.Transition(item))
            }
        }
        flushBuffer(pageBuffer, result)
        if (isRtl) result.reverse()
        return result
    }

    private fun flushBuffer(buffer: MutableList<ReaderPage?>, result: MutableList<ReaderUiItem>) {
        if (buffer.isEmpty()) return
        val first = buffer[0] ?: return
        val second = buffer.getOrNull(1)
        result.add(ReaderUiItem.Page(first, second))
        buffer.clear()
    }
}
```

---

## 5. Technical Footprint & Integration

1. **[`ComposePagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposePagerViewer.kt)**: Replace `viewer` and `downloadManager` parameters with `config: PagerViewerConfigUiModel`; remove `Injekt.get()`.
2. **[`ComposeWebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ComposeWebtoonViewer.kt)**: Replace `viewer` and `downloadManager` parameters with `config: WebtoonViewerConfigUiModel`; remove `Injekt.get()`.
3. **[`ReaderActivity.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt)**: Pass assembled viewer configurations into Compose content.
4. **[`BuildWebtoonItemsUseCase.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/domain/BuildWebtoonItemsUseCase.kt)**: Extract webtoon item composition into a testable pure Kotlin interactor.
5. **[`BuildPagerItemsUseCase.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/domain/BuildPagerItemsUseCase.kt)**: Extract pager item composition and dual-page pairing into a testable pure Kotlin interactor.
6. **[`ReaderUiItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/model/ReaderUiItem.kt)**: Add spread-aware `isEquivalentTo(target)` identity contract.
7. **[`DoublePageLayout.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt)**: Stateless dual-page spread layout composable.

---

## 6. Decoupling for Testability & Pure JVM Testing Strategy

### 6.1 Current Testing Bottlenecks

Testing reader UI item positioning, preloading prepends, and index re-anchoring has historically been blocked because:
- **`WebtoonViewer` constructor requires `activity: ReaderActivity`**, bringing in the entire Android Activity lifecycle and making pure JVM unit testing impossible.
- **Injekt service locator lookups** inside Compose viewers and ViewModels throw `InjektException` when executed without mock injection registries.
- **DisplayMetrics & WindowManager lookups** inside `WebtoonViewer.init` crash under standard JUnit test runners.

### 6.2 Pure JVM Domain Unit Test Suite

By extracting pure Kotlin interactors and value objects, the entire chapter item assembly and list re-anchoring flow can be verified with zero Android dependencies:

#### 1. Webtoon Item Assembly Tests (`BuildWebtoonItemsUseCaseTest`)
Located at `app/src/test/java/eu/kanade/tachiyomi/ui/reader/domain/BuildWebtoonItemsUseCaseTest.kt`:

```kotlin
class BuildWebtoonItemsUseCaseTest {

    private val useCase = BuildWebtoonItemsUseCase()

    @Test
    fun `when only current chapter present, emits only current pages`() {
        val ch = createReaderChapter(1L, pageCount = 5)
        val items = useCase(ViewerChapters(currentChapter = ch))

        assertThat(items).hasSize(5)
        assertThat(items.filterIsInstance<ReaderUiItem.Page>()).hasSize(5)
        assertThat(items.filterIsInstance<ReaderUiItem.Transition>()).isEmpty()
    }

    @Test
    fun `when prevChapter is preloaded, prepends transition header and pages`() {
        val prev = createReaderChapter(1L, pageCount = 10)
        val current = createReaderChapter(2L, pageCount = 20)

        val items = useCase(ViewerChapters(currentChapter = current, prevChapter = prev))

        // 1 transition + 10 prev pages + 20 current pages = 31 items
        assertThat(items).hasSize(31)
        assertThat(items.first()).isInstanceOf(ReaderUiItem.Transition::class.java)
        assertThat((items[1] as ReaderUiItem.Page).page.chapter.chapter.id).isEqualTo(1L)
        assertThat((items[11] as ReaderUiItem.Page).page.chapter.chapter.id).isEqualTo(2L)
    }

    @Test
    fun `when nextChapter is preloaded, appends transition footer and pages`() {
        val current = createReaderChapter(2L, pageCount = 20)
        val next = createReaderChapter(3L, pageCount = 15)

        val items = useCase(ViewerChapters(currentChapter = current, nextChapter = next))

        // 20 current pages + 1 transition + 15 next pages = 36 items
        assertThat(items).hasSize(36)
        assertThat(items[20]).isInstanceOf(ReaderUiItem.Transition::class.java)
        assertThat((items[21] as ReaderUiItem.Page).page.chapter.chapter.id).isEqualTo(3L)
    }
}
```

#### 2. Item Equivalence & Re-anchoring Tests (`ReaderUiItemEquivalenceTest`)
Located at `app/src/test/java/eu/kanade/tachiyomi/ui/reader/model/ReaderUiItemEquivalenceTest.kt`:

```kotlin
class ReaderUiItemEquivalenceTest {

    @Test
    fun `items with identical page and chapter are equivalent`() {
        val pageA = ReaderUiItem.Page(createPage(index = 5, chapterId = 10L))
        val pageB = ReaderUiItem.Page(createPage(index = 5, chapterId = 10L))

        assertThat(pageA.isEquivalentTo(pageB)).isTrue()
    }

    @Test
    fun `items with different chapters are not equivalent`() {
        val pageA = ReaderUiItem.Page(createPage(index = 5, chapterId = 10L))
        val pageB = ReaderUiItem.Page(createPage(index = 5, chapterId = 11L))

        assertThat(pageA.isEquivalentTo(pageB)).isFalse()
    }

    @Test
    fun `re-anchoring finds correct index when previous chapter is prepended`() {
        val initialItems = listOf(
            ReaderUiItem.Page(createPage(index = 0, chapterId = 2L)),
            ReaderUiItem.Page(createPage(index = 1, chapterId = 2L)),
        )
        val anchorItem = initialItems[1]

        // Prepend 50 pages from previous chapter
        val prependedPages = (0 until 50).map { ReaderUiItem.Page(createPage(index = it, chapterId = 1L)) }
        val updatedItems = prependedPages + initialItems

        val reanchoredIndex = updatedItems.indexOfFirst { it.isEquivalentTo(anchorItem) }
        assertThat(reanchoredIndex).isEqualTo(51)
    }

    @Test
    fun `re-anchoring preserves position when dual page pairs are shifted`() {
        val p4 = createPage(index = 4, chapterId = 1L)
        val p5 = createPage(index = 5, chapterId = 1L)
        val p6 = createPage(index = 6, chapterId = 1L)

        // Before shift: [p4, p5]
        val activeItem = ReaderUiItem.Page(p4, p5)

        // After shift: [p3, p4], [p5, p6]
        val shiftedItems = listOf(
            ReaderUiItem.Page(createPage(index = 3, chapterId = 1L), p4),
            ReaderUiItem.Page(p5, p6),
        )

        // User was looking at the spread containing p5; equivalence finds the pair containing p5
        val reanchoredIndex = shiftedItems.indexOfFirst { it.isEquivalentTo(activeItem) }
        assertThat(reanchoredIndex).isEqualTo(0) // p4 matches first pair
    }
}
```

### 6.3 Compose UI Headless Testing

Once decoupled from `WebtoonViewer`, `ComposeWebtoonViewer` can be rendered in headless Compose tests via `runComposeUiTest`:

```kotlin
@RunWith(AndroidJUnit4::class)
class ComposeWebtoonViewerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenPrependingItems_activeChapterItemRemainsInView() {
        // Render stateless ComposeWebtoonViewer with mock configuration
        // Verify LazyColumn re-anchoring behavior
    }
}
```

---

## 7. Implementation Plan & Milestones

### Prerequisites & Sequential Placement
> [!IMPORTANT]
> **Execution Placement:** **Reader Track — Phase R1 (Core Navigation, Engine & Viewers), Step R6**  
> **Prerequisites:** Steps R1, R2, R4, and R5.  
> **Unlocks:** Reader Track Phase R2 (Auxiliary Sheets & Overlays, Steps R7–R10) and Step R11 ([`native_compose_webtoon_subsampling_renderer_proposal.md`](native_compose_webtoon_subsampling_renderer_proposal.md)).  
>
> Decoupling `ComposePagerViewer` and `ComposeWebtoonViewer` from legacy View classes (`PagerViewer`, `WebtoonViewer`), `DownloadManager`, and `Injekt` isolates viewer rendering into pure Jetpack Compose components driven by `PagerViewerConfigUiModel` / `WebtoonViewerConfigUiModel`, completing core viewer decoupling.

- [ ] **Step 1**: Define `PagerViewerConfigUiModel` and `WebtoonViewerConfigUiModel` with complete dual-page preferences.
- [ ] **Step 2**: Implement `ReaderUiItem.isEquivalentTo` domain identity contract supporting dual pages and pure JVM tests.
- [ ] **Step 3a**: Extract `BuildWebtoonItemsUseCase` domain interactor and add `BuildWebtoonItemsUseCaseTest`.
- [ ] **Step 3b**: Extract `BuildPagerItemsUseCase` domain interactor with dual-page chunking and add `BuildPagerItemsUseCaseTest`.
- [ ] **Step 4**: Hoist preference observation from viewers to `ReaderViewModel`.
- [ ] **Step 5**: Refactor `ComposePagerViewer.kt` to eliminate legacy view and service dependencies.
- [ ] **Step 6**: Refactor `ComposeWebtoonViewer.kt` to eliminate legacy view and service dependencies.
- [ ] **Step 7**: Deprecate `WebtoonViewer` and `PagerViewer`.
- [ ] **Step 8**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
