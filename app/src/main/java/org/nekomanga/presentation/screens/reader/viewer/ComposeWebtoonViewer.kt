package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListPrefetchScope
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlin.math.abs
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
        (viewer.adapter.currentChapter
                ?: items
                    .firstOrNull { it is ReaderUiItem.Page }
                    ?.let { (it as ReaderUiItem.Page).page.chapter })
            ?.chapter
            ?.id

    key(viewer) {
        val defaultPageIndex =
            items
                .indexOfFirst { it is ReaderUiItem.Page || it is ReaderUiItem.SplitPage }
                .takeIf { it != -1 } ?: 0
        val initialItemIndex =
            (viewer.requestedPagePosition?.targetPage ?: defaultPageIndex).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val prefetchStrategy = remember { WebtoonPrefetchStrategy(prefetchCount = 5) }
        val lazyListState =
            rememberLazyListState(
                initialFirstVisibleItemIndex = initialItemIndex,
                prefetchStrategy = prefetchStrategy,
            )

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()

        val readerPreferences: ReaderPreferences = remember { Injekt.get() }
        val readerTheme by readerPreferences.readerTheme().collectAsState()
        val webtoonSidePadding by readerPreferences.webtoonSidePadding().collectAsState()
        val animatedTransitions by
            readerPreferences.animatedPageTransitionsWebtoon().collectAsState()
        val enableZoomOut by readerPreferences.webtoonEnableZoomOut().collectAsState()
        val themeBackground = MaterialTheme.colorScheme.background
        val backgroundColor =
            remember(readerTheme, themeBackground) {
                ReaderTheme.fromPreference(readerTheme).color(themeBackground)
            }

        val currentItems by rememberUpdatedState(items)

        LaunchedEffect(currentChapterId) {
            viewer.adapter.prevTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
            viewer.adapter.nextTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
        }

        // Sync programmatic page jumps (slider, TOC, etc.)
        LaunchedEffect(viewer.requestedPagePosition) {
            val req = viewer.requestedPagePosition ?: return@LaunchedEffect
            val target = req.targetPage
            if (target in currentItems.indices) {
                if (lazyListState.firstVisibleItemIndex != target) {
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

        // Track active visible page
        LaunchedEffect(lazyListState) {
            snapshotFlow {
                val layoutInfo = lazyListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportMiddle =
                        (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val activeItemInfo =
                        visibleItems.minByOrNull {
                            val itemMiddle = it.offset + it.size / 2
                            abs(itemMiddle - viewportMiddle)
                        } ?: visibleItems.first()
                    currentItems.getOrNull(activeItemInfo.index)
                } else {
                    currentItems.getOrNull(lazyListState.firstVisibleItemIndex)
                }
            }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { item ->
                    when (item) {
                        is ReaderUiItem.Page -> {
                            onPageSelected(item.page)
                            val pages = item.page.chapter.pages
                            if (
                                pages != null && item.page.chapter == viewer.adapter.currentChapter
                            ) {
                                if (pages.size - item.page.number < 5) {
                                    viewer.adapter.nextTransition?.to?.let {
                                        viewer.activity.requestPreloadChapter(it)
                                    }
                                }
                                if (item.page.number <= 5) {
                                    viewer.adapter.prevTransition?.to?.let {
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
        val hasMargins = viewer.hasMargins

        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize().background(backgroundColor).clipToBounds(),
        ) {
            val horizontalPadding = maxWidth * sidePaddingPercent
            val columnHeight = if (scale < 1f) maxHeight / scale else maxHeight
            LazyColumn(
                state = lazyListState,
                userScrollEnabled = true,
                modifier =
                    Modifier.fillMaxWidth()
                        .wrapContentHeight(unbounded = true)
                        .requiredHeight(columnHeight)
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
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val screenWidth = size.width.toFloat()
                                    val screenHeight = size.height.toFloat()
                                    val isNavigationRegion =
                                        if (screenWidth > 0 && screenHeight > 0) {
                                            val pos =
                                                PointF(
                                                    offset.x / screenWidth,
                                                    offset.y / screenHeight,
                                                )
                                            val navigator = viewer.config.navigator
                                            when (navigator.getAction(pos)) {
                                                ViewerNavigation.NavigationRegion.NEXT,
                                                ViewerNavigation.NavigationRegion.RIGHT -> {
                                                    if (viewer.activity.menuVisible) {
                                                        viewer.activity.hideMenu()
                                                    }
                                                    viewer.moveToNext()
                                                    true
                                                }
                                                ViewerNavigation.NavigationRegion.PREV,
                                                ViewerNavigation.NavigationRegion.LEFT -> {
                                                    if (viewer.activity.menuVisible) {
                                                        viewer.activity.hideMenu()
                                                    }
                                                    viewer.moveToPrevious()
                                                    true
                                                }
                                                ViewerNavigation.NavigationRegion.MENU -> false
                                            }
                                        } else {
                                            false
                                        }

                                    if (!isNavigationRegion) {
                                        coroutineScope.launch {
                                            if (scale > 1.05f || scale < 0.95f) {
                                                val animScale = Animatable(scale)
                                                val animX = Animatable(offsetX)
                                                launch {
                                                    animScale.animateTo(1f, tween(250)) {
                                                        scale = value
                                                    }
                                                }
                                                launch {
                                                    animX.animateTo(0f, tween(250)) {
                                                        offsetX = value
                                                    }
                                                }
                                            } else {
                                                val targetScale = 2.5f
                                                val targetX =
                                                    ((size.width / 2f) - offset.x) *
                                                        (targetScale - 1f)
                                                val maxOffsetX =
                                                    (size.width * (targetScale - 1f)) / 2f
                                                val boundedX =
                                                    targetX.coerceIn(-maxOffsetX, maxOffsetX)

                                                val animScale = Animatable(scale)
                                                val animX = Animatable(offsetX)
                                                launch {
                                                    animScale.animateTo(targetScale, tween(250)) {
                                                        scale = value
                                                    }
                                                }
                                                launch {
                                                    animX.animateTo(boundedX, tween(250)) {
                                                        offsetX = value
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onTap = { offset ->
                                    val screenWidth = size.width.toFloat()
                                    val screenHeight = size.height.toFloat()
                                    if (screenWidth > 0 && screenHeight > 0) {
                                        val pos =
                                            PointF(
                                                offset.x / screenWidth,
                                                offset.y / screenHeight,
                                            )
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
                                    } else {
                                        viewer.activity.toggleMenu()
                                    }
                                },
                                onLongPress = {
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
                                        }
                                    }
                                },
                            )
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

private class WebtoonPrefetchStrategy(private val prefetchCount: Int = 3) :
    LazyListPrefetchStrategy {
    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return
        if (delta < 0) {
            val lastVisible = layoutInfo.visibleItemsInfo.last().index
            for (i in 1..prefetchCount) {
                val nextIndex = lastVisible + i
                if (nextIndex < layoutInfo.totalItemsCount) {
                    schedulePrefetch(nextIndex)
                }
            }
        } else if (delta > 0) {
            val firstVisible = layoutInfo.visibleItemsInfo.first().index
            for (i in 1..prefetchCount) {
                val prevIndex = firstVisible - i
                if (prevIndex >= 0) {
                    schedulePrefetch(prevIndex)
                }
            }
        }
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return
        val lastVisible = layoutInfo.visibleItemsInfo.last().index
        val firstVisible = layoutInfo.visibleItemsInfo.first().index

        for (i in 1..prefetchCount) {
            val nextIndex = lastVisible + i
            if (nextIndex < layoutInfo.totalItemsCount) {
                schedulePrefetch(nextIndex)
            }
        }
        for (i in 1..prefetchCount) {
            val prevIndex = firstVisible - i
            if (prevIndex >= 0) {
                schedulePrefetch(prevIndex)
            }
        }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        for (i in 0 until prefetchCount) {
            schedulePrecomposition(firstVisibleItemIndex + i)
        }
    }
}
