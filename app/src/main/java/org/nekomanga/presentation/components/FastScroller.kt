package org.nekomanga.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun FastScroller(
    state: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color,
    thumbInactiveColor: Color,
    thumbWidth: Dp = 8.dp,
    thumbMinHeight: Dp = 48.dp,
) {
    val scope = rememberCoroutineScope()
    var isSelected by remember { mutableStateOf(false) }
    var containerHeight by remember { mutableStateOf(0f) }

    val isVisible by remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.isNotEmpty() &&
                state.layoutInfo.totalItemsCount > state.layoutInfo.visibleItemsInfo.size
        }
    }

    val thumbMinHeightPx = with(LocalDensity.current) { thumbMinHeight.toPx() }

    val thumbHeight by remember {
        derivedStateOf {
            if (!isVisible || containerHeight == 0f) {
                0f
            } else {
                val visibleItemsFraction =
                    state.layoutInfo.visibleItemsInfo.size.toFloat() /
                        state.layoutInfo.totalItemsCount
                (containerHeight * visibleItemsFraction).coerceAtLeast(thumbMinHeightPx)
            }
        }
    }

    val thumbOffset by remember {
        derivedStateOf {
            if (!isVisible || thumbHeight == 0f || containerHeight == 0f) {
                0f
            } else {
                val totalItemsCount = state.layoutInfo.totalItemsCount
                if (totalItemsCount == 0) {
                    0f
                } else {
                    val firstVisibleItem = state.layoutInfo.visibleItemsInfo.firstOrNull()
                    val averageItemHeight =
                        if (firstVisibleItem != null) {
                            val totalVisibleItemsHeight =
                                state.layoutInfo.visibleItemsInfo.sumOf { it.size }
                            totalVisibleItemsHeight.toFloat() / state.layoutInfo.visibleItemsInfo.size
                        } else {
                            0f
                        }

                    if (averageItemHeight == 0f) {
                        0f
                    } else {
                        val scrolledPastItemsHeight = state.firstVisibleItemIndex * averageItemHeight
                        val scrolledPastAndVisibleOffset =
                            scrolledPastItemsHeight + state.firstVisibleItemScrollOffset
                        val totalContentHeight = totalItemsCount * averageItemHeight
                        val scrollableDistance = totalContentHeight - containerHeight
                        if (scrollableDistance <= 0) {
                            0f
                        } else {
                            val scrollRatio = scrolledPastAndVisibleOffset / scrollableDistance
                            val thumbScrollableDistance = containerHeight - thumbHeight
                            (thumbScrollableDistance * scrollRatio)
                        }
                    }
                }
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible && (isSelected || state.isScrollInProgress)) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isSelected) 0 else 500,
            delayMillis = if (isSelected) 0 else 500,
        ),
    )

    fun scrollTo(offsetY: Float) {
        scope.launch {
            val totalItems = state.layoutInfo.totalItemsCount
            if (totalItems == 0) return@launch

            val thumbScrollRatio = (offsetY - thumbHeight / 2) / (containerHeight - thumbHeight)
            val targetItemIndex = (thumbScrollRatio * totalItems).toInt()

            state.scrollToItem(targetItemIndex.coerceIn(0, totalItems - 1))
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(thumbWidth + 16.dp)
            .alpha(alpha)
            .onGloballyPositioned { containerHeight = it.size.height.toFloat() }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isSelected = true
                        scrollTo(it.y)
                    },
                    onDragEnd = { isSelected = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        scrollTo(change.position.y)
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, thumbOffset.roundToInt()) }
                .height(thumbHeight.dp)
                .width(thumbWidth)
                .background(
                    color = if (isSelected) thumbColor else thumbInactiveColor,
                    shape = CircleShape,
                ),
        )
    }
}