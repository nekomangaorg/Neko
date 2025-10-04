package org.nekomanga.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SCROLL_TIMEOUT_IN_MILLIS = 3000L

@Composable
fun FastScroller(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.surfaceTint,
    thumbSelectedColor: Color = MaterialTheme.colorScheme.primary,
    thumbWidth: Dp = 8.dp,
    thumbMinWidth: Dp = 48.dp,
) {
    val coroutineScope = rememberCoroutineScope()
    var isSelected by remember { mutableStateOf(false) }

    val isScrolling by remember { derivedStateOf { lazyListState.isScrollInProgress } }

    var isVisible by remember { mutableStateOf(false) }

    val animatedThumbWidth by animateDpAsState(
        targetValue = if (isSelected) thumbWidth + 4.dp else thumbWidth,
        label = "thumb_width_animation",
    )

    LaunchedEffect(isScrolling, isSelected) {
        if (isScrolling || isSelected) {
            isVisible = true
        } else {
            delay(SCROLL_TIMEOUT_IN_MILLIS)
            isVisible = false
        }
    }

    val alpha by
    animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec =
        tween(
            durationMillis = if (isVisible) 75 else 500,
            delayMillis = if (isVisible) 0 else 500,
        ),
        label = "alpha",
    )

    val realThumbColor by
    remember(isSelected) { mutableStateOf(if (isSelected) thumbSelectedColor else thumbColor) }

    val firstVisibleItemIndex by
    remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by
    remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }
    val itemsCount by
    remember { derivedStateOf { lazyListState.layoutInfo.totalItemsCount } }
    val visibleItemsCount by
    remember { derivedStateOf { lazyListState.layoutInfo.visibleItemsInfo.size } }
    val averageItemSize by
    remember {
        derivedStateOf {
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                0
            } else {
                visibleItems.sumOf { it.size } / visibleItems.size
            }
        }
    }

    Box(modifier = modifier.fillMaxHeight().width(thumbWidth + (thumbWidth / 2)).alpha(alpha)) {
        Box(
            modifier =
            Modifier.align(Alignment.CenterEnd)
                .graphicsLayer {
                    if (itemsCount > visibleItemsCount) {
                        val thumbHeight =
                            (lazyListState.layoutInfo.viewportSize.height.toFloat() / itemsCount) *
                                visibleItemsCount

                        val scrollOffset =
                            if (averageItemSize != 0) {
                                (firstVisibleItemScrollOffset.toFloat() / averageItemSize)
                            } else {
                                0f
                            }

                        this.translationY =
                            (firstVisibleItemIndex.toFloat() / (itemsCount - visibleItemsCount)) *
                                (lazyListState.layoutInfo.viewportSize.height - thumbHeight) +
                                scrollOffset
                        this.scaleY =
                            (thumbHeight / lazyListState.layoutInfo.viewportSize.height).coerceIn(
                                thumbMinWidth.value / lazyListState.layoutInfo.viewportSize.height,
                                1f,
                            )
                    }
                }
                .width(animatedThumbWidth)
                .fillMaxHeight()
                .background(color = realThumbColor, shape = MaterialTheme.shapes.large)
                .draggable(
                    state =
                    rememberDraggableState { delta ->
                        if (isSelected) {
                            coroutineScope.launch {
                                val itemsLeft = itemsCount - visibleItemsCount
                                if (itemsLeft > 0) {
                                    val scrollBy =
                                        (delta / lazyListState.layoutInfo.viewportSize.height) *
                                            itemsLeft
                                    lazyListState.scrollToItem(
                                        (scrollBy + firstVisibleItemIndex)
                                            .toInt()
                                            .coerceIn(0, itemsLeft),
                                    )
                                }
                            }
                        }
                    },
                    orientation = Orientation.Vertical,
                    startDragImmediately = true,
                    onDragStarted = { isSelected = true },
                    onDragStopped = { isSelected = false },
                ),
        )
    }
}

@Composable
fun FastScroller(
    lazyGridState: LazyGridState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.surfaceTint,
    thumbSelectedColor: Color = MaterialTheme.colorScheme.primary,
    thumbWidth: Dp = 8.dp,
    thumbMinWidth: Dp = 48.dp,
) {
    val coroutineScope = rememberCoroutineScope()

    var isSelected by remember { mutableStateOf(false) }

    val isScrolling by remember { derivedStateOf { lazyGridState.isScrollInProgress } }

    val animatedThumbWidth by animateDpAsState(
        targetValue = if (isSelected) thumbWidth + 4.dp else thumbWidth,
        label = "thumb_width_animation",
    )

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolling, isSelected) {
        if (isScrolling || isSelected) {
            isVisible = true
        } else {
            delay(SCROLL_TIMEOUT_IN_MILLIS)
            isVisible = false
        }
    }

    val alpha by
    animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec =
        tween(
            durationMillis = if (isVisible) 75 else 500,
            delayMillis = if (isVisible) 0 else 500,
        ),
        label = "alpha",
    )

    val realThumbColor by
    remember(isSelected) { mutableStateOf(if (isSelected) thumbSelectedColor else thumbColor) }

    val firstVisibleItemIndex by
    remember { derivedStateOf { lazyGridState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by
    remember { derivedStateOf { lazyGridState.firstVisibleItemScrollOffset } }

    val itemsCount by
    remember { derivedStateOf { lazyGridState.layoutInfo.totalItemsCount } }

    val visibleItemsCount by
    remember {
        derivedStateOf { lazyGridState.layoutInfo.visibleItemsInfo.size }
    }

    val averageItemSize by
    remember {
        derivedStateOf {
            val visibleItems = lazyGridState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                0
            } else {
                visibleItems.sumOf { it.size.height } / visibleItems.size
            }
        }
    }

    Box(modifier = modifier.fillMaxHeight().width(thumbWidth + (thumbWidth / 2)).alpha(alpha)) {
        Box(
            modifier =
            Modifier.align(Alignment.CenterEnd)
                .graphicsLayer {
                    if (itemsCount > visibleItemsCount) {
                        val thumbHeight =
                            (lazyGridState.layoutInfo.viewportSize.height.toFloat() /
                                itemsCount) * visibleItemsCount

                        val scrollOffset =
                            if (averageItemSize != 0) {
                                (firstVisibleItemScrollOffset.toFloat() / averageItemSize)
                            } else {
                                0f
                            }

                        this.translationY =
                            (firstVisibleItemIndex.toFloat() /
                                (itemsCount - visibleItemsCount)) *
                                (lazyGridState.layoutInfo.viewportSize.height - thumbHeight) +
                                scrollOffset
                        this.scaleY =
                            (thumbHeight / lazyGridState.layoutInfo.viewportSize.height)
                                .coerceIn(
                                    thumbMinWidth.value /
                                        lazyGridState.layoutInfo.viewportSize.height,
                                    1f,
                                )
                    }
                }
                .width(animatedThumbWidth)
                .fillMaxHeight()
                .background(color = realThumbColor, shape = MaterialTheme.shapes.large)
                .draggable(
                    state =
                    rememberDraggableState { delta ->
                        if (isSelected) {
                            coroutineScope.launch {
                                val itemsLeft = itemsCount - visibleItemsCount
                                if (itemsLeft > 0) {
                                    val scrollBy =
                                        (delta /
                                            lazyGridState.layoutInfo.viewportSize.height) *
                                            itemsLeft
                                    lazyGridState.scrollToItem(
                                        (scrollBy + firstVisibleItemIndex)
                                            .toInt()
                                            .coerceIn(0, itemsLeft),
                                    )
                                }
                            }
                        }
                    },
                    orientation = Orientation.Vertical,
                    startDragImmediately = true,
                    onDragStarted = { isSelected = true },
                    onDragStopped = { isSelected = false },
                ),
        )
    }
}