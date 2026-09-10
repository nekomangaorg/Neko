package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import androidx.compose.foundation.lazy.LazyListItemInfo
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import kotlin.jvm.JvmName
import kotlin.math.abs

/**
 * Lightweight viewport bounds contract representing a rendered item in the Webtoon reader.
 * Decoupled from Jetpack Compose UI to enable zero-allocation domain calculations and unit testing.
 */
data class VisibleItemBounds(
    val index: Int,
    val offset: Int,
    val size: Int,
) {
    val bottom: Int
        get() = offset + size

    val middle: Int
        get() = offset + size / 2
}

/**
 * Pure domain resolver for determining the active page or item in the Webtoon continuous vertical
 * strip. Resolves edge cases such as LazyColumn backward clamping (Issue #3347) and short chapter
 * auto-completion in a single-pass O(N) traversal with zero heap allocations.
 */
object WebtoonActiveItemResolver {

    /**
     * Resolves the active index from a list of [VisibleItemBounds] (used in pure JVM unit tests).
     */
    fun resolveActiveIndex(
        visibleItems: List<VisibleItemBounds>,
        currentItems: List<ReaderUiItem>,
        activeChapterId: Long?,
        viewportStartOffset: Int,
        viewportEndOffset: Int,
        firstVisibleIndex: Int,
        firstVisibleScrollOffset: Int,
    ): Int {
        if (visibleItems.isEmpty()) return firstVisibleIndex
        return resolveActiveIndexInternal(
            itemCount = visibleItems.size,
            getItemIndex = { visibleItems[it].index },
            getItemOffset = { visibleItems[it].offset },
            getItemSize = { visibleItems[it].size },
            currentItems = currentItems,
            activeChapterId = activeChapterId,
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
            firstVisibleIndex = firstVisibleIndex,
            firstVisibleScrollOffset = firstVisibleScrollOffset,
        )
    }

    /**
     * Resolves the active index directly from Compose's [LazyListItemInfo] list. Operates with zero
     * heap allocations by accessing primitive item properties directly.
     */
    @JvmName("resolveActiveIndexLazyList")
    fun resolveActiveIndex(
        visibleItems: List<LazyListItemInfo>,
        currentItems: List<ReaderUiItem>,
        activeChapterId: Long?,
        viewportStartOffset: Int,
        viewportEndOffset: Int,
        firstVisibleIndex: Int,
        firstVisibleScrollOffset: Int,
    ): Int {
        if (visibleItems.isEmpty()) return firstVisibleIndex
        return resolveActiveIndexInternal(
            itemCount = visibleItems.size,
            getItemIndex = { visibleItems[it].index },
            getItemOffset = { visibleItems[it].offset },
            getItemSize = { visibleItems[it].size },
            currentItems = currentItems,
            activeChapterId = activeChapterId,
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
            firstVisibleIndex = firstVisibleIndex,
            firstVisibleScrollOffset = firstVisibleScrollOffset,
        )
    }

    private inline fun resolveActiveIndexInternal(
        itemCount: Int,
        getItemIndex: (Int) -> Int,
        getItemOffset: (Int) -> Int,
        getItemSize: (Int) -> Int,
        currentItems: List<ReaderUiItem>,
        activeChapterId: Long?,
        viewportStartOffset: Int,
        viewportEndOffset: Int,
        firstVisibleIndex: Int,
        firstVisibleScrollOffset: Int,
    ): Int {
        val viewportMiddle = (viewportStartOffset + viewportEndOffset) / 2

        // Single-pass tracking variables (zero heap allocations)
        var firstCurrentItemIndex = -1
        var lastCurrentItemIndex = -1
        var lastCurrentItemBottom = Int.MIN_VALUE

        var itemSpanningMiddleIndex = -1
        var closestDistanceToMiddle = Int.MAX_VALUE
        var closestItemIndex = -1

        var closestNonPrecedingDistance = Int.MAX_VALUE
        var closestNonPrecedingIndex = -1

        var closestCurrentDistance = Int.MAX_VALUE
        var closestCurrentIndex = -1

        for (i in 0 until itemCount) {
            val itemIndex = getItemIndex(i)
            val itemOffset = getItemOffset(i)
            val itemSize = getItemSize(i)
            val itemBottom = itemOffset + itemSize
            val itemMiddle = itemOffset + itemSize / 2

            val uiItem = currentItems.getOrNull(itemIndex)

            val isCurrentChapterPage =
                activeChapterId != null &&
                    (uiItem is ReaderUiItem.Page || uiItem is ReaderUiItem.SplitPage) &&
                    uiItem.chapterId == activeChapterId

            val dist = abs(itemMiddle - viewportMiddle)

            if (isCurrentChapterPage) {
                if (firstCurrentItemIndex == -1) {
                    firstCurrentItemIndex = itemIndex
                }
                lastCurrentItemIndex = itemIndex
                lastCurrentItemBottom = itemBottom

                if (dist < closestCurrentDistance) {
                    closestCurrentDistance = dist
                    closestCurrentIndex = itemIndex
                }
            }

            if (firstCurrentItemIndex != -1 && itemIndex >= firstCurrentItemIndex) {
                if (dist < closestNonPrecedingDistance) {
                    closestNonPrecedingDistance = dist
                    closestNonPrecedingIndex = itemIndex
                }
            }

            // Check if item spans viewport middle
            if (viewportMiddle in itemOffset until itemBottom) {
                itemSpanningMiddleIndex = itemIndex
            }

            // Track closest item to viewport middle overall
            if (dist < closestDistanceToMiddle) {
                closestDistanceToMiddle = dist
                closestItemIndex = itemIndex
            }
        }

        val hasCurrentChapterItems = firstCurrentItemIndex != -1

        // Scenario 1: Reader is anchored at the start of the current chapter
        if (
            hasCurrentChapterItems &&
                firstVisibleIndex == firstCurrentItemIndex &&
                firstVisibleScrollOffset == 0
        ) {
            return firstCurrentItemIndex
        }

        // Scenario 2: Item spanning middle is valid
        if (itemSpanningMiddleIndex != -1) {
            val isPrecedingItemWhileChapterVisible =
                hasCurrentChapterItems && itemSpanningMiddleIndex < firstCurrentItemIndex

            if (!isPrecedingItemWhileChapterVisible) {
                return itemSpanningMiddleIndex
            }
        }

        // Scenario 3: Viewport middle is beyond the current chapter content
        if (hasCurrentChapterItems) {
            if (viewportMiddle >= lastCurrentItemBottom) {
                return if (closestNonPrecedingIndex != -1) closestNonPrecedingIndex
                else lastCurrentItemIndex
            }

            return closestCurrentIndex
        }

        // Scenario 4: Current chapter completely scrolled off-screen
        return if (closestItemIndex != -1) closestItemIndex else getItemIndex(0)
    }
}
