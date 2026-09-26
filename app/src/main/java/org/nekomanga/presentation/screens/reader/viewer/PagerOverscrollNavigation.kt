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

    val nestedScrollConnection =
        remember(pagerState, isVertical, isRtl, thresholdPx) {
            object : NestedScrollConnection {
                var accumulatedOverscroll = 0f

                private fun checkAndTrigger(delta: Float) {
                    if (
                        (accumulatedOverscroll > 0f && delta < 0f) ||
                            (accumulatedOverscroll < 0f && delta > 0f)
                    ) {
                        accumulatedOverscroll = 0f
                    }
                    val currentIndex = pagerState.currentPage
                    val currentItem = currentItems.getOrNull(currentIndex)

                    val chapterToNavigate: Chapter?
                    val navTarget: ChapterNavTarget?
                    val isPrevTransition: Boolean

                    if (currentItem is ReaderUiItem.Transition) {
                        val transition = currentItem.transition
                        chapterToNavigate = transition.to?.chapter
                        isPrevTransition = transition is ChapterTransition.Prev
                        navTarget =
                            if (isPrevTransition) ChapterNavTarget.End else ChapterNavTarget.Start
                    } else if (
                        currentItem is ReaderUiItem.Page || currentItem is ReaderUiItem.SplitPage
                    ) {
                        val isAtStartOfContent = currentIndex == 0
                        val isAtEndOfContent = currentIndex == currentItems.lastIndex

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
                                currentItems.firstOrNull {
                                    it is ReaderUiItem.Transition &&
                                        it.transition is ChapterTransition.Prev
                                } as? ReaderUiItem.Transition
                            chapterToNavigate = prevTransition?.transition?.to?.chapter
                            isPrevTransition = true
                            navTarget = ChapterNavTarget.End
                        } else if (wantsNext) {
                            val nextTransition =
                                currentItems.lastOrNull {
                                    it is ReaderUiItem.Transition &&
                                        it.transition is ChapterTransition.Next
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
                        if (isTrigger && !currentIsNavigating) {
                            accumulatedOverscroll = 0f
                            currentOnNavigateToChapter(
                                chapterToNavigate,
                                navTarget,
                            )
                        }
                    } else {
                        accumulatedOverscroll = 0f
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
                        } else {
                            accumulatedOverscroll = 0f
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

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    accumulatedOverscroll = 0f
                    return Velocity.Zero
                }
            }
        }

    return this.nestedScroll(nestedScrollConnection)
}
