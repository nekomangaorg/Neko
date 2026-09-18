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
        previousItems: List<ReaderUiItem>? = null,
    ): AnchorTarget? {
        val currentFirstVisibleItem = items.getOrNull(currentFirstVisibleIndex)
        val previousFirstItem = lastFirstVisibleItem

        // Fast-path 1: Native Compose key tracking or caller already positioned at equivalent item.
        if (
            currentFirstVisibleItem != null &&
                previousFirstItem != null &&
                currentFirstVisibleItem::class == previousFirstItem::class &&
                currentFirstVisibleItem.isEquivalentTo(previousFirstItem)
        ) {
            return null
        }

        // Fast-path 2: When items are appended/updated without shifting the current viewport item
        // (e.g. next chapter pages loaded while reading transition or last page).
        val previousItemAtCurrentIndex = previousItems?.getOrNull(currentFirstVisibleIndex)
        if (
            currentFirstVisibleItem != null &&
                previousItemAtCurrentIndex != null &&
                currentFirstVisibleItem::class == previousItemAtCurrentIndex::class &&
                currentFirstVisibleItem.isEquivalentTo(previousItemAtCurrentIndex)
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
            val matchingSliceIndices = items.mapIndexedNotNull { index, item ->
                if (
                    item is ReaderUiItem.SplitPage &&
                        isSameChapter(item.page.chapter, firstItem.page.chapter) &&
                        item.page.index == firstItem.page.index
                ) {
                    index to item
                } else {
                    null
                }
            }
            if (matchingSliceIndices.isNotEmpty()) {
                val totalDisplayedHeight = matchingSliceIndices.sumOf {
                    it.second.split.displayedHeight
                }
                if (totalDisplayedHeight > 0) {
                    var currentScreenTop = 0
                    for ((index, item) in matchingSliceIndices) {
                        val sliceScreenHeight = item.split.displayedHeight
                        if (
                            pageOffset in
                                currentScreenTop until (currentScreenTop + sliceScreenHeight)
                        ) {
                            val sliceOffset = (pageOffset - currentScreenTop).coerceAtLeast(0)
                            return AnchorTarget(index, sliceOffset, item)
                        }
                        currentScreenTop += sliceScreenHeight
                    }
                    val (lastIndex, lastItem) = matchingSliceIndices.last()
                    val lastSliceStart = totalDisplayedHeight - lastItem.split.displayedHeight
                    return AnchorTarget(
                        lastIndex,
                        (pageOffset - lastSliceStart).coerceAtLeast(0),
                        lastItem,
                    )
                } else {
                    val totalBitmapHeight = matchingSliceIndices.maxOf {
                        it.second.split.topOffset + it.second.split.splitHeight
                    }
                    val totalHeight =
                        if (firstItem.page.renderedHeight > 0) firstItem.page.renderedHeight
                        else totalBitmapHeight
                    if (totalHeight > 0 && totalBitmapHeight > 0) {
                        val scale = totalBitmapHeight.toDouble() / totalHeight.toDouble()
                        val bitmapOffset = (pageOffset * scale).toInt()
                        val matching =
                            matchingSliceIndices.firstOrNull { (_, item) ->
                                bitmapOffset in
                                    item.split.topOffset until
                                        (item.split.topOffset + item.split.splitHeight)
                            } ?: matchingSliceIndices.last()

                        val sliceOffsetBitmap =
                            (bitmapOffset - matching.second.split.topOffset).coerceAtLeast(0)
                        val sliceOffsetScreen = (sliceOffsetBitmap / scale).toInt()
                        return AnchorTarget(matching.first, sliceOffsetScreen, matching.second)
                    }
                }
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
                val targetOffset =
                    if (firstItem.split.displayedHeight > 0 && firstItem.split.splitHeight > 0) {
                        val scale =
                            firstItem.split.displayedHeight.toDouble() /
                                firstItem.split.splitHeight.toDouble()
                        val sliceScreenTop = (firstItem.split.topOffset * scale).toInt()
                        sliceScreenTop + lastFirstVisibleOffset
                    } else {
                        firstItem.split.topOffset + lastFirstVisibleOffset
                    }
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
            val toChapter = trans.to
            if (trans is ChapterTransition.Next && toChapter != null) {
                val toChapterId = toChapter.chapter.id
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
                if (
                    targetIndex == currentFirstVisibleIndex &&
                        previousFirstItem != null &&
                        targetItem::class == previousFirstItem::class &&
                        targetItem.isEquivalentTo(previousFirstItem)
                ) {
                    return null
                }
                return AnchorTarget(targetIndex, targetOffset, targetItem)
            }
        }
        return null
    }
}
