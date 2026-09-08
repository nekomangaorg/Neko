package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import java.util.Collections
import okio.buffer
import okio.source

/**
 * Pure domain controller for Webtoon reader item generation, transitions, and tall-page splitting.
 * Decoupled from Android View and RecyclerView hierarchies.
 */
class ReaderWebtoonController {

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
        nonTallPages.clear()
        val newItems = mutableListOf<ReaderUiItem>()

        val prevHasMissingChapters = hasMissingChapters(chapters.currChapter, chapters.prevChapter)
        val nextHasMissingChapters = hasMissingChapters(chapters.nextChapter, chapters.currChapter)

        // Add previous chapter pages
        if (chapters.prevChapter != null) {
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                newItems.addAll(mapPagesToItems(prevPages, screenHeight))
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
            newItems.addAll(mapPagesToItems(currPages, screenHeight))
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and pages
        val nextTrans = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
        nextTransition = nextTrans
        if (
            chapters.nextChapter == null ||
                nextHasMissingChapters ||
                forceTransition ||
                chapters.nextChapter.state !is ReaderChapter.State.Loaded
        ) {
            newItems.add(ReaderUiItem.Transition(nextTrans))
        }

        if (chapters.nextChapter != null) {
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(mapPagesToItems(nextPages, screenHeight))
            }
        }

        return newItems
    }

    private fun mapPagesToItems(pages: List<ReaderPage>, screenHeight: Int): List<ReaderUiItem> {
        return pages.flatMap { page ->
            if (screenHeight > 0 && page.status == Page.State.READY && page.stream != null) {
                val result = checkAndTrackTallPage(page, screenHeight)
                if (result is TallSplitResult.Split) {
                    return@flatMap result.splits.map { ReaderUiItem.SplitPage(it) }
                }
            }
            listOf(ReaderUiItem.Page(page))
        }
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
            (it as? ReaderUiItem.Page)?.page == originalPage
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
            val splits = Companion.checkTallPage(page, screenHeight, maxTextureSize)
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
                is ReaderUiItem.Page -> it.page == page
                is ReaderUiItem.SplitPage -> it.page == page
                is ReaderUiItem.Transition -> false
            }
        }
    }

    companion object {
        /**
         * Inspects [page] image headers and calculates [ReaderPageSplit] slices if the image is
         * taller than [screenHeight] * 2 or exceeds [maxTextureSize].
         */
        fun checkTallPage(
            page: ReaderPage,
            screenHeight: Int,
            maxTextureSize: Int = GLUtil.maxTextureSize,
        ): List<ReaderPageSplit>? {
            val streamFn = page.stream ?: return null
            val options =
                try {
                    streamFn().source().buffer().use { ImageUtil.extractImageOptions(it) }
                } catch (_: Exception) {
                    return null
                }
            return computeSplits(
                page,
                options.outWidth,
                options.outHeight,
                screenHeight,
                maxTextureSize,
            )
        }

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
        ): List<ReaderPageSplit>? {
            if (outHeight <= 0 || outWidth <= 0) return null
            val displayMaxHeight =
                if (screenHeight > 0) {
                    minOf(maxOf(screenHeight * 2, 4096), maxTextureSize)
                } else {
                    maxTextureSize
                }
            val isTall =
                (outHeight.toFloat() / outWidth.toFloat() > 3f) || (outHeight > maxTextureSize)
            if (!isTall || outHeight <= displayMaxHeight) {
                return null
            }

            val maxSliceHeight = minOf(displayMaxHeight, maxTextureSize)
            val partCount = (outHeight - 1) / maxSliceHeight + 1
            if (partCount <= 1) return null

            val optimalSplitHeight = outHeight / partCount
            val splits = mutableListOf<ReaderPageSplit>()
            for (i in 0 until partCount) {
                val topOffset = i * optimalSplitHeight
                val splitH =
                    if (i == partCount - 1) {
                        outHeight - topOffset
                    } else {
                        optimalSplitHeight
                    }
                val split =
                    ReaderPageSplit(page = page, topOffset = topOffset, splitHeight = splitH)
                split.aspectRatio = outWidth.toFloat() / splitH.toFloat()
                splits.add(split)
            }
            return splits
        }
    }
}
