package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
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
    key(viewer) {
        val lazyListState = rememberLazyListState()

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

        // Sync slider or programmatic page jumps
        LaunchedEffect(viewer.requestedPagePosition) {
            viewer.requestedPagePosition?.let { request ->
                if (request.targetPage in items.indices) {
                    if (request.animated) {
                        lazyListState.animateScrollToItem(request.targetPage)
                    } else {
                        lazyListState.scrollToItem(request.targetPage)
                    }
                }
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
                        when (val item = items[activeIndex]) {
                            is ReaderPage -> {
                                onPageSelected(item)
                                val pages = item.chapter.pages
                                if (
                                    pages != null &&
                                        pages.size - item.number < 5 &&
                                        item.chapter == viewer.adapter.currentChapter
                                ) {
                                    val nextItem = items.lastOrNull()
                                    val transitionChapter =
                                        (nextItem as? ChapterTransition.Next)?.to
                                            ?: (nextItem as? ReaderPage)?.chapter
                                    if (transitionChapter != null) {
                                        viewer.activity.requestPreloadChapter(transitionChapter)
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
                modifier =
                    Modifier.fillMaxSize().pointerInput(viewer, items) {
                        detectTapGestures(
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
                                if (viewer.activity.menuVisible || viewer.config.longTapEnabled) {
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
                                onTap = { viewer.activity.toggleMenu() },
                            )
                        }
                    }
                }
            }
        }
    }
}
