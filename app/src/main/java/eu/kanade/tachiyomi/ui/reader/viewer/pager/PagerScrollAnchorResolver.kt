package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.isEquivalentTo

/**
 * Pure domain resolver that computes the target scroll index needed to preserve the user's reading
 * position across dynamic list updates (chapter prepends, appends, dual-page shift re-chunking, or
 * transition seam pruning) in paginated viewers.
 */
object PagerScrollAnchorResolver {

    data class AnchorTarget(
        val index: Int,
        val item: ReaderUiItem,
    )

    enum class ChapterBoundary {
        START,
        END,
    }

    /**
     * Resolves the target item index at the specified [boundary] (START or END) for [chapterId],
     * taking the viewer reading direction [isRtl] into account.
     */
    fun resolveChapterBoundaryIndex(
        items: List<ReaderUiItem>,
        chapterId: Long?,
        isRtl: Boolean,
        boundary: ChapterBoundary = ChapterBoundary.START,
    ): Int? {
        if (items.isEmpty() || chapterId == null || chapterId <= 0L) return null
        val isForward = (boundary == ChapterBoundary.START) xor isRtl
        val index =
            if (isForward) {
                items.indexOfFirst { it.chapterId == chapterId }
            } else {
                items.indexOfLast { it.chapterId == chapterId }
            }
        return index.takeIf { it != -1 }
    }

    /**
     * Calculates the new target index when [items] changes, to seamlessly anchor the viewport to
     * the previously read item. Returns null if Compose already maintained the anchor or no
     * re-anchoring is required.
     */
    fun resolveReanchorTarget(
        items: List<ReaderUiItem>,
        lastActiveItem: ReaderUiItem?,
        currentVisibleIndex: Int,
        previousItems: List<ReaderUiItem>? = null,
        isRtl: Boolean = false,
        activeChapterId: Long? = null,
    ): AnchorTarget? {
        if (items.isEmpty()) return null

        val currentItem = items.getOrNull(currentVisibleIndex)
        val activeItem = lastActiveItem

        // If target item belongs to an obsolete chapter, anchor directly to active chapter start
        if (
            activeChapterId != null &&
                activeItem?.chapterId != null &&
                activeItem.chapterId != activeChapterId
        ) {
            val boundaryIndex =
                resolveChapterBoundaryIndex(
                    items = items,
                    chapterId = activeChapterId,
                    isRtl = isRtl,
                    boundary = ChapterBoundary.START,
                )
            if (boundaryIndex != null && boundaryIndex != currentVisibleIndex) {
                return AnchorTarget(boundaryIndex, items[boundaryIndex])
            }
        }

        // Fast-path 1: Native Compose key tracking or caller already positioned at equivalent item.
        if (currentItem != null && activeItem != null && currentItem.isEquivalentTo(activeItem)) {
            return null
        }

        // Fast-path 2: When items are appended/updated without shifting the current viewport item
        // (e.g. next chapter pages appended to end while reading current chapter).
        val previousItemAtCurrent = previousItems?.getOrNull(currentVisibleIndex)
        if (
            currentItem != null &&
                previousItemAtCurrent != null &&
                currentItem::class == previousItemAtCurrent::class &&
                currentItem.isEquivalentTo(previousItemAtCurrent)
        ) {
            return null
        }

        val targetItem = activeItem ?: currentItem ?: return null
        val newIndex = items.indexOfFirst { it.isEquivalentTo(targetItem) }

        if (newIndex != -1 && newIndex != currentVisibleIndex) {
            return AnchorTarget(newIndex, items[newIndex])
        }

        // Fallback: If target was an adjacent transition that was replaced by newly loaded pages,
        // anchor directly to the appropriate boundary page of that newly loaded chapter.
        if (newIndex == -1 && targetItem is ReaderUiItem.Transition) {
            val trans = targetItem.transition
            val toChapter = trans.to
            if (toChapter != null) {
                val toChapterId = toChapter.chapter.id
                val boundary =
                    if (trans is ChapterTransition.Next) {
                        ChapterBoundary.START
                    } else {
                        ChapterBoundary.END
                    }
                val boundaryIndex =
                    resolveChapterBoundaryIndex(
                        items = items,
                        chapterId = toChapterId,
                        isRtl = isRtl,
                        boundary = boundary,
                    )
                if (boundaryIndex != null) {
                    return AnchorTarget(boundaryIndex, items[boundaryIndex])
                }
            }
        }

        // Fallback: If target was obsolete or not found, anchor to the start boundary of the active
        // chapter
        if (newIndex == -1 && activeChapterId != null) {
            val boundaryIndex =
                resolveChapterBoundaryIndex(
                    items = items,
                    chapterId = activeChapterId,
                    isRtl = isRtl,
                    boundary = ChapterBoundary.START,
                )
            if (boundaryIndex != null && boundaryIndex != currentVisibleIndex) {
                return AnchorTarget(boundaryIndex, items[boundaryIndex])
            }
        }

        return null
    }
}
