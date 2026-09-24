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
    ): AnchorTarget? {
        if (items.isEmpty()) return null

        val currentItem = items.getOrNull(currentVisibleIndex)
        val activeItem = lastActiveItem

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
                if (trans is ChapterTransition.Next) {
                    val firstIndex = items.indexOfFirst { it.chapterId == toChapterId }
                    if (firstIndex != -1) {
                        return AnchorTarget(firstIndex, items[firstIndex])
                    }
                } else if (trans is ChapterTransition.Prev) {
                    val lastIndex = items.indexOfLast { it.chapterId == toChapterId }
                    if (lastIndex != -1) {
                        return AnchorTarget(lastIndex, items[lastIndex])
                    }
                }
            }
        }

        return null
    }
}
