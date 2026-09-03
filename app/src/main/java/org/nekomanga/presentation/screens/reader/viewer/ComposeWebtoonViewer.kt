package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ComposeWebtoonViewer(
    viewer: WebtoonViewer,
    items: List<ReaderUiItem>,
    manga: MangaItem?,
    downloadManager: DownloadManager,
    onPageSelected: (ReaderPage) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onRetryTransition: (ReaderChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentChapterId =
        (viewer.currentChapter
                ?: items
                    .firstOrNull { it is ReaderUiItem.Page }
                    ?.let { (it as ReaderUiItem.Page).page.chapter })
            ?.chapter
            ?.id

    key(viewer) {
        val defaultPageIndex =
            items
                .indexOfFirst {
                    when (it) {
                        is ReaderUiItem.Page -> it.page.chapter.chapter.id == currentChapterId
                        is ReaderUiItem.SplitPage -> it.page.chapter.chapter.id == currentChapterId
                        is ReaderUiItem.Transition -> false
                    }
                }
                .takeIf { it != -1 }
                ?: items
                    .indexOfFirst { it is ReaderUiItem.Page || it is ReaderUiItem.SplitPage }
                    .takeIf { it != -1 }
                ?: 0
        val initialItemIndex =
            (viewer.requestedPagePosition?.targetPage ?: defaultPageIndex).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()

        val readerPreferences: ReaderPreferences = remember { Injekt.get() }
        val readerTheme by readerPreferences.readerTheme().collectAsState()
        val webtoonSidePadding by readerPreferences.webtoonSidePadding().collectAsState()
        val animatedTransitions by
            readerPreferences.animatedPageTransitionsWebtoon().collectAsState()
        val disableGaps by readerPreferences.webtoonDisableGaps().collectAsState()
        val enableZoomOut by readerPreferences.webtoonEnableZoomOut().collectAsState()
        val themeBackground = MaterialTheme.colorScheme.background
        val backgroundColor =
            remember(readerTheme, themeBackground) {
                ReaderTheme.fromPreference(readerTheme).color(themeBackground)
            }

        val currentItems by rememberUpdatedState(items)

        LaunchedEffect(currentChapterId) {
            viewer.prevTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
            viewer.nextTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
        }

        // Sync programmatic page jumps (slider, TOC, etc.)
        LaunchedEffect(viewer.requestedPagePosition) {
            val req = viewer.requestedPagePosition ?: return@LaunchedEffect
            val target = req.targetPage
            if (target in currentItems.indices) {
                if (
                    lazyListState.firstVisibleItemIndex != target ||
                        lazyListState.firstVisibleItemScrollOffset != 0
                ) {
                    val useAnimation = req.animated && animatedTransitions
                    if (useAnimation) {
                        lazyListState.animateScrollToItem(target)
                    } else {
                        lazyListState.scrollToItem(target)
                    }
                }
                viewer.requestedPagePosition = null
            }
        }

        // Sync delta scroll
        LaunchedEffect(viewer.requestedScrollDelta) {
            viewer.requestedScrollDelta?.let { delta ->
                val scrollAmount = if (scale > 0f) delta.toFloat() / scale else delta.toFloat()
                if (animatedTransitions) {
                    lazyListState.animateScrollBy(scrollAmount)
                } else {
                    lazyListState.scrollBy(scrollAmount)
                }
                viewer.requestedScrollDelta = null
            }
        }

        // Hide menu on manual user scroll drag
        LaunchedEffect(lazyListState.interactionSource) {
            lazyListState.interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Start && viewer.activity.menuVisible) {
                    viewer.activity.hideMenu()
                }
            }
        }

        val context = LocalContext.current
        val viewConfiguration = remember(context) { ViewConfiguration.get(context) }
        val touchSlopPx =
            remember(viewConfiguration) { viewConfiguration.scaledTouchSlop.toDouble() }
        val doubleTapSlopPx =
            remember(viewConfiguration) { viewConfiguration.scaledDoubleTapSlop.toDouble() }
        val doubleTapTimeoutMs = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }
        val longPressTimeoutMs = remember { ViewConfiguration.getLongPressTimeout().toLong() }

        // Preload initial batch of pages when items are loaded or updated
        LaunchedEffect(items) {
            val startIndex =
                (viewer.requestedPagePosition?.targetPage ?: defaultPageIndex).coerceIn(
                    0,
                    (items.size - 1).coerceAtLeast(0),
                )
            val preloadEnd = minOf(items.size - 1, startIndex + 6)
            for (i in startIndex..preloadEnd) {
                val item = items.getOrNull(i) ?: continue
                val page =
                    when (item) {
                        is ReaderUiItem.Page -> item.page
                        is ReaderUiItem.SplitPage -> item.page
                        is ReaderUiItem.Transition -> null
                    }
                page?.let { p -> launch { p.chapter.pageLoader?.loadPage(p) } }
                val data =
                    when (item) {
                        is ReaderUiItem.Page -> item.page
                        is ReaderUiItem.SplitPage -> item.split
                        is ReaderUiItem.Transition -> null
                    }
                if (data != null) {
                    val request = ImageRequest.Builder(context).data(data).build()
                    context.imageLoader.enqueue(request)
                }
            }
        }

        // Track active visible page and preload upcoming/previous pages
        LaunchedEffect(lazyListState) {
            snapshotFlow {
                val layoutInfo = lazyListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val activeIndex =
                    if (visibleItems.isNotEmpty()) {
                        val viewportMiddle =
                            (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                        val activeItemInfo =
                            visibleItems.firstOrNull { item ->
                                val itemTop = item.offset
                                val itemBottom = item.offset + item.size
                                viewportMiddle in itemTop until itemBottom
                            }
                                ?: visibleItems.minByOrNull { item ->
                                    val itemMiddle = item.offset + item.size / 2
                                    abs(itemMiddle - viewportMiddle)
                                }
                                ?: visibleItems.first()
                        activeItemInfo.index
                    } else {
                        lazyListState.firstVisibleItemIndex
                    }
                activeIndex to currentItems.getOrNull(activeIndex)
            }
                .filterNotNull()
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collect { (activeIndex, item) ->
                    if (item != null) {
                        when (item) {
                            is ReaderUiItem.Page -> {
                                onPageSelected(item.page)
                                val pages = item.page.chapter.pages
                                if (pages != null && item.page.chapter == viewer.currentChapter) {
                                    if (pages.size - item.page.number < 5) {
                                        viewer.nextTransition?.to?.let {
                                            viewer.activity.requestPreloadChapter(it)
                                        }
                                    }
                                    if (item.page.number <= 5) {
                                        viewer.prevTransition?.to?.let {
                                            viewer.activity.requestPreloadChapter(it)
                                        }
                                    }
                                }
                            }
                            is ReaderUiItem.SplitPage -> {
                                onPageSelected(item.page)
                            }
                            is ReaderUiItem.Transition -> {
                                onTransitionSelected(item.transition)
                                val toChapter = item.transition.to
                                if (toChapter != null) {
                                    viewer.activity.requestPreloadChapter(toChapter)
                                }
                            }
                        }

                        // Preload window: 2 pages behind, 6 pages ahead
                        val preloadStart = (activeIndex - 2).coerceAtLeast(0)
                        val preloadEnd = (activeIndex + 6).coerceAtMost(currentItems.lastIndex)
                        for (i in preloadStart..preloadEnd) {
                            val preloadItem = currentItems.getOrNull(i) ?: continue
                            val preloadPage =
                                when (preloadItem) {
                                    is ReaderUiItem.Page -> preloadItem.page
                                    is ReaderUiItem.SplitPage -> preloadItem.page
                                    is ReaderUiItem.Transition -> null
                                }
                            preloadPage?.let { p -> launch { p.chapter.pageLoader?.loadPage(p) } }
                            val data =
                                when (preloadItem) {
                                    is ReaderUiItem.Page -> preloadItem.page
                                    is ReaderUiItem.SplitPage -> preloadItem.split
                                    is ReaderUiItem.Transition -> null
                                }
                            if (data != null) {
                                val request = ImageRequest.Builder(context).data(data).build()
                                context.imageLoader.enqueue(request)
                            }
                        }
                    }
                }
        }

        LaunchedEffect(enableZoomOut) {
            if (!enableZoomOut && scale < 1f) {
                val animScale = Animatable(scale)
                val animX = Animatable(offsetX)
                launch { animScale.animateTo(1f, tween(200)) { scale = value } }
                launch { animX.animateTo(0f, tween(200)) { offsetX = value } }
            }
        }

        val sidePaddingPercent = webtoonSidePadding / 100f
        val hasMargins = viewer.noWebtoonTag && !disableGaps

        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize().background(backgroundColor).clipToBounds(),
        ) {
            val horizontalPadding = maxWidth * sidePaddingPercent
            val columnHeight = if (scale < 1f) maxHeight / scale else maxHeight
            LazyColumn(
                state = lazyListState,
                verticalArrangement =
                    Arrangement.spacedBy(if (hasMargins) Size.medium else Size.none),
                userScrollEnabled = true,
                modifier =
                    (if (scale < 1f) {
                            Modifier.fillMaxWidth().requiredHeight(columnHeight)
                        } else {
                            Modifier.fillMaxSize()
                        })
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = 0f
                        }
                        .pointerInput(enableZoomOut) {
                            val velocityTracker = VelocityTracker()
                            awaitEachGesture {
                                val minScale = if (viewer.config.enableZoomOut) 0.5f else 1f
                                val maxScale = 3f

                                val down = awaitFirstDown(requireUnconsumed = false)
                                velocityTracker.resetTracking()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val activePointers = event.changes.filter { it.pressed }

                                    if (activePointers.size >= 2) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()

                                        val newScale =
                                            (scale * zoomChange).coerceIn(minScale, maxScale)
                                        scale = newScale

                                        if (newScale > 1f) {
                                            val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                            offsetX =
                                                (offsetX + panChange.x).coerceIn(
                                                    -maxOffsetX,
                                                    maxOffsetX,
                                                )
                                        } else {
                                            offsetX = 0f
                                        }

                                        event.changes.forEach {
                                            if (it.positionChanged()) {
                                                it.consume()
                                            }
                                        }
                                    } else if (activePointers.size == 1) {
                                        val change = activePointers.first()
                                        velocityTracker.addPosition(
                                            change.uptimeMillis,
                                            change.position,
                                        )

                                        if (scale > 1.05f) {
                                            val panX = change.position.x - change.previousPosition.x
                                            if (panX != 0f) {
                                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                                offsetX =
                                                    (offsetX + panX).coerceIn(
                                                        -maxOffsetX,
                                                        maxOffsetX,
                                                    )
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (scale < 1f && !viewer.config.enableZoomOut) {
                                    coroutineScope.launch {
                                        val animScale = Animatable(scale)
                                        val animX = Animatable(offsetX)
                                        launch {
                                            animScale.animateTo(1f, tween(200)) { scale = value }
                                        }
                                        launch {
                                            animX.animateTo(0f, tween(200)) { offsetX = value }
                                        }
                                    }
                                } else if (scale > 1.05f) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    if (abs(velocity.x) > 100f) {
                                        coroutineScope.launch {
                                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                            val animX = Animatable(offsetX)
                                            val targetX =
                                                (offsetX + velocity.x * 0.15f).coerceIn(
                                                    -maxOffsetX,
                                                    maxOffsetX,
                                                )
                                            animX.animateTo(targetX, tween(200)) { offsetX = value }
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(viewer) {
                            var lastTapTime = 0L
                            var lastTapOffset = Offset.Zero
                            var pendingMenuJob: Job? = null

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTime = System.currentTimeMillis()
                                val downPos = down.position
                                var isLongPressTriggered = false
                                var pointerUp: PointerInputChange? = null

                                try {
                                    withTimeout(longPressTimeoutMs) {
                                        pointerUp = waitForUpOrCancellation()
                                    }
                                } catch (_: PointerEventTimeoutCancellationException) {
                                    if (
                                        viewer.activity.menuVisible || viewer.config.longTapEnabled
                                    ) {
                                        val activeItem =
                                            currentItems.getOrNull(
                                                lazyListState.firstVisibleItemIndex
                                            )
                                        val page =
                                            when (activeItem) {
                                                is ReaderUiItem.Page -> activeItem.page
                                                is ReaderUiItem.SplitPage -> activeItem.page
                                                is ReaderUiItem.Transition,
                                                null -> null
                                            }
                                        if (page != null) {
                                            viewer.activity.onPageLongTap(page)
                                            isLongPressTriggered = true
                                        }
                                    }
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                }

                                if (!isLongPressTriggered && pointerUp != null) {
                                    val up = pointerUp!!
                                    val upPos = up.position
                                    val upTime = System.currentTimeMillis()
                                    val distance =
                                        hypot(
                                            (upPos.x - downPos.x).toDouble(),
                                            (upPos.y - downPos.y).toDouble(),
                                        )

                                    if (distance < touchSlopPx) {
                                        val screenWidth = size.width.toFloat()
                                        val screenHeight = size.height.toFloat()

                                        if (screenWidth > 0 && screenHeight > 0) {
                                            val pos =
                                                PointF(
                                                    upPos.x / screenWidth,
                                                    upPos.y / screenHeight,
                                                )
                                            val navigator = viewer.config.navigator
                                            val action = navigator.getAction(pos)

                                            val isDoubleTap =
                                                (upTime - lastTapTime < doubleTapTimeoutMs) &&
                                                    (hypot(
                                                        (upPos.x - lastTapOffset.x).toDouble(),
                                                        (upPos.y - lastTapOffset.y).toDouble(),
                                                    ) < doubleTapSlopPx) &&
                                                    (viewer.config.doubleTapAnimDuration > 0)

                                            if (isDoubleTap) {
                                                pendingMenuJob?.cancel()
                                                lastTapTime = 0L
                                                lastTapOffset = Offset.Zero
                                                val animDuration =
                                                    viewer.config.doubleTapAnimDuration
                                                        .coerceAtLeast(100)
                                                coroutineScope.launch {
                                                    if (scale > 1.05f || scale < 0.95f) {
                                                        val animScale = Animatable(scale)
                                                        val animX = Animatable(offsetX)
                                                        launch {
                                                            animScale.animateTo(
                                                                1f,
                                                                tween(animDuration),
                                                            ) {
                                                                scale = value
                                                            }
                                                        }
                                                        launch {
                                                            animX.animateTo(
                                                                0f,
                                                                tween(animDuration),
                                                            ) {
                                                                offsetX = value
                                                            }
                                                        }
                                                    } else {
                                                        val targetScale = 2.5f
                                                        val targetX =
                                                            ((size.width / 2f) - upPos.x) *
                                                                (targetScale - 1f)
                                                        val maxOffsetX =
                                                            (size.width * (targetScale - 1f)) / 2f
                                                        val boundedX =
                                                            targetX.coerceIn(
                                                                -maxOffsetX,
                                                                maxOffsetX,
                                                            )

                                                        val animScale = Animatable(scale)
                                                        val animX = Animatable(offsetX)
                                                        launch {
                                                            animScale.animateTo(
                                                                targetScale,
                                                                tween(animDuration),
                                                            ) {
                                                                scale = value
                                                            }
                                                        }
                                                        launch {
                                                            animX.animateTo(
                                                                boundedX,
                                                                tween(animDuration),
                                                            ) {
                                                                offsetX = value
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                lastTapTime = upTime
                                                lastTapOffset = upPos

                                                when (action) {
                                                    ViewerNavigation.NavigationRegion.MENU -> {
                                                        if (
                                                            viewer.config.doubleTapAnimDuration > 0
                                                        ) {
                                                            pendingMenuJob?.cancel()
                                                            pendingMenuJob = coroutineScope.launch {
                                                                delay(200)
                                                                viewer.activity.toggleMenu()
                                                            }
                                                        } else {
                                                            viewer.activity.toggleMenu()
                                                        }
                                                    }
                                                    ViewerNavigation.NavigationRegion.NEXT,
                                                    ViewerNavigation.NavigationRegion.RIGHT -> {
                                                        pendingMenuJob?.cancel()
                                                        if (viewer.activity.menuVisible) {
                                                            viewer.activity.hideMenu()
                                                        }
                                                        viewer.moveToNext()
                                                    }
                                                    ViewerNavigation.NavigationRegion.PREV,
                                                    ViewerNavigation.NavigationRegion.LEFT -> {
                                                        pendingMenuJob?.cancel()
                                                        if (viewer.activity.menuVisible) {
                                                            viewer.activity.hideMenu()
                                                        }
                                                        viewer.moveToPrevious()
                                                    }
                                                }
                                            }
                                        } else {
                                            viewer.activity.toggleMenu()
                                        }
                                    }
                                }
                            }
                        },
                contentPadding = PaddingValues(bottom = if (hasMargins) Size.medium else Size.none),
            ) {
                items(
                    items = items,
                    key = { item -> item.key("webtoon") },
                ) { item ->
                    when (item) {
                        is ReaderUiItem.Page -> {
                            WebtoonPageItem(
                                viewer = viewer,
                                item = item.page,
                                modifier =
                                    if (horizontalPadding > Size.none) {
                                        Modifier.padding(horizontal = horizontalPadding)
                                    } else {
                                        Modifier
                                    },
                            )
                        }
                        is ReaderUiItem.SplitPage -> {
                            WebtoonPageItem(
                                viewer = viewer,
                                item = item.split,
                                modifier =
                                    if (horizontalPadding > Size.none) {
                                        Modifier.padding(horizontal = horizontalPadding)
                                    } else {
                                        Modifier
                                    },
                            )
                        }
                        is ReaderUiItem.Transition -> {
                            ReaderTransitionPage(
                                transition = item.transition,
                                manga = manga,
                                downloadManager = downloadManager,
                                onRetry = onRetryTransition,
                                onTap = { pos ->
                                    val navigator = viewer.config.navigator
                                    when (navigator.getAction(pos)) {
                                        ViewerNavigation.NavigationRegion.MENU ->
                                            viewer.activity.toggleMenu()
                                        ViewerNavigation.NavigationRegion.NEXT,
                                        ViewerNavigation.NavigationRegion.RIGHT -> {
                                            if (viewer.activity.menuVisible) {
                                                viewer.activity.hideMenu()
                                            }
                                            viewer.moveToNext()
                                        }
                                        ViewerNavigation.NavigationRegion.PREV,
                                        ViewerNavigation.NavigationRegion.LEFT -> {
                                            if (viewer.activity.menuVisible) {
                                                viewer.activity.hideMenu()
                                            }
                                            viewer.moveToPrevious()
                                        }
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            top =
                                                if (
                                                    item.transition is ChapterTransition.Prev &&
                                                        item.transition.to == null
                                                ) {
                                                    Size.appBarHeight + Size.large
                                                } else {
                                                    Size.small
                                                },
                                            bottom = Size.extraLarge,
                                        ),
                            )
                        }
                    }
                }
            }
        }
    }
}
