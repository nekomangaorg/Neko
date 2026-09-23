package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem

/**
 * Modifier extracting edge overscroll physics and direction inversion for paginated chapter
 * navigation.
 */
fun Modifier.pagerOverscrollNavigation(
    pagerState: PagerState,
    isRtl: Boolean,
    isVertical: Boolean,
    items: List<ReaderUiItem>,
    thresholdPx: Float,
    isNavigating: Boolean = false,
    onNavigateToChapter: (Chapter, ChapterNavTarget) -> Unit,
): Modifier = composed {
    val currentItems by rememberUpdatedState(items)
    val currentIsNavigating by rememberUpdatedState(isNavigating)
    val currentOnNavigateToChapter by rememberUpdatedState(onNavigateToChapter)

    val nestedScrollConnection =
        remember(pagerState, isVertical, isRtl, thresholdPx) {
            object : NestedScrollConnection {
                var accumulatedOverscroll = 0f

                private fun checkAndTrigger(delta: Float) {
                    val currentIndex = pagerState.currentPage
                    val currentItem = currentItems.getOrNull(currentIndex)

                    if (currentItem is ReaderUiItem.Transition) {
                        val transition = currentItem.transition
                        val toChapter = transition.to
                        if (toChapter != null) {
                            accumulatedOverscroll += delta
                            val isTrigger =
                                if (isRtl) {
                                    if (transition is ChapterTransition.Prev) {
                                        accumulatedOverscroll < -thresholdPx
                                    } else {
                                        accumulatedOverscroll > thresholdPx
                                    }
                                } else {
                                    if (transition is ChapterTransition.Prev) {
                                        accumulatedOverscroll > thresholdPx
                                    } else {
                                        accumulatedOverscroll < -thresholdPx
                                    }
                                }
                            if (isTrigger && !currentIsNavigating) {
                                accumulatedOverscroll = 0f
                                val navTarget =
                                    if (transition is ChapterTransition.Prev) {
                                        ChapterNavTarget.End
                                    } else {
                                        ChapterNavTarget.Start
                                    }
                                currentOnNavigateToChapter(
                                    toChapter.chapter,
                                    navTarget,
                                )
                            }
                        }
                    }
                }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        val delta = if (isVertical) available.y else available.x
                        val isAtStartEdge = pagerState.currentPage == 0 && delta > 0
                        val isAtEndEdge =
                            pagerState.currentPage == pagerState.pageCount - 1 && delta < 0
                        if (isAtStartEdge || isAtEndEdge) {
                            checkAndTrigger(delta)
                        }
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        val delta = if (isVertical) available.y else available.x
                        if (delta != 0f) {
                            checkAndTrigger(delta)
                        }
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    accumulatedOverscroll = 0f
                    return Velocity.Zero
                }
            }
        }

    nestedScroll(nestedScrollConnection)
}
