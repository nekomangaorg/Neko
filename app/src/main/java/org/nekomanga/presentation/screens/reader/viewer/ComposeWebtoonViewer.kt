package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlin.math.abs
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

    key(viewer, currentChapterId, items.size > 1) {
        val defaultPageIndex =
            items
                .indexOfFirst { it is ReaderUiItem.Page || it is ReaderUiItem.SplitPage }
                .takeIf { it != -1 } ?: 0
        val initialItemIndex =
            (viewer.requestedPagePosition?.targetPage ?: defaultPageIndex).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()

        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val readerTheme by readerPreferences.readerTheme().collectAsState()
        val themeBackground = MaterialTheme.colorScheme.background
        val backgroundColor =
            remember(readerTheme, themeBackground) {
                when (readerTheme) {
                    0 -> Color.White
                    1 -> Color.Black
                    2 -> Color.White
                    3 -> themeBackground
                    4 -> Color.Black
                    else -> themeBackground
                }
            }

        var lastActiveItem by remember { mutableStateOf<ReaderUiItem?>(null) }

        LaunchedEffect(currentChapterId) {
            viewer.adapter.prevTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
            viewer.adapter.nextTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
        }

        LaunchedEffect(items) {
            val activeItem = lastActiveItem
            if (activeItem != null) {
                val newIndex = items.indexOfFirst { item ->
                    val itemPage =
                        when (item) {
                            is ReaderUiItem.Page -> item.page
                            is ReaderUiItem.SplitPage -> item.page
                            is ReaderUiItem.Transition -> null
                        }
                    val activePage =
                        when (activeItem) {
                            is ReaderUiItem.Page -> activeItem.page
                            is ReaderUiItem.SplitPage -> activeItem.page
                            is ReaderUiItem.Transition -> null
                        }
                    if (itemPage != null && activePage != null) {
                        itemPage.chapter.chapter.id == activePage.chapter.chapter.id &&
                            itemPage.index == activePage.index
                    } else if (
                        item is ReaderUiItem.Transition && activeItem is ReaderUiItem.Transition
                    ) {
                        val itemIsPrev = item.transition is ChapterTransition.Prev
                        val activeIsPrev = activeItem.transition is ChapterTransition.Prev
                        itemIsPrev == activeIsPrev &&
                            item.transition.from.chapter.id ==
                                activeItem.transition.from.chapter.id &&
                            item.transition.to?.chapter?.id == activeItem.transition.to?.chapter?.id
                    } else {
                        item == activeItem
                    }
                }
                if (newIndex != -1 && newIndex != lazyListState.firstVisibleItemIndex) {
                    val offset = lazyListState.firstVisibleItemScrollOffset
                    lazyListState.scrollToItem(newIndex, offset)
                }
            }
        }

        // Sync programmatic page jumps (slider, TOC, etc.)
        LaunchedEffect(viewer.requestedPagePosition) {
            val req = viewer.requestedPagePosition ?: return@LaunchedEffect
            val target = req.targetPage
            if (target in items.indices) {
                if (lazyListState.firstVisibleItemIndex != target) {
                    if (req.animated) {
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
                lazyListState.animateScrollBy(delta.toFloat())
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
        LaunchedEffect(lazyListState, items) {
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
                    activeItemInfo.index
                } else {
                    lazyListState.firstVisibleItemIndex
                }
            }
                .collect { activeIndex ->
                    if (activeIndex in items.indices) {
                        val item = items[activeIndex]
                        lastActiveItem = item
                        when (item) {
                            is ReaderUiItem.Page -> {
                                onPageSelected(item.page)
                                val pages = item.page.chapter.pages
                                if (
                                    pages != null &&
                                        item.page.chapter == viewer.adapter.currentChapter
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
        }

        val sidePaddingPercent = viewer.config.sidePadding / 100f
        val hasMargins = viewer.hasMargins

        Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
            LazyColumn(
                state = lazyListState,
                userScrollEnabled = scale <= 1.05f,
                modifier =
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .pointerInput(viewer.config.enableZoomOut) {
                            detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
                                val newScale =
                                    (scale * zoom).coerceIn(
                                        if (viewer.config.enableZoomOut) 0.5f else 1f,
                                        3f,
                                    )
                                scale = newScale
                                if (newScale > 1f) {
                                    val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                    offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                            if (scale < 1f) {
                                coroutineScope.launch {
                                    val animScale = Animatable(scale)
                                    val animX = Animatable(offsetX)
                                    val animY = Animatable(offsetY)
                                    launch { animScale.animateTo(1f, tween(200)) { scale = value } }
                                    launch { animX.animateTo(0f, tween(200)) { offsetX = value } }
                                    launch { animY.animateTo(0f, tween(200)) { offsetY = value } }
                                }
                            }
                        }
                        .pointerInput(viewer, items) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    coroutineScope.launch {
                                        if (scale > 1.05f) {
                                            val animScale = Animatable(scale)
                                            val animX = Animatable(offsetX)
                                            val animY = Animatable(offsetY)
                                            launch {
                                                animScale.animateTo(1f, tween(250)) {
                                                    scale = value
                                                }
                                            }
                                            launch {
                                                animX.animateTo(0f, tween(250)) { offsetX = value }
                                            }
                                            launch {
                                                animY.animateTo(0f, tween(250)) { offsetY = value }
                                            }
                                        } else {
                                            val targetScale = 2.5f
                                            val targetX =
                                                ((size.width / 2f) - offset.x) * (targetScale - 1f)
                                            val targetY =
                                                ((size.height / 2f) - offset.y) * (targetScale - 1f)
                                            val maxOffsetX = (size.width * (targetScale - 1f)) / 2f
                                            val maxOffsetY = (size.height * (targetScale - 1f)) / 2f
                                            val boundedX = targetX.coerceIn(-maxOffsetX, maxOffsetX)
                                            val boundedY = targetY.coerceIn(-maxOffsetY, maxOffsetY)

                                            val animScale = Animatable(scale)
                                            val animX = Animatable(offsetX)
                                            val animY = Animatable(offsetY)
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
                                            launch {
                                                animY.animateTo(boundedY, tween(250)) {
                                                    offsetY = value
                                                }
                                            }
                                        }
                                    }
                                },
                                onTap = { offset ->
                                    if (scale > 1.05f) {
                                        coroutineScope.launch {
                                            val animScale = Animatable(scale)
                                            val animX = Animatable(offsetX)
                                            val animY = Animatable(offsetY)
                                            launch {
                                                animScale.animateTo(1f, tween(200)) {
                                                    scale = value
                                                }
                                            }
                                            launch {
                                                animX.animateTo(0f, tween(200)) { offsetX = value }
                                            }
                                            launch {
                                                animY.animateTo(0f, tween(200)) { offsetY = value }
                                            }
                                        }
                                    } else {
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
                                    }
                                },
                                onLongPress = {
                                    if (
                                        viewer.activity.menuVisible || viewer.config.longTapEnabled
                                    ) {
                                        val activeItem =
                                            items.getOrNull(lazyListState.firstVisibleItemIndex)
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
                itemsIndexed(
                    items = items,
                    key = { index, item ->
                        when (item) {
                            is ReaderUiItem.Page ->
                                "page_${item.page.chapter.chapter.id}_${item.page.index}_$index"
                            is ReaderUiItem.SplitPage ->
                                "split_${item.page.chapter.chapter.id}_${item.page.index}_${item.split.topOffset}_$index"
                            is ReaderUiItem.Transition ->
                                "transition_${(item.transition as? ChapterTransition.Prev)?.let { "prev" } ?: "next"}_${item.transition.from.chapter.id}_${item.transition.to?.chapter?.id}_$index"
                        }
                    },
                ) { _, item ->
                    when (item) {
                        is ReaderUiItem.Page,
                        is ReaderUiItem.SplitPage -> {
                            WebtoonPageItem(
                                viewer = viewer,
                                item = item,
                                modifier =
                                    if (sidePaddingPercent > 0f) {
                                        Modifier.padding(horizontal = (sidePaddingPercent * 100).dp)
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
                                onTap = {
                                    val toChapter = item.transition.to
                                    if (toChapter != null) {
                                        coroutineScope.launch {
                                            viewer.activity.loadChapter(toChapter.chapter)
                                        }
                                    } else {
                                        viewer.activity.toggleMenu()
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            top =
                                                if (item.transition is ChapterTransition.Prev) {
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
