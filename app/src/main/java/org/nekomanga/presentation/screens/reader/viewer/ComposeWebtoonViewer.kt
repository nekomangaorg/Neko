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
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ComposeWebtoonViewer(
    viewer: WebtoonViewer,
    items: List<Any>,
    manga: Manga?,
    downloadManager: DownloadManager,
    onPageSelected: (ReaderPage) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onRetryTransition: (ReaderChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentChapterId =
        (viewer.adapter.currentChapter
                ?: items.firstOrNull { it is ReaderPage }?.let { (it as ReaderPage).chapter })
            ?.chapter
            ?.id

    key(viewer, currentChapterId, items.size > 1) {
        val defaultPageIndex =
            items.indexOfFirst { it is ReaderPage || it is ReaderPageSplit }.takeIf { it != -1 }
                ?: 0
        val initialItemIndex =
            (viewer.requestedPagePosition?.targetPage ?: defaultPageIndex).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)

        val scaleAnimatable = remember { Animatable(1f) }
        val offsetXAnimatable = remember { Animatable(0f) }
        val offsetYAnimatable = remember { Animatable(0f) }
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

        var lastActiveItem by remember { mutableStateOf<Any?>(null) }

        LaunchedEffect(currentChapterId) {
            viewer.adapter.prevTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
            viewer.adapter.nextTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
        }

        LaunchedEffect(items) {
            val activeItem = lastActiveItem
            if (activeItem != null) {
                val newIndex = items.indexOfFirst { item ->
                    val itemPage = (item as? ReaderPage) ?: (item as? ReaderPageSplit)?.page
                    val activePage =
                        (activeItem as? ReaderPage) ?: (activeItem as? ReaderPageSplit)?.page
                    if (itemPage != null && activePage != null) {
                        itemPage.chapter.chapter.id == activePage.chapter.chapter.id &&
                            itemPage.index == activePage.index
                    } else if (item is ChapterTransition && activeItem is ChapterTransition) {
                        val itemIsPrev = item is ChapterTransition.Prev
                        val activeIsPrev = activeItem is ChapterTransition.Prev
                        itemIsPrev == activeIsPrev &&
                            item.from.chapter.id == activeItem.from.chapter.id &&
                            item.to?.chapter?.id == activeItem.to?.chapter?.id
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
                            is ReaderPage -> {
                                onPageSelected(item)
                                val pages = item.chapter.pages
                                if (
                                    pages != null && item.chapter == viewer.adapter.currentChapter
                                ) {
                                    if (pages.size - item.number < 5) {
                                        viewer.adapter.nextTransition?.to?.let {
                                            viewer.activity.requestPreloadChapter(it)
                                        }
                                    }
                                    if (item.number <= 5) {
                                        viewer.adapter.prevTransition?.to?.let {
                                            viewer.activity.requestPreloadChapter(it)
                                        }
                                    }
                                }
                            }
                            is ReaderPageSplit -> {
                                onPageSelected(item.page)
                            }
                            is ChapterTransition -> {
                                onTransitionSelected(item)
                                val toChapter = item.to
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
                userScrollEnabled = scaleAnimatable.value <= 1.05f,
                modifier =
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            scaleX = scaleAnimatable.value
                            scaleY = scaleAnimatable.value
                            translationX = offsetXAnimatable.value
                            translationY = offsetYAnimatable.value
                        }
                        .pointerInput(viewer.config.enableZoomOut) {
                            detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
                                val currentScale = scaleAnimatable.value
                                val newScale =
                                    (currentScale * zoom).coerceIn(
                                        if (viewer.config.enableZoomOut) 0.5f else 1f,
                                        3f,
                                    )
                                val currentX = offsetXAnimatable.value
                                val currentY = offsetYAnimatable.value
                                coroutineScope.launch {
                                    scaleAnimatable.snapTo(newScale)
                                    if (newScale > 1f) {
                                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                        val newX =
                                            (currentX + pan.x).coerceIn(
                                                -maxOffsetX,
                                                maxOffsetX,
                                            )
                                        val newY =
                                            (currentY + pan.y).coerceIn(
                                                -maxOffsetY,
                                                maxOffsetY,
                                            )
                                        offsetXAnimatable.snapTo(newX)
                                        offsetYAnimatable.snapTo(newY)
                                    } else {
                                        offsetXAnimatable.snapTo(0f)
                                        offsetYAnimatable.snapTo(0f)
                                    }
                                }
                            }
                            if (scaleAnimatable.value < 1f) {
                                coroutineScope.launch {
                                    scaleAnimatable.animateTo(1f, tween(200))
                                    offsetXAnimatable.animateTo(0f, tween(200))
                                    offsetYAnimatable.animateTo(0f, tween(200))
                                }
                            }
                        }
                        .pointerInput(viewer, items) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    coroutineScope.launch {
                                        if (scaleAnimatable.value > 1.05f) {
                                            launch { scaleAnimatable.animateTo(1f, tween(250)) }
                                            launch { offsetXAnimatable.animateTo(0f, tween(250)) }
                                            launch { offsetYAnimatable.animateTo(0f, tween(250)) }
                                        } else {
                                            val targetScale = 2.5f
                                            val targetX =
                                                ((size.width / 2f) - offset.x) * (targetScale - 1f)
                                            val targetY =
                                                ((size.height / 2f) - offset.y) * (targetScale - 1f)
                                            val maxOffsetX = (size.width * (targetScale - 1f)) / 2f
                                            val maxOffsetY = (size.height * (targetScale - 1f)) / 2f
                                            launch {
                                                scaleAnimatable.animateTo(targetScale, tween(250))
                                            }
                                            launch {
                                                offsetXAnimatable.animateTo(
                                                    targetX.coerceIn(-maxOffsetX, maxOffsetX),
                                                    tween(250),
                                                )
                                            }
                                            launch {
                                                offsetYAnimatable.animateTo(
                                                    targetY.coerceIn(-maxOffsetY, maxOffsetY),
                                                    tween(250),
                                                )
                                            }
                                        }
                                    }
                                },
                                onTap = { offset ->
                                    if (scaleAnimatable.value > 1.05f) {
                                        coroutineScope.launch {
                                            launch { scaleAnimatable.animateTo(1f, tween(200)) }
                                            launch { offsetXAnimatable.animateTo(0f, tween(200)) }
                                            launch { offsetYAnimatable.animateTo(0f, tween(200)) }
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
                                            (activeItem as? ReaderPage)
                                                ?: (activeItem as? ReaderPageSplit)?.page
                                        if (page != null) {
                                            viewer.activity.onPageLongTap(page)
                                        }
                                    }
                                },
                            )
                        },
                contentPadding = PaddingValues(bottom = if (hasMargins) 15.dp else 0.dp),
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item ->
                        when (item) {
                            is ReaderPage -> "page_${item.chapter.chapter.id}_${item.index}_$index"
                            is ReaderPageSplit ->
                                "split_${item.page.chapter.chapter.id}_${item.page.index}_${item.topOffset}_$index"
                            is ChapterTransition ->
                                "transition_${(item as? ChapterTransition.Prev)?.let { "prev" } ?: "next"}_${item.from.chapter.id}_${item.to?.chapter?.id}_$index"
                            else -> "item_${index}_${item.hashCode()}"
                        }
                    },
                ) { _, item ->
                    when (item) {
                        is ReaderPage,
                        is ReaderPageSplit -> {
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
                        is ChapterTransition -> {
                            ReaderTransitionPage(
                                transition = item,
                                manga = manga,
                                downloadManager = downloadManager,
                                onRetry = onRetryTransition,
                                onTap = {
                                    val toChapter = item.to
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
                                                if (item is ChapterTransition.Prev) {
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
