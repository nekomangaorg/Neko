package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters

/**
 * Pure domain controller for Webtoon reader item generation, transitions, and tall-page splitting.
 * Decoupled from Android View and RecyclerView hierarchies.
 */
class ReaderWebtoonController {

    var prevTransition: ChapterTransition.Prev? = null
        private set

    var nextTransition: ChapterTransition.Next? = null
        private set

    var currentChapter: ReaderChapter? = null
        private set

    /** Tracks which pages have already been tall-split to prevent re-splitting on rebind. */
    val tallSplitPages = mutableSetOf<ReaderPage>()

    /**
     * Builds the list of [ReaderUiItem] for the given [chapters]. Handles previous chapter padding
     * pages, transition pages, and next chapter peek pages.
     */
    fun buildItems(
        chapters: ViewerChapters,
        forceTransition: Boolean,
        screenHeight: Int = 0,
    ): List<ReaderUiItem> {
        tallSplitPages.clear()
        val newItems = mutableListOf<ReaderUiItem>()

        val prevHasMissingChapters = hasMissingChapters(chapters.currChapter, chapters.prevChapter)
        val nextHasMissingChapters = hasMissingChapters(chapters.nextChapter, chapters.currChapter)

        // Add previous chapter pages
        if (chapters.prevChapter != null) {
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                var accumulatedHeight = 0
                var pagesToTake = 0
                for (page in prevPages.reversed()) {
                    if (page.renderedHeight > 0) {
                        accumulatedHeight += page.renderedHeight
                        pagesToTake++
                        if (screenHeight > 0 && accumulatedHeight >= screenHeight) break
                    }
                }
                val takeCount = maxOf(2, pagesToTake)
                newItems.addAll(prevPages.takeLast(takeCount).map { ReaderUiItem.Page(it) })
            }
        }

        // Add transition page if forced, missing chapters, or previous chapter is not loaded
        val prevTrans = ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter)
        prevTransition = prevTrans
        if (
            chapters.prevChapter == null ||
                prevHasMissingChapters ||
                forceTransition ||
                chapters.prevChapter.state !is ReaderChapter.State.Loaded
        ) {
            newItems.add(ReaderUiItem.Transition(prevTrans))
        }

        // Add current chapter pages
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            newItems.addAll(currPages.map { ReaderUiItem.Page(it) })
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
            newItems.add(ReaderUiItem.Transition(nextTrans))
        }

        if (chapters.nextChapter != null) {
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2).map { ReaderUiItem.Page(it) })
            }
        }

        return newItems
    }

    /**
     * Inserts [insertPages] after [originalPage] in the [currentItems] list. Called when a tall
     * page is split into multiple chunks.
     */
    fun splitPage(
        currentItems: List<ReaderUiItem>,
        originalPage: ReaderPage,
        insertPages: List<ReaderPageSplit>,
    ): List<ReaderUiItem> {
        val position = currentItems.indexOfFirst {
            (it as? ReaderUiItem.Page)?.page == originalPage
        }
        if (position < 0) return currentItems

        val newItems = currentItems.toMutableList()
        val splitItems = insertPages.map { ReaderUiItem.SplitPage(it) }
        newItems.addAll(position + 1, splitItems)
        tallSplitPages.add(originalPage)
        return newItems
    }

    /** Finds the index of [page] in [items]. */
    fun findPageIndex(items: List<ReaderUiItem>, page: ReaderPage): Int {
        return items.indexOfFirst {
            when (it) {
                is ReaderUiItem.Page -> it.page == page
                is ReaderUiItem.SplitPage -> it.page == page
                is ReaderUiItem.Transition -> false
            }
        }
    }
}
