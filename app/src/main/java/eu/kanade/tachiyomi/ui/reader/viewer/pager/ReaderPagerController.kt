package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.domain.BuildPagerItemsUseCase
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters

/**
 * Pure domain controller for Pager reader page pairing, double-page shifting, and transitions.
 * Decoupled from Android View and ViewPager hierarchies.
 */
class ReaderPagerController(
    private val buildPagerItemsUseCase: BuildPagerItemsUseCase = BuildPagerItemsUseCase()
) {

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
        prevTransition = ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter)
        currentChapter = chapters.currChapter
        nextTransition = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)

        return buildPagerItemsUseCase(
            chapters = chapters,
            doublePages = doublePages,
            splitPages = splitPages,
            shiftDoublePage = shiftDoublePage,
            isRtl = isRtl,
            forceTransition = forceTransition,
            pageToShift = pageToShift,
        )
    }

    fun joinItems(
        items: List<ReaderUiItem>,
        doublePages: Boolean,
        splitPages: Boolean,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
    ): List<ReaderUiItem> {
        return buildPagerItemsUseCase.joinItems(
            items = items,
            doublePages = doublePages,
            splitPages = splitPages,
            shiftDoublePage = shiftDoublePage,
            isRtl = isRtl,
            pageToShift = pageToShift,
        )
    }

    /** Legacy compatibility overload taking raw subItems. */
    @JvmName("joinItemsUntyped")
    fun joinItems(
        subItems: List<Any>,
        doublePages: Boolean,
        splitPages: Boolean,
        shiftDoublePage: Boolean,
        isRtl: Boolean,
    ): List<ReaderUiItem> {
        return buildPagerItemsUseCase.joinItems(
            subItems = subItems,
            doublePages = doublePages,
            splitPages = splitPages,
            shiftDoublePage = shiftDoublePage,
            isRtl = isRtl,
            pageToShift = pageToShift,
        )
    }

    /** Finds the index of [page] in [items]. */
    fun findPageIndex(items: List<ReaderUiItem>, page: ReaderPage): Int {
        return items.indexOfFirst {
            when (it) {
                is ReaderUiItem.Page -> {
                    it.page == page ||
                        it.extraPage == page ||
                        it.page.isFromSamePage(page) ||
                        it.extraPage?.isFromSamePage(page) == true
                }
                is ReaderUiItem.SplitPage -> {
                    it.page == page || it.page.isFromSamePage(page)
                }
                else -> false
            }
        }
    }

    /**
     * Resolves the left-to-right display ordering of [page] and [extraPage] in double-page mode.
     */
    fun getDoublePageOrder(
        page: ReaderPage,
        extraPage: ReaderPage,
        isRtl: Boolean,
        invertDoublePages: Boolean,
    ): Pair<ReaderPage, ReaderPage> {
        val isLTR = (!isRtl).xor(invertDoublePages)
        return if (isLTR) {
            page to extraPage
        } else {
            extraPage to page
        }
    }

    companion object {
        /**
         * Resolves the list of item indices that should be preloaded around [currentIndex], taking
         * into account reading direction ([isRtl]) and [preloadAmount].
         *
         * Indices are prioritized in order:
         * 1. [currentIndex]
         * 2. Ahead pages (in reading direction) up to [preloadAmount]
         * 3. Behind pages (in opposite direction) up to 2
         */
        fun getPreloadIndices(
            currentIndex: Int,
            preloadAmount: Int,
            totalItems: Int,
            isRtl: Boolean,
        ): List<Int> {
            if (totalItems <= 0 || currentIndex !in 0 until totalItems) return emptyList()
            val indices = LinkedHashSet<Int>()
            indices.add(currentIndex)

            val aheadStep = if (isRtl) -1 else 1
            val behindStep = if (isRtl) 1 else -1

            for (step in 1..preloadAmount) {
                val target = currentIndex + step * aheadStep
                if (target in 0 until totalItems) {
                    indices.add(target)
                }
            }
            for (step in 1..2) {
                val target = currentIndex + step * behindStep
                if (target in 0 until totalItems) {
                    indices.add(target)
                }
            }
            return indices.toList()
        }
    }
}
