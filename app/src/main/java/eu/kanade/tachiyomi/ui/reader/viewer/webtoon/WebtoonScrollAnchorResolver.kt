package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.isEquivalentTo
import eu.kanade.tachiyomi.ui.reader.model.isSameChapter

/**
 * Pure domain resolver that computes the target scroll index and sub-pixel offset needed to
 * preserve the user's reading position across dynamic list updates (chapter prepends, appends, tall
 * page slice insertions, or chapter seam pruning).
 */
object WebtoonScrollAnchorResolver {

    data class AnchorTarget(
        val index: Int,
        val offset: Int,
        val item: ReaderUiItem,
    )

    /**
     * Calculates the new target index and offset when [items] changes, to seamlessly anchor the
     * viewport to the previously read item. Returns null if Compose already maintained the anchor
     * or no re-anchoring is required.
     */
    fun resolveReanchorTarget(
        items: List<ReaderUiItem>,
        lastFirstVisibleItem: ReaderUiItem?,
        lastFirstVisibleOffset: Int,
        lastActiveItem: ReaderUiItem?,
        activeChapterId: Long?,
        currentFirstVisibleIndex: Int,
    ): AnchorTarget? {
        val currentFirstVisibleItem = items.getOrNull(currentFirstVisibleIndex)
        val previousFirstItem = lastFirstVisibleItem
        if (
            currentFirstVisibleItem != null &&
                previousFirstItem != null &&
                currentFirstVisibleItem.isEquivalentTo(previousFirstItem)
        ) {
            return null
        }

        val firstItem = lastFirstVisibleItem ?: currentFirstVisibleItem
        val activeItem = lastActiveItem

        // Case A: The previously visible monolithic Page was split into slices.
        // Find the slice that covers the exact scroll offset into that page and preserve
        // sub-pixel position.
        if (firstItem is ReaderUiItem.Page) {
            val pageOffset = lastFirstVisibleOffset
            val matchingSliceIndex = items.indexOfFirst {
                it is ReaderUiItem.SplitPage &&
                    isSameChapter(it.page.chapter, firstItem.page.chapter) &&
                    it.page.index == firstItem.page.index &&
                    pageOffset in
                        it.split.topOffset until (it.split.topOffset + it.split.splitHeight)
            }
            if (matchingSliceIndex != -1) {
                val slice = (items[matchingSliceIndex] as ReaderUiItem.SplitPage).split
                val sliceOffset = (pageOffset - slice.topOffset).coerceAtLeast(0)
                return AnchorTarget(matchingSliceIndex, sliceOffset, items[matchingSliceIndex])
            }
        }

        // Case B: The previously visible SplitPage slice was replaced by a monolithic Page.
        if (firstItem is ReaderUiItem.SplitPage) {
            val pageIndex = items.indexOfFirst {
                it is ReaderUiItem.Page &&
                    isSameChapter(it.page.chapter, firstItem.page.chapter) &&
                    it.page.index == firstItem.page.index
            }
            if (pageIndex != -1) {
                val targetOffset = firstItem.split.topOffset + lastFirstVisibleOffset
                return AnchorTarget(pageIndex, targetOffset, items[pageIndex])
            }
        }

        var targetIndex: Int
        var targetOffset: Int

        val firstItemNewIndex =
            firstItem?.let { target -> items.indexOfFirst { it.isEquivalentTo(target) } } ?: -1

        if (firstItemNewIndex != -1) {
            targetIndex = firstItemNewIndex
            targetOffset = lastFirstVisibleOffset
        } else if (activeItem != null) {
            val activeItemNewIndex = items.indexOfFirst { it.isEquivalentTo(activeItem) }
            if (activeItemNewIndex != -1) {
                targetIndex = activeItemNewIndex
                targetOffset = 0
            } else {
                targetIndex = -1
                targetOffset = 0
            }
        } else {
            targetIndex = -1
            targetOffset = 0
        }

        // Fallback: If target was an adjacent transition that was replaced by newly loaded pages,
        // anchor directly to the first page of that newly loaded chapter.
        val anchorItem = firstItem ?: activeItem
        if (targetIndex == -1 && anchorItem is ReaderUiItem.Transition) {
            val trans = anchorItem.transition
            if (trans is ChapterTransition.Next && trans.to != null) {
                val toChapterId = trans.to.chapter.id
                val nextChapterFirstPageIndex = items.indexOfFirst {
                    (it as? ReaderUiItem.Page)?.page?.chapter?.chapter?.id == toChapterId ||
                        (it as? ReaderUiItem.SplitPage)?.page?.chapter?.chapter?.id == toChapterId
                }
                if (nextChapterFirstPageIndex != -1) {
                    targetIndex = nextChapterFirstPageIndex
                    targetOffset = 0
                }
            }
        }

        // Obsolete chapter fallback: If target was in an obsolete chapter that is no longer
        // present,
        // safely anchor to the start of the active chapter.
        if (targetIndex == -1) {
            val anchor = firstItem ?: activeItem
            if (anchor != null) {
                val anchorChapterId =
                    anchor.chapterId
                        ?: (anchor as? ReaderUiItem.Transition)?.transition?.from?.chapter?.id
                if (
                    anchorChapterId != null &&
                        activeChapterId != null &&
                        anchorChapterId != activeChapterId
                ) {
                    val activeStart = items.indexOfFirst { it.chapterId == activeChapterId }
                    targetIndex = if (activeStart != -1) activeStart else 0
                    targetOffset = 0
                }
            }
        }

        if (targetIndex != -1) {
            val targetItem = items.getOrNull(targetIndex)
            if (targetItem != null) {
                return AnchorTarget(targetIndex, targetOffset, targetItem)
            }
        }
        return null
    }
}
