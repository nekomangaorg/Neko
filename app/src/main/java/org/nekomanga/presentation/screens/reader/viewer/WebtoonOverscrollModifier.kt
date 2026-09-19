package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import org.nekomanga.presentation.theme.Size

/**
 * Modifier extracting continuous pull-to-overscroll physics at the start of the list for seamless
 * chapter navigation.
 */
fun Modifier.webtoonOverscrollNavigation(
    lazyListState: LazyListState,
    items: List<ReaderUiItem>,
    onNavigateToChapter: ((Chapter, ChapterNavTarget) -> Unit)? = null,
    onNavigateAdjacent: (forward: Boolean) -> Unit,
): Modifier = composed {
    val density = LocalDensity.current
    val overscrollThresholdPx = remember(density) { with(density) { (Size.huge * 2).toPx() } }

    val connection =
        remember(lazyListState, items, overscrollThresholdPx) {
            object : NestedScrollConnection {
                var pullOffset = 0f

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (pullOffset > 0 && available.y < 0) {
                        val consumedY = available.y.coerceAtLeast(-pullOffset)
                        pullOffset += consumedY
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (
                        source == NestedScrollSource.UserInput &&
                            available.y > 0 &&
                            lazyListState.firstVisibleItemIndex == 0 &&
                            lazyListState.firstVisibleItemScrollOffset == 0
                    ) {
                        pullOffset += available.y
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val offset = pullOffset
                    pullOffset = 0f
                    if (offset > overscrollThresholdPx) {
                        val prevChapter =
                            (items.firstOrNull {
                                    it is ReaderUiItem.Transition &&
                                        it.transition is ChapterTransition.Prev
                                } as? ReaderUiItem.Transition)
                                ?.transition
                                ?.to
                                ?.chapter
                                ?: (items.firstOrNull() as? ReaderUiItem.Page)
                                    ?.page
                                    ?.chapter
                                    ?.chapter
                        if (prevChapter != null) {
                            if (onNavigateToChapter != null) {
                                onNavigateToChapter(prevChapter, ChapterNavTarget.End)
                            } else {
                                onNavigateAdjacent(false)
                            }
                            return available
                        }
                    }
                    return Velocity.Zero
                }
            }
        }

    nestedScroll(connection)
}
