package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters

/**
 * Pure Kotlin interactor mapping [ViewerChapters] into an ordered sequential list of [ReaderUiItem]
 * instances for the continuous Webtoon viewer. Handles chapter seam padding pages (last 2),
 * transitions, and split reuse to maintain key and layout continuity.
 */
class BuildWebtoonItemsUseCase(
    private val checkTallPage: CheckTallPageUseCase = CheckTallPageUseCase()
) {

    data class Result(
        val items: List<ReaderUiItem>,
        val prevTransition: ChapterTransition.Prev?,
        val nextTransition: ChapterTransition.Next?,
        val hadTransitionForNext: Boolean,
        val tallSplitPages: Set<ReaderPage>,
    )

    operator fun invoke(
        chapters: ViewerChapters,
        forceTransition: Boolean = false,
        screenHeight: Int = 0,
        existingItems: List<ReaderUiItem> = emptyList(),
        hadTransitionForNext: Boolean = false,
    ): Result {
        val tallSplitPages = mutableSetOf<ReaderPage>()
        val newItems = mutableListOf<ReaderUiItem>()

        val prevHasMissingChapters = hasMissingChapters(chapters.currChapter, chapters.prevChapter)
        val nextHasMissingChapters = hasMissingChapters(chapters.nextChapter, chapters.currChapter)

        // Add previous chapter padding pages (last 2 pages) to maintain key continuity across
        // chapter boundaries
        if (chapters.prevChapter != null) {
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null && prevPages.isNotEmpty()) {
                newItems.addAll(
                    mapPagesToItems(
                        pages = prevPages.takeLast(2),
                        existingItems = existingItems,
                        tallSplitPages = tallSplitPages,
                    )
                )
            }
        }

        val prevTrans = ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter)
        newItems.add(ReaderUiItem.Transition(prevTrans))

        // Add current chapter pages
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            newItems.addAll(
                mapPagesToItems(
                    pages = currPages,
                    existingItems = existingItems,
                    tallSplitPages = tallSplitPages,
                )
            )
        }

        // Add next chapter transition and pages
        val nextTrans = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
        val shouldAddNextTransition =
            chapters.nextChapter == null ||
                nextHasMissingChapters ||
                forceTransition ||
                hadTransitionForNext ||
                chapters.nextChapter.state !is ReaderChapter.State.Loaded

        var newHadTransitionForNext = hadTransitionForNext
        if (shouldAddNextTransition) {
            newItems.add(ReaderUiItem.Transition(nextTrans))
            if (chapters.nextChapter != null) {
                newHadTransitionForNext = true
            }
        }

        if (chapters.nextChapter != null) {
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(
                    mapPagesToItems(
                        pages = nextPages,
                        existingItems = existingItems,
                        tallSplitPages = tallSplitPages,
                    )
                )
            }
        }

        return Result(
            items = newItems,
            prevTransition = prevTrans,
            nextTransition = nextTrans,
            hadTransitionForNext = newHadTransitionForNext,
            tallSplitPages = tallSplitPages,
        )
    }

    private fun mapPagesToItems(
        pages: List<ReaderPage>,
        existingItems: List<ReaderUiItem>,
        tallSplitPages: MutableSet<ReaderPage>,
    ): List<ReaderUiItem> {
        return pages.flatMap { page ->
            val existingSplits =
                existingItems.filterIsInstance<ReaderUiItem.SplitPage>().filter {
                    it.page.isFromSamePage(page)
                }
            if (existingSplits.isNotEmpty()) {
                tallSplitPages.add(page)
                return@flatMap existingSplits
            }
            val precomputed = page.precomputedSplits
            if (precomputed != null && precomputed.isNotEmpty()) {
                tallSplitPages.add(page)
                return@flatMap precomputed.map { ReaderUiItem.SplitPage(it) }
            }
            listOf(ReaderUiItem.Page(page))
        }
    }
}
