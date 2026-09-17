package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.domain.BuildWebtoonItemsUseCase
import eu.kanade.tachiyomi.ui.reader.domain.CheckTallPageUseCase
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.util.system.GLUtil
import java.util.Collections

/**
 * Pure domain controller for Webtoon reader item generation, transitions, and tall-page splitting.
 * Decoupled from Android View and RecyclerView hierarchies.
 */
class ReaderWebtoonController(
    private val checkTallPageUseCase: CheckTallPageUseCase = CheckTallPageUseCase(),
    private val buildWebtoonItemsUseCase: BuildWebtoonItemsUseCase =
        BuildWebtoonItemsUseCase(checkTallPageUseCase),
) {

    sealed interface TallSplitResult {
        data class Split(val splits: List<ReaderPageSplit>) : TallSplitResult

        data object NotTall : TallSplitResult

        data object AlreadySplit : TallSplitResult
    }

    var prevTransition: ChapterTransition.Prev? = null
        private set

    var nextTransition: ChapterTransition.Next? = null
        private set

    var currentChapter: ReaderChapter? = null
        private set

    /** Tracks which pages have already been tall-split to prevent re-splitting on rebind. */
    val tallSplitPages: MutableSet<ReaderPage> =
        Collections.synchronizedSet(mutableSetOf<ReaderPage>())

    /** Tracks which pages have been verified as non-tall to avoid redundant split checks. */
    val nonTallPages: MutableSet<ReaderPage> =
        Collections.synchronizedSet(mutableSetOf<ReaderPage>())

    private val splitCheckLock = Any()

    private var hadTransitionForNext = false

    /**
     * Builds the list of [ReaderUiItem] for the given [chapters]. Handles previous chapter padding
     * pages, transition pages, and next chapter peek pages. Reuses any already computed splits from
     * [existingItems] to maintain key and layout continuity.
     */
    fun buildItems(
        chapters: ViewerChapters,
        forceTransition: Boolean,
        screenHeight: Int = 0,
        existingItems: List<ReaderUiItem> = emptyList(),
    ): List<ReaderUiItem> {
        tallSplitPages.clear()
        nonTallPages.clear()
        if (currentChapter?.chapter?.id != chapters.currChapter.chapter.id) {
            hadTransitionForNext = false
        }

        val result =
            buildWebtoonItemsUseCase(
                chapters = chapters,
                forceTransition = forceTransition,
                screenHeight = screenHeight,
                existingItems = existingItems,
                hadTransitionForNext = hadTransitionForNext,
            )

        prevTransition = result.prevTransition
        nextTransition = result.nextTransition
        currentChapter = chapters.currChapter
        hadTransitionForNext = result.hadTransitionForNext
        tallSplitPages.addAll(result.tallSplitPages)

        return result.items
    }

    /**
     * Splits [originalPage] into [insertPages] within [currentItems]. If [insertPages] begins at
     * topOffset == 0, it replaces the monolithic [originalPage]. Otherwise, it inserts the slices
     * directly after [originalPage].
     */
    fun splitPage(
        currentItems: List<ReaderUiItem>,
        originalPage: ReaderPage,
        insertPages: List<ReaderPageSplit>,
    ): List<ReaderUiItem> {
        val position = currentItems.indexOfFirst {
            (it as? ReaderUiItem.Page)?.page?.isFromSamePage(originalPage) == true
        }
        if (position < 0) return currentItems

        val newItems = currentItems.toMutableList()
        val splitItems = insertPages.map { ReaderUiItem.SplitPage(it) }
        if (insertPages.isNotEmpty() && insertPages.first().topOffset == 0) {
            newItems.removeAt(position)
            newItems.addAll(position, splitItems)
        } else {
            newItems.addAll(position + 1, splitItems)
        }
        tallSplitPages.add(originalPage)
        return newItems
    }

    /**
     * Inspects [page] image headers and determines if it exceeds height thresholds. Returns
     * [TallSplitResult.AlreadySplit] if the page was already split, [TallSplitResult.Split] with
     * slices if the page is tall, or [TallSplitResult.NotTall] otherwise.
     */
    fun checkAndTrackTallPage(
        page: ReaderPage,
        screenHeight: Int,
        maxTextureSize: Int = GLUtil.maxTextureSize,
    ): TallSplitResult {
        synchronized(splitCheckLock) {
            if (tallSplitPages.contains(page)) return TallSplitResult.AlreadySplit
            if (nonTallPages.contains(page)) return TallSplitResult.NotTall
            val splits = checkTallPageUseCase(page, screenHeight, maxTextureSize)
            return if (splits != null) {
                tallSplitPages.add(page)
                TallSplitResult.Split(splits)
            } else {
                nonTallPages.add(page)
                TallSplitResult.NotTall
            }
        }
    }

    /** Returns true if [page] has already been verified as non-tall. */
    fun isNonTall(page: ReaderPage): Boolean = nonTallPages.contains(page)

    /** Finds the index of [page] in [items]. */
    fun findPageIndex(items: List<ReaderUiItem>, page: ReaderPage): Int {
        return items.indexOfFirst {
            when (it) {
                is ReaderUiItem.Page -> it.page.isFromSamePage(page)
                is ReaderUiItem.SplitPage -> it.page.isFromSamePage(page)
                is ReaderUiItem.Transition -> false
            }
        }
    }

    companion object {
        private val defaultCheckTallPageUseCase = CheckTallPageUseCase()

        /**
         * Inspects [page] image headers and calculates [ReaderPageSplit] slices if the image is
         * taller than [screenHeight] * 2 or exceeds [maxTextureSize].
         */
        fun checkTallPage(
            page: ReaderPage,
            screenHeight: Int,
            maxTextureSize: Int = GLUtil.maxTextureSize,
        ): List<ReaderPageSplit>? = defaultCheckTallPageUseCase(page, screenHeight, maxTextureSize)

        /**
         * Pure function that calculates optimal slice splits given dimensions and maximum texture
         * sizes.
         */
        fun computeSplits(
            page: ReaderPage,
            outWidth: Int,
            outHeight: Int,
            screenHeight: Int,
            maxTextureSize: Int = GLUtil.maxTextureSize,
        ): List<ReaderPageSplit>? =
            defaultCheckTallPageUseCase.computeSplits(
                page,
                outWidth,
                outHeight,
                screenHeight,
                maxTextureSize,
            )
    }
}
