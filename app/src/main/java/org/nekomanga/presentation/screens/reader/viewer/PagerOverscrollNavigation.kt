package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
@Composable
fun Modifier.pagerOverscrollNavigation(
    pagerState: PagerState,
    isRtl: Boolean,
    isVertical: Boolean,
    items: List<ReaderUiItem>,
    thresholdPx: Float,
    isNavigating: Boolean = false,
    onNavigateToChapter: (Chapter, ChapterNavTarget) -> Unit,
): Modifier {
    val currentItems by rememberUpdatedState(items)
    val currentIsNavigating by rememberUpdatedState(isNavigating)
    val currentOnNavigateToChapter by rememberUpdatedState(onNavigateToChapter)

    val overscrollHandler =
        remember(isRtl, thresholdPx) {
            PagerOverscrollHandler(
                isRtl = isRtl,
                thresholdPx = thresholdPx,
                onNavigateToChapter = { ch, target -> currentOnNavigateToChapter(ch, target) },
            )
        }

    val nestedScrollConnection =
        remember(pagerState, isVertical, overscrollHandler) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        val delta = if (isVertical) available.y else available.x
                        val isAtStartEdge = pagerState.currentPage == 0 && delta > 0
                        val isAtEndEdge =
                            pagerState.currentPage == pagerState.pageCount - 1 && delta < 0
                        if (isAtStartEdge || isAtEndEdge) {
                            overscrollHandler.handleScrollDelta(
                                delta = delta,
                                currentIndex = pagerState.currentPage,
                                items = currentItems,
                                isNavigating = currentIsNavigating,
                            )
                        } else {
                            overscrollHandler.reset()
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
                            overscrollHandler.handleScrollDelta(
                                delta = delta,
                                currentIndex = pagerState.currentPage,
                                items = currentItems,
                                isNavigating = currentIsNavigating,
                            )
                        }
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    overscrollHandler.reset()
                    return Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    overscrollHandler.reset()
                    return Velocity.Zero
                }
            }
        }

    return this.nestedScroll(nestedScrollConnection)
}

internal class PagerOverscrollHandler(
    val isRtl: Boolean,
    val thresholdPx: Float,
    val onNavigateToChapter: (Chapter, ChapterNavTarget) -> Unit,
) {
    var accumulatedOverscroll = 0f

    fun reset() {
        accumulatedOverscroll = 0f
    }

    fun handleScrollDelta(
        delta: Float,
        currentIndex: Int,
        items: List<ReaderUiItem>,
        isNavigating: Boolean,
    ) {
        if (
            (accumulatedOverscroll > 0f && delta < 0f) || (accumulatedOverscroll < 0f && delta > 0f)
        ) {
            accumulatedOverscroll = 0f
        }
        val currentItem = items.getOrNull(currentIndex)

        val chapterToNavigate: Chapter?
        val navTarget: ChapterNavTarget?
        val isPrevTransition: Boolean

        if (currentItem is ReaderUiItem.Transition) {
            val transition = currentItem.transition
            chapterToNavigate = transition.to?.chapter
            isPrevTransition = transition is ChapterTransition.Prev
            navTarget = if (isPrevTransition) ChapterNavTarget.End else ChapterNavTarget.Start
        } else if (currentItem is ReaderUiItem.Page || currentItem is ReaderUiItem.SplitPage) {
            val isAtStartOfContent = currentIndex == 0
            val isAtEndOfContent = currentIndex == items.lastIndex

            val wantsPrevious =
                if (isRtl) {
                    isAtEndOfContent && delta < 0
                } else {
                    isAtStartOfContent && delta > 0
                }
            val wantsNext =
                if (isRtl) {
                    isAtStartOfContent && delta > 0
                } else {
                    isAtEndOfContent && delta < 0
                }

            if (wantsPrevious) {
                val prevTransition =
                    items.firstOrNull {
                        it is ReaderUiItem.Transition && it.transition is ChapterTransition.Prev
                    } as? ReaderUiItem.Transition
                chapterToNavigate = prevTransition?.transition?.to?.chapter
                isPrevTransition = true
                navTarget = ChapterNavTarget.End
            } else if (wantsNext) {
                val nextTransition =
                    items.lastOrNull {
                        it is ReaderUiItem.Transition && it.transition is ChapterTransition.Next
                    } as? ReaderUiItem.Transition
                chapterToNavigate = nextTransition?.transition?.to?.chapter
                isPrevTransition = false
                navTarget = ChapterNavTarget.Start
            } else {
                chapterToNavigate = null
                navTarget = null
                isPrevTransition = false
            }
        } else {
            chapterToNavigate = null
            navTarget = null
            isPrevTransition = false
        }

        if (chapterToNavigate != null && navTarget != null) {
            accumulatedOverscroll += delta
            val isTrigger =
                if (isRtl) {
                    if (isPrevTransition) {
                        accumulatedOverscroll < -thresholdPx
                    } else {
                        accumulatedOverscroll > thresholdPx
                    }
                } else {
                    if (isPrevTransition) {
                        accumulatedOverscroll > thresholdPx
                    } else {
                        accumulatedOverscroll < -thresholdPx
                    }
                }
            if (isTrigger && !isNavigating) {
                accumulatedOverscroll = 0f
                onNavigateToChapter(chapterToNavigate, navTarget)
            }
        } else {
            accumulatedOverscroll = 0f
        }
    }
}
