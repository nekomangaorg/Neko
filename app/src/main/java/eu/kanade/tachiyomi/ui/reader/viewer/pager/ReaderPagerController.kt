package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters
import kotlin.math.max

/**
 * Pure domain controller for Pager reader page pairing, double-page shifting, and transitions.
 * Decoupled from Android View and ViewPager hierarchies.
 */
class ReaderPagerController {

    var prevTransition: ChapterTransition.Prev? = null
        private set

    var nextTransition: ChapterTransition.Next? = null
        private set

    var currentChapter: ReaderChapter? = null
        private set

    var pageToShift: ReaderPage? = null

    /** Builds the list of [ReaderUiItem] for the given [chapters] and display configuration. */
    fun buildItems(
        chapters: ViewerChapters,
        forceTransition: Boolean,
        doublePages: Boolean,
        splitPages: Boolean,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
    ): List<ReaderUiItem> {
        val subItems = mutableListOf<Any>()

        val prevHasMissingChapters = hasMissingChapters(chapters.currChapter, chapters.prevChapter)
        val nextHasMissingChapters = hasMissingChapters(chapters.nextChapter, chapters.currChapter)

        // Add previous chapter pages and transition
        if (chapters.prevChapter != null) {
            val prevPages = chapters.prevChapter.pages
            val numberOfFullPages =
                chapters.prevChapter.pages?.count { it.fullPage == true || it.isolatedPage } ?: 0
            if (prevPages != null) {
                subItems.addAll(
                    prevPages.takeLast(if ((prevPages.size + numberOfFullPages) % 2 == 0) 2 else 3)
                )
            }
        }

        val prevTrans = ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter)
        prevTransition = prevTrans
        if (
            chapters.prevChapter == null ||
                prevHasMissingChapters ||
                forceTransition ||
                chapters.prevChapter.state !is ReaderChapter.State.Loaded
        ) {
            subItems.add(prevTrans)
        }

        // Add current chapter pages
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            subItems.addAll(currPages)
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and pages
        val nextTrans = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
        nextTransition = nextTrans
        if (
            chapters.nextChapter == null ||
                nextHasMissingChapters ||
                forceTransition ||
                chapters.nextChapter?.state !is ReaderChapter.State.Loaded
        ) {
            subItems.add(nextTrans)
        }

        if (chapters.nextChapter != null) {
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                subItems.addAll(nextPages.take(2))
            }
        }

        return joinItems(
            subItems = subItems,
            doublePages = doublePages,
            splitPages = splitPages,
            shiftDoublePage = shiftDoublePage,
            isRtl = isRtl,
        )
    }

    /** Joins and splits pages into [ReaderUiItem]s based on double/split page settings. */
    fun joinItems(
        subItems: List<Any>,
        doublePages: Boolean,
        splitPages: Boolean,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
    ): List<ReaderUiItem> {
        val result = mutableListOf<ReaderUiItem>()

        if (!doublePages) {
            subItems.forEach {
                (it as? ReaderPage)?.apply {
                    shiftedPage = false
                    firstHalf = null
                    endPageConfidence = null
                    startPageConfidence = null
                }
            }

            if (splitPages) {
                var itemIndex = 0
                val pagedItems = subItems.toMutableList()
                while (itemIndex < pagedItems.size) {
                    val page = pagedItems[itemIndex] as? ReaderPage
                    if (page == null) {
                        itemIndex++
                        continue
                    }
                    if (page.longPage == true) {
                        page.firstHalf = true
                        pagedItems[itemIndex] = InsertPage(page).apply { firstHalf = true }
                        val secondHalf = InsertPage(page).apply { firstHalf = false }
                        pagedItems.add(itemIndex + 1, secondHalf)
                        itemIndex++
                    }
                    itemIndex++
                }
                for (item in pagedItems) {
                    when (item) {
                        is ReaderPage -> result.add(ReaderUiItem.Page(item))
                        is ChapterTransition -> result.add(ReaderUiItem.Transition(item))
                    }
                }
            } else {
                for (item in subItems) {
                    when (item) {
                        is ReaderPage -> result.add(ReaderUiItem.Page(item))
                        is ChapterTransition -> result.add(ReaderUiItem.Transition(item))
                    }
                }
            }

            if (isRtl) {
                result.reverse()
            }
        } else {
            val pagedItems = mutableListOf<MutableList<ReaderPage?>>()
            val otherItems = mutableListOf<Any>()
            pagedItems.add(mutableListOf())

            // Step 1: segment pages and transition pages
            subItems.forEach {
                if (it is ReaderPage) {
                    if (
                        pagedItems.last().lastOrNull() != null &&
                            pagedItems.last().last()?.chapter?.chapter?.id != it.chapter.chapter.id
                    ) {
                        pagedItems.add(mutableListOf())
                    }
                    pagedItems.last().add(it)
                } else {
                    otherItems.add(it)
                    pagedItems.add(mutableListOf())
                }
            }

            var pagedIndex = 0
            val joinedList = mutableListOf<ReaderUiItem>()

            // Step 2: process each set of pages
            pagedItems.forEach { items ->
                items.forEach {
                    it?.shiftedPage = false
                    it?.firstHalf = null
                }

                // Step 3: Shift pages if configured
                if (shiftDoublePage) {
                    run loop@{
                        var index = items.indexOf(pageToShift)
                        if (pageToShift?.fullPage == true) {
                            index = max(0, index - 1)
                        }
                        val fullPageBeforeIndex =
                            max(
                                0,
                                if (index > -1) {
                                    items.take(index).indexOfLast { it?.fullPage == true }
                                } else {
                                    -1
                                },
                            )
                        (fullPageBeforeIndex until items.size).forEach {
                            if (items[it]?.fullPage != true) {
                                items[it]?.shiftedPage = true
                                return@loop
                            }
                        }
                    }
                }

                // Step 4: Add blanks for chunking
                var itemIndex = 0
                while (itemIndex < items.size) {
                    items[itemIndex]?.isolatedPage = false
                    if (
                        items[itemIndex]?.fullPage == true || items[itemIndex]?.shiftedPage == true
                    ) {
                        items.add(itemIndex + 1, null)
                        if (
                            items[itemIndex]?.fullPage == true &&
                                itemIndex > 0 &&
                                items[itemIndex - 1] != null &&
                                (itemIndex - 1) % 2 == 0
                        ) {
                            items[itemIndex - 1]?.isolatedPage = true
                            items.add(itemIndex, null)
                            itemIndex++
                        }
                        itemIndex++
                    }
                    itemIndex++
                }

                // Step 5: Chunk into pairs
                if (items.isNotEmpty()) {
                    joinedList.addAll(
                        items.chunked(2).map { chunk ->
                            val first = chunk.first()!!
                            val second = chunk.getOrNull(1)
                            ReaderUiItem.Page(first, second)
                        }
                    )
                }

                otherItems.getOrNull(pagedIndex)?.let {
                    val lastPage = (joinedList.lastOrNull() as? ReaderUiItem.Page)?.page
                    if (
                        lastPage == null ||
                            (if (it is ChapterTransition.Next) {
                                it.from.chapter.id == lastPage.chapter.chapter.id
                            } else {
                                true
                            })
                    ) {
                        if (it is ChapterTransition) {
                            joinedList.add(ReaderUiItem.Transition(it))
                        }
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

    /** Finds the index of [page] in [items]. */
    fun findPageIndex(items: List<ReaderUiItem>, page: ReaderPage): Int {
        return items.indexOfFirst {
            if (it is ReaderUiItem.Page) {
                it.page == page ||
                    it.extraPage == page ||
                    it.page.isFromSamePage(page) ||
                    it.extraPage?.isFromSamePage(page) == true
            } else {
                false
            }
        }
    }
}
