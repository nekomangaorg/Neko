package org.nekomanga.presentation.components

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastLastOrNull
import androidx.compose.ui.util.fastMaxBy
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample

@Composable
fun VerticalFastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbAllowed: () -> Boolean = { true },
    thumbColor: Color = MaterialTheme.colorScheme.secondary,
    topContentPadding: Dp = Dp.Hairline,
    bottomContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceable = subcompose("content", content).map { it.measure(constraints) }
        val contentHeight = contentPlaceable.fastMaxBy { it.height }?.height ?: 0
        val contentWidth = contentPlaceable.fastMaxBy { it.width }?.width ?: 0

        val scrollerConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val scrollerPlaceable =
            subcompose("scroller") {
                    val layoutInfo = listState.layoutInfo
                    if (layoutInfo.visibleItemsInfo.isEmpty() || layoutInfo.totalItemsCount == 0)
                        return@subcompose

                    val thumbTopPadding = with(LocalDensity.current) { topContentPadding.toPx() }
                    var thumbOffsetY by
                        remember(thumbTopPadding) { mutableFloatStateOf(thumbTopPadding) }

                    val dragInteractionSource = remember { MutableInteractionSource() }
                    val isThumbDragged by dragInteractionSource.collectIsDraggedAsState()
                    val scrolled = remember {
                        MutableSharedFlow<Unit>(
                            extraBufferCapacity = 1,
                            onBufferOverflow = BufferOverflow.DROP_OLDEST,
                        )
                    }

                    val scrollStateTracker = remember { MutableData(listState.isScrollInProgress) }
                    val stableScrollInProgress =
                        scrollStateTracker.value || listState.isScrollInProgress
                    scrollStateTracker.value = listState.isScrollInProgress
                    val anyScrollInProgress = stableScrollInProgress || isThumbDragged

                    val thumbBottomPadding =
                        with(LocalDensity.current) { bottomContentPadding.toPx() }
                    val heightPx =
                        contentHeight.toFloat() -
                            thumbTopPadding -
                            thumbBottomPadding -
                            listState.layoutInfo.afterContentPadding
                    val thumbHeightPx = with(LocalDensity.current) { ThumbLength.toPx() }
                    val trackHeightPx = heightPx - thumbHeightPx
                    val scrollHeightPx =
                        contentHeight.toFloat() -
                            listState.layoutInfo.beforeContentPadding -
                            listState.layoutInfo.afterContentPadding -
                            thumbBottomPadding

                    val visibleItems = layoutInfo.visibleItemsInfo
                    val topItem =
                        visibleItems.fastFirstOrNull { it.bottom >= 0 } ?: visibleItems.first()
                    val bottomItem =
                        visibleItems.fastLastOrNull { it.top <= scrollHeightPx }
                            ?: visibleItems.last()

                    val topHiddenProportion = -1f * topItem.top / topItem.size.coerceAtLeast(1)
                    val bottomHiddenProportion =
                        (bottomItem.bottom - scrollHeightPx) / bottomItem.size.coerceAtLeast(1)
                    val previousSections = topHiddenProportion + topItem.index
                    val remainingSections =
                        bottomHiddenProportion +
                            (layoutInfo.totalItemsCount - (bottomItem.index + 1))
                    val scrollableSections = previousSections + remainingSections

                    val layoutChangeTracker = remember { MutableData(scrollableSections) }
                    val layoutChanged =
                        !anyScrollInProgress &&
                            abs(layoutChangeTracker.value - scrollableSections) > 0.1
                    layoutChangeTracker.value = scrollableSections

                    val estimateConfidence = remember { MutableData(remainingSections) }
                    if (layoutChanged) estimateConfidence.value = remainingSections
                    val maxRemainingSections =
                        remember(estimateConfidence.value) { scrollableSections }
                    estimateConfidence.value = max(estimateConfidence.value, remainingSections)

                    if (maxRemainingSections < 0.5) return@subcompose

                    // Defer Compose state read using snapshotFlow instead of triggering
                    // LaunchedEffect on every scroll tick
                    val isThumbDraggedState by rememberUpdatedState(isThumbDragged)
                    val maxRemainingSectionsState by rememberUpdatedState(maxRemainingSections)
                    val trackHeightPxState by rememberUpdatedState(trackHeightPx)
                    val thumbTopPaddingState by rememberUpdatedState(thumbTopPadding)
                    val stableScrollInProgressState by rememberUpdatedState(stableScrollInProgress)
                    val scrollHeightPxState by rememberUpdatedState(scrollHeightPx)

                    LaunchedEffect(listState) {
                        snapshotFlow { listState.firstVisibleItemScrollOffset }
                            .collectLatest {
                                if (
                                    listState.layoutInfo.totalItemsCount != 0 &&
                                        !isThumbDraggedState
                                ) {
                                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                                    val topItem =
                                        visibleItems.fastFirstOrNull { it.bottom >= 0 }
                                            ?: visibleItems.firstOrNull()
                                            ?: return@collectLatest
                                    val bottomItemInner =
                                        visibleItems.fastLastOrNull {
                                            it.top <= scrollHeightPxState
                                        } ?: visibleItems.lastOrNull() ?: return@collectLatest
                                    val topHiddenProportion =
                                        -1f * topItem.top / topItem.size.coerceAtLeast(1)
                                    val bottomHiddenProportion =
                                        (bottomItemInner.bottom - scrollHeightPxState) /
                                            bottomItemInner.size.coerceAtLeast(1)
                                    val remainingSectionsLocal =
                                        bottomHiddenProportion +
                                            (listState.layoutInfo.totalItemsCount -
                                                (bottomItemInner.index + 1))
                                    val proportion =
                                        1f - remainingSectionsLocal / maxRemainingSectionsState
                                    thumbOffsetY =
                                        trackHeightPxState * proportion + thumbTopPaddingState
                                    if (stableScrollInProgressState) scrolled.tryEmit(Unit)
                                }
                            }
                    }

                    LaunchedEffect(thumbOffsetY) {
                        if (listState.layoutInfo.totalItemsCount == 0 || !isThumbDragged)
                            return@LaunchedEffect
                        val thumbProportion = (thumbOffsetY - thumbTopPadding) / trackHeightPx
                        if (thumbProportion <= 0.001f) {
                            estimateConfidence.value = -1f
                            listState.scrollToItem(index = 0, scrollOffset = 0)
                            scrolled.tryEmit(Unit)
                            return@LaunchedEffect
                        }
                        val scrollRemainingSections = (1f - thumbProportion) * maxRemainingSections
                        val currentSection =
                            listState.layoutInfo.totalItemsCount - scrollRemainingSections
                        val scrollSectionIndex =
                            currentSection
                                .toInt()
                                .coerceAtMost(listState.layoutInfo.totalItemsCount)
                        val expectedScrollItem =
                            listState.layoutInfo.visibleItemsInfo.find {
                                it.index == scrollSectionIndex
                            }
                                ?: listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                ?: return@LaunchedEffect
                        val scrollRelativeOffset =
                            expectedScrollItem.size * (currentSection - scrollSectionIndex)
                        val scrollSectionOffset =
                            (scrollRelativeOffset - scrollHeightPx).roundToInt()
                        val scrollItemIndex =
                            scrollSectionIndex.coerceIn(0, listState.layoutInfo.totalItemsCount - 1)
                        val scrollItemOffset =
                            scrollSectionOffset +
                                (scrollSectionIndex - scrollItemIndex) *
                                    (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.size ?: 0)
                        listState.scrollToItem(
                            index = scrollItemIndex,
                            scrollOffset = scrollItemOffset,
                        )
                        scrolled.tryEmit(Unit)
                    }

                    val alpha = remember { Animatable(0f) }
                    val isThumbVisible = alpha.value > 0f
                    LaunchedEffect(scrolled, alpha) {
                        scrolled.sample(100).collectLatest {
                            if (thumbAllowed()) {
                                alpha.snapTo(1f)
                                alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
                            } else {
                                alpha.animateTo(0f, animationSpec = ImmediateFadeOutAnimationSpec)
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier.offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                                .then(
                                    if (isThumbVisible && !listState.isScrollInProgress) {
                                        Modifier.draggable(
                                            interactionSource = dragInteractionSource,
                                            orientation = Orientation.Vertical,
                                            state =
                                                rememberDraggableState { delta ->
                                                    val newOffsetY = thumbOffsetY + delta
                                                    thumbOffsetY =
                                                        newOffsetY.coerceIn(
                                                            thumbTopPadding,
                                                            thumbTopPadding + trackHeightPx,
                                                        )
                                                },
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (
                                        isThumbVisible &&
                                            !isThumbDragged &&
                                            !listState.isScrollInProgress
                                    ) {
                                        Modifier.systemGestureExclusion()
                                    } else {
                                        Modifier
                                    }
                                )
                                .height(ThumbLength)
                                .padding(horizontal = 8.dp)
                                .padding(end = endContentPadding)
                                .width(ThumbThickness)
                                .alpha(alpha.value)
                                .background(color = thumbColor, shape = MaterialTheme.shapes.medium)
                    )
                }
                .map { it.measure(scrollerConstraints) }
        val scrollerWidth = scrollerPlaceable.fastMaxBy { it.width }?.width ?: 0

        layout(contentWidth, contentHeight) {
            contentPlaceable.fastForEach { it.place(0, 0) }
            scrollerPlaceable.fastForEach { it.placeRelative(contentWidth - scrollerWidth, 0) }
        }
    }
}

@Composable
private fun rememberColumnWidthSums(
    columns: GridCells,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
) =
    remember<Density.(Constraints) -> List<Int>>(columns, horizontalArrangement, contentPadding) {
        { constraints ->
            require(constraints.maxWidth != Constraints.Infinity) {
                "LazyVerticalGrid's width should be bound by parent"
            }
            val horizontalPadding =
                contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    contentPadding.calculateEndPadding(LayoutDirection.Ltr)
            val gridWidth = constraints.maxWidth - horizontalPadding.roundToPx()
            with(columns) {
                calculateCrossAxisCellSizes(gridWidth, horizontalArrangement.spacing.roundToPx())
                    .toMutableList()
                    .apply {
                        for (i in 1..<size) {
                            this[i] += this[i - 1]
                        }
                    }
            }
        }
    }

@Composable
fun VerticalGridFastScroller(
    state: LazyGridState,
    columns: GridCells,
    arrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    thumbAllowed: () -> Boolean = { true },
    thumbColor: Color = MaterialTheme.colorScheme.secondary,
    topContentPadding: Dp = Dp.Hairline,
    bottomContentPadding: Dp = Dp.Hairline,
    endContentPadding: Dp = Dp.Hairline,
    content: @Composable () -> Unit,
) {
    val slotSizesSums =
        rememberColumnWidthSums(
            columns = columns,
            horizontalArrangement = arrangement,
            contentPadding = contentPadding,
        )

    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceable = subcompose("content", content).map { it.measure(constraints) }
        val contentHeight = contentPlaceable.fastMaxBy { it.height }?.height ?: 0
        val contentWidth = contentPlaceable.fastMaxBy { it.width }?.width ?: 0

        val scrollerConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val scrollerPlaceable =
            subcompose("scroller") {
                    val layoutInfo = state.layoutInfo
                    val showScroller =
                        remember(columns, layoutInfo.totalItemsCount) {
                            layoutInfo.visibleItemsInfo.size < layoutInfo.totalItemsCount
                        }
                    if (!showScroller) return@subcompose
                    val thumbTopPadding = with(LocalDensity.current) { topContentPadding.toPx() }
                    var thumbOffsetY by
                        remember(thumbTopPadding) { mutableFloatStateOf(thumbTopPadding) }

                    val dragInteractionSource = remember { MutableInteractionSource() }
                    val isThumbDragged by dragInteractionSource.collectIsDraggedAsState()
                    val scrolled = remember {
                        MutableSharedFlow<Unit>(
                            extraBufferCapacity = 1,
                            onBufferOverflow = BufferOverflow.DROP_OLDEST,
                        )
                    }

                    val thumbBottomPadding =
                        with(LocalDensity.current) { bottomContentPadding.toPx() }
                    val heightPx =
                        contentHeight.toFloat() -
                            thumbTopPadding -
                            thumbBottomPadding -
                            state.layoutInfo.afterContentPadding
                    val thumbHeightPx = with(LocalDensity.current) { ThumbLength.toPx() }
                    val trackHeightPx = heightPx - thumbHeightPx

                    val columnCount =
                        remember(columns) { slotSizesSums(constraints).size.coerceAtLeast(1) }
                    val scrollRange =
                        remember(columns) {
                            computeGridScrollRange(state = state, columnCount = columnCount)
                        }

                    LaunchedEffect(thumbOffsetY) {
                        if (layoutInfo.totalItemsCount == 0 || !isThumbDragged)
                            return@LaunchedEffect
                        val visibleItems = state.layoutInfo.visibleItemsInfo
                        val startChild = visibleItems.first()
                        val endChild = visibleItems.last()
                        val laidOutArea =
                            (endChild.offset.y + endChild.size.height) - startChild.offset.y
                        val laidOutRows = 1 + abs(endChild.index - startChild.index) / columnCount
                        val avgSizePerRow = laidOutArea.toFloat() / laidOutRows

                        val scrollRatio = (thumbOffsetY - thumbTopPadding) / trackHeightPx
                        val scrollAmt =
                            scrollRatio * (scrollRange.toFloat() - heightPx).coerceAtLeast(1f)
                        val rowNumber = (scrollAmt / avgSizePerRow).toInt()
                        val rowOffset = scrollAmt - rowNumber * avgSizePerRow

                        state.scrollToItem(
                            index = columnCount * rowNumber,
                            scrollOffset = rowOffset.roundToInt(),
                        )
                        scrolled.tryEmit(Unit)
                    }

                    // Defer Compose state read using snapshotFlow instead of triggering
                    // LaunchedEffect on every scroll tick
                    val heightPxState by rememberUpdatedState(heightPx)
                    val trackHeightPxGridState by rememberUpdatedState(trackHeightPx)
                    val thumbTopPaddingGridState by rememberUpdatedState(thumbTopPadding)
                    val scrollRangeState by rememberUpdatedState(scrollRange)
                    val columnCountState by rememberUpdatedState(columnCount)
                    val isThumbDraggedGridState by rememberUpdatedState(isThumbDragged)

                    LaunchedEffect(state) {
                        snapshotFlow { state.firstVisibleItemScrollOffset }
                            .collectLatest {
                                if (
                                    state.layoutInfo.totalItemsCount == 0 || isThumbDraggedGridState
                                )
                                    return@collectLatest
                                val scrollOffset =
                                    computeGridScrollOffset(
                                        state = state,
                                        columnCount = columnCountState,
                                    )
                                val extraScrollRange =
                                    (scrollRangeState.toFloat() - heightPxState).coerceAtLeast(1f)
                                val proportion =
                                    (scrollOffset.toFloat() / extraScrollRange).coerceAtMost(1f)
                                thumbOffsetY =
                                    trackHeightPxGridState * proportion + thumbTopPaddingGridState
                                scrolled.tryEmit(Unit)
                            }
                    }

                    val alpha = remember { Animatable(0f) }
                    val isThumbVisible = alpha.value > 0f
                    LaunchedEffect(scrolled, alpha) {
                        scrolled.sample(100).collectLatest {
                            if (thumbAllowed()) {
                                alpha.snapTo(1f)
                                alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
                            } else {
                                alpha.animateTo(0f, animationSpec = ImmediateFadeOutAnimationSpec)
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier.offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                                .then(
                                    if (isThumbVisible && !state.isScrollInProgress) {
                                        Modifier.draggable(
                                            interactionSource = dragInteractionSource,
                                            orientation = Orientation.Vertical,
                                            state =
                                                rememberDraggableState { delta ->
                                                    val newOffsetY = thumbOffsetY + delta
                                                    thumbOffsetY =
                                                        newOffsetY.coerceIn(
                                                            thumbTopPadding,
                                                            thumbTopPadding + trackHeightPx,
                                                        )
                                                },
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (
                                        isThumbVisible &&
                                            !isThumbDragged &&
                                            !state.isScrollInProgress
                                    ) {
                                        Modifier.systemGestureExclusion()
                                    } else {
                                        Modifier
                                    }
                                )
                                .height(ThumbLength)
                                .padding(end = endContentPadding)
                                .width(ThumbThickness)
                                .alpha(alpha.value)
                                .background(color = thumbColor, shape = MaterialTheme.shapes.medium)
                    )
                }
                .map { it.measure(scrollerConstraints) }
        val scrollerWidth = scrollerPlaceable.fastMaxBy { it.width }?.width ?: 0

        layout(contentWidth, contentHeight) {
            contentPlaceable.fastForEach { it.place(0, 0) }
            scrollerPlaceable.fastForEach { it.placeRelative(contentWidth - scrollerWidth, 0) }
        }
    }
}

private fun computeGridScrollOffset(state: LazyGridState, columnCount: Int): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val laidOutArea = (endChild.offset.y + endChild.size.height) - startChild.offset.y
    val laidOutRows = 1 + abs(endChild.index - startChild.index) / columnCount
    val avgSizePerRow = laidOutArea.toFloat() / laidOutRows

    val rowsBefore = min(startChild.index, endChild.index).coerceAtLeast(0) / columnCount
    return (rowsBefore * avgSizePerRow - startChild.offset.y).roundToInt()
}

private fun computeGridScrollRange(state: LazyGridState, columnCount: Int): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val laidOutArea = (endChild.offset.y + endChild.size.height) - startChild.offset.y
    val laidOutRows = 1 + abs(endChild.index - startChild.index) / columnCount
    val avgSizePerRow = laidOutArea.toFloat() / laidOutRows

    val totalRows = 1 + (state.layoutInfo.totalItemsCount - 1) / columnCount
    val endSpacing = avgSizePerRow - endChild.size.height
    return (endSpacing + (laidOutArea.toFloat() / laidOutRows) * totalRows).roundToInt()
}

private class MutableData<T>(var value: T)

private val ThumbLength = 48.dp
private val ThumbThickness = 12.dp
private val FadeOutAnimationSpec =
    tween<Float>(durationMillis = ViewConfiguration.getScrollBarFadeDuration(), delayMillis = 2000)
private val ImmediateFadeOutAnimationSpec =
    tween<Float>(durationMillis = ViewConfiguration.getScrollBarFadeDuration())

private val LazyListItemInfo.top: Int
    get() = offset

private val LazyListItemInfo.bottom: Int
    get() = offset + size
