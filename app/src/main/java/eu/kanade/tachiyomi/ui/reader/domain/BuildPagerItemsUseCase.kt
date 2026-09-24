package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters
import kotlin.math.max

/**
 * Pure Kotlin interactor mapping [ViewerChapters] into a sequential list of [ReaderUiItem]
 * instances for horizontal and vertical paginated viewers. Handles dual-page spread pairing,
 * wide-spread isolation, pair shifting, and single-page splits.
 */
class BuildPagerItemsUseCase {

    operator fun invoke(
        chapters: ViewerChapters,
        doublePages: Boolean,
        splitPages: Boolean,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
        forceTransition: Boolean = false,
        pageToShift: ReaderPage? = null,
    ): List<ReaderUiItem> {
        val subItems = mutableListOf<ReaderUiItem>()

        val prevHasMissingChapters = hasMissingChapters(chapters.currChapter, chapters.prevChapter)
        val nextHasMissingChapters = hasMissingChapters(chapters.nextChapter, chapters.currChapter)

        // Add previous chapter pages and transition
        if (chapters.prevChapter != null) {
            val prevPages = chapters.prevChapter.pages
            val numberOfFullPages =
                chapters.prevChapter.pages?.count { it.fullPage == true || it.isolatedPage } ?: 0
            if (prevPages != null) {
                val pagesToTake =
                    prevPages.takeLast(if ((prevPages.size + numberOfFullPages) % 2 == 0) 2 else 3)
                subItems.addAll(pagesToTake.map { ReaderUiItem.Page(it) })
            }
        }

        val prevTrans = ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter)
        if (
            chapters.prevChapter == null ||
                prevHasMissingChapters ||
                forceTransition ||
                chapters.prevChapter.state !is ReaderChapter.State.Loaded
        ) {
            subItems.add(ReaderUiItem.Transition(prevTrans))
        }

        // Add current chapter pages
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            subItems.addAll(currPages.map { ReaderUiItem.Page(it) })
        }

        // Add next chapter transition and pages
        val nextTrans = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
        if (
            chapters.nextChapter == null ||
                nextHasMissingChapters ||
                forceTransition ||
                chapters.nextChapter.state !is ReaderChapter.State.Loaded
        ) {
            subItems.add(ReaderUiItem.Transition(nextTrans))
        }

        if (chapters.nextChapter != null) {
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                subItems.addAll(nextPages.take(2).map { ReaderUiItem.Page(it) })
            }
        }

        return joinItems(
            items = subItems,
            doublePages = doublePages,
            splitPages = splitPages,
            shiftDoublePage = shiftDoublePage,
            isRtl = isRtl,
            pageToShift = pageToShift,
        )
    }

    operator fun invoke(
        chapters: ViewerChapters,
        pageLayout: PageLayout,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
        forceTransition: Boolean = false,
        pageToShift: ReaderPage? = null,
    ): List<ReaderUiItem> {
        val isDoublePages = pageLayout == PageLayout.DOUBLE_PAGES
        val isSplitPages = pageLayout == PageLayout.SPLIT_PAGES
        return invoke(
            chapters = chapters,
            doublePages = isDoublePages,
            splitPages = isSplitPages,
            shiftDoublePage = shiftDoublePage,
            isRtl = isRtl,
            forceTransition = forceTransition,
            pageToShift = pageToShift,
        )
    }

    /** Joins and splits pages into [ReaderUiItem]s based on double/split page settings. */
    fun joinItems(
        items: List<ReaderUiItem>,
        doublePages: Boolean,
        splitPages: Boolean,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
        pageToShift: ReaderPage? = null,
    ): List<ReaderUiItem> {
        val result = mutableListOf<ReaderUiItem>()

        if (!doublePages) {
            items.forEach { item ->
                if (item is ReaderUiItem.Page) {
                    item.page.shiftedPage = false
                    item.page.firstHalf = null
                    item.page.endPageConfidence = null
                    item.page.startPageConfidence = null
                }
            }

            if (splitPages) {
                var itemIndex = 0
                val pagedItems = items.toMutableList()
                while (itemIndex < pagedItems.size) {
                    val item = pagedItems[itemIndex]
                    if (item !is ReaderUiItem.Page) {
                        itemIndex++
                        continue
                    }
                    val page = item.page
                    if (page.longPage == true) {
                        page.firstHalf = true
                        pagedItems[itemIndex] =
                            ReaderUiItem.Page(InsertPage(page).apply { firstHalf = true })
                        val secondHalf =
                            ReaderUiItem.Page(InsertPage(page).apply { firstHalf = false })
                        pagedItems.add(itemIndex + 1, secondHalf)
                        itemIndex++
                    }
                    itemIndex++
                }
                result.addAll(pagedItems)
            } else {
                result.addAll(items)
            }

            if (isRtl) {
                result.reverse()
            }
        } else {
            val pagedItems = mutableListOf<MutableList<ReaderPage?>>()
            val transitions = mutableListOf<ChapterTransition>()
            pagedItems.add(mutableListOf())

            // Step 1: segment pages and transition pages
            items.forEach { item ->
                when (item) {
                    is ReaderUiItem.Page -> {
                        val page = item.page
                        if (
                            pagedItems.last().lastOrNull() != null &&
                                pagedItems.last().last()?.chapter?.chapter?.id !=
                                    page.chapter.chapter.id
                        ) {
                            pagedItems.add(mutableListOf())
                        }
                        pagedItems.last().add(page)
                    }
                    is ReaderUiItem.Transition -> {
                        transitions.add(item.transition)
                        pagedItems.add(mutableListOf())
                    }
                    is ReaderUiItem.SplitPage -> {
                        pagedItems.last().add(item.page)
                    }
                }
            }

            var pagedIndex = 0
            val joinedList = mutableListOf<ReaderUiItem>()

            // Step 2: process each set of pages
            pagedItems.forEach { pages ->
                pages.forEach {
                    it?.shiftedPage = false
                    it?.firstHalf = null
                }

                // Step 3: Shift pages if configured
                if (shiftDoublePage) {
                    run loop@{
                        var index = pages.indexOf(pageToShift)
                        if (pageToShift?.fullPage == true) {
                            index = max(0, index - 1)
                        }
                        val fullPageBeforeIndex =
                            max(
                                0,
                                if (index > -1) {
                                    pages.take(index).indexOfLast { it?.fullPage == true }
                                } else {
                                    -1
                                },
                            )
                        (fullPageBeforeIndex until pages.size).forEach {
                            if (pages[it]?.fullPage != true) {
                                pages[it]?.shiftedPage = true
                                return@loop
                            }
                        }
                    }
                }

                // Step 4: Add blanks for chunking
                var itemIndex = 0
                while (itemIndex < pages.size) {
                    pages[itemIndex]?.isolatedPage = false
                    if (
                        pages[itemIndex]?.fullPage == true || pages[itemIndex]?.shiftedPage == true
                    ) {
                        pages.add(itemIndex + 1, null)
                        if (
                            pages[itemIndex]?.fullPage == true &&
                                itemIndex > 0 &&
                                pages[itemIndex - 1] != null &&
                                (itemIndex - 1) % 2 == 0
                        ) {
                            pages[itemIndex - 1]?.isolatedPage = true
                            pages.add(itemIndex, null)
                            itemIndex++
                        }
                        itemIndex++
                    }
                    itemIndex++
                }

                // Step 5: Chunk into pairs
                if (pages.isNotEmpty()) {
                    joinedList.addAll(
                        pages.chunked(2).map { chunk ->
                            val first = chunk.first()!!
                            val second = chunk.getOrNull(1)
                            ReaderUiItem.Page(first, second)
                        }
                    )
                }

                transitions.getOrNull(pagedIndex)?.let { trans ->
                    val lastPage = (joinedList.lastOrNull() as? ReaderUiItem.Page)?.page
                    if (
                        lastPage == null ||
                            (if (trans is ChapterTransition.Next) {
                                trans.from.chapter.id == lastPage.chapter.chapter.id
                            } else {
                                true
                            })
                    ) {
                        joinedList.add(ReaderUiItem.Transition(trans))
                        pagedIndex++
                    }
                }
            }

            if (isRtl) {
                joinedList.reverse()
            }

            result.addAll(joinedList)
        }

        return result
    }

    /** Untyped compatibility overload for legacy callers. */
    @JvmName("joinItemsUntyped")
    fun joinItems(
        subItems: List<Any>,
        doublePages: Boolean,
        splitPages: Boolean,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
        pageToShift: ReaderPage? = null,
    ): List<ReaderUiItem> {
        val typedItems = subItems.mapNotNull { item ->
            when (item) {
                is ReaderUiItem -> item
                is ReaderPage -> ReaderUiItem.Page(item)
                is ChapterTransition -> ReaderUiItem.Transition(item)
                else -> null
            }
        }
        return joinItems(
            items = typedItems,
            doublePages = doublePages,
            splitPages = splitPages,
            shiftDoublePage = shiftDoublePage,
            isRtl = isRtl,
            pageToShift = pageToShift,
        )
    }
}
