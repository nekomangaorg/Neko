package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposePagerViewer(
    viewer: PagerViewer,
    items: List<Any>,
    isRtl: Boolean,
    isVertical: Boolean,
    manga: Manga?,
    downloadManager: DownloadManager,
    onPageSelected: (ReaderPage, Boolean) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onRetryTransition: (ReaderChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentChapterId =
        (viewer.adapter.currentChapter
                ?: items
                    .firstOrNull {
                        it is ReaderPage || (it is Pair<*, *> && it.first is ReaderPage)
                    }
                    ?.let {
                        if (it is Pair<*, *>) (it.first as? ReaderPage)?.chapter
                        else (it as? ReaderPage)?.chapter
                    })
            ?.chapter
            ?.id

    key(viewer::class, currentChapterId, isRtl, isVertical) {
        val defaultPageIndex =
            items
                .indexOfFirst { it is ReaderPage || (it is Pair<*, *> && it.first is ReaderPage) }
                .takeIf { it != -1 } ?: 0

        val initialPage =
            (viewer.requestedPagePosition?.first ?: defaultPageIndex).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val pagerState =
            rememberPagerState(
                initialPage = initialPage,
                pageCount = { items.size },
            )

        LaunchedEffect(viewer.requestedPagePosition) {
            viewer.requestedPagePosition?.let { (targetPage, animated) ->
                if (
                    targetPage in 0 until pagerState.pageCount &&
                        targetPage != pagerState.currentPage
                ) {
                    if (animated) {
                        pagerState.animateScrollToPage(targetPage)
                    } else {
                        pagerState.scrollToPage(targetPage)
                    }
                }
            }
        }

        LaunchedEffect(pagerState.interactionSource) {
            pagerState.interactionSource.interactions.collect { interaction ->
                if (
                    interaction is androidx.compose.foundation.interaction.DragInteraction.Start &&
                        viewer.activity.menuVisible
                ) {
                    viewer.activity.hideMenu()
                }
            }
        }

        LaunchedEffect(pagerState, items) {
            snapshotFlow { pagerState.currentPage }
                .collect { pageIndex ->
                    if (pageIndex in items.indices) {
                        val item = items[pageIndex]
                        val unwrapped = if (item is Pair<*, *>) item.first else item
                        val extra = if (item is Pair<*, *>) item.second as? ReaderPage else null
                        when (unwrapped) {
                            is ReaderPage -> onPageSelected(unwrapped, extra != null)
                            is ChapterTransition -> onTransitionSelected(unwrapped)
                        }
                    }
                }
        }

        Box(modifier = modifier.fillMaxSize()) {
            if (isVertical) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { index ->
                        val item = items.getOrNull(index)
                        val unwrapped = if (item is Pair<*, *>) item.first else item
                        when (unwrapped) {
                            is ReaderPage ->
                                "pager_page_${unwrapped.chapter.chapter.id}_${unwrapped.index}_$index"
                            is ChapterTransition ->
                                "pager_transition_${(unwrapped as? ChapterTransition.Prev)?.let { "prev" } ?: "next"}_${unwrapped.from.chapter.id}_${unwrapped.to?.chapter?.id}_$index"
                            else -> "pager_item_${index}_${item.hashCode()}"
                        }
                    },
                ) { index ->
                    val item = items.getOrNull(index) ?: return@VerticalPager
                    PagerItemContent(
                        viewer = viewer,
                        item = item,
                        manga = manga,
                        downloadManager = downloadManager,
                        onRetryTransition = onRetryTransition,
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { index ->
                        val item = items.getOrNull(index)
                        val unwrapped = if (item is Pair<*, *>) item.first else item
                        when (unwrapped) {
                            is ReaderPage ->
                                "pager_page_${unwrapped.chapter.chapter.id}_${unwrapped.index}_$index"
                            is ChapterTransition ->
                                "pager_transition_${(unwrapped as? ChapterTransition.Prev)?.let { "prev" } ?: "next"}_${unwrapped.from.chapter.id}_${unwrapped.to?.chapter?.id}_$index"
                            else -> "pager_item_${index}_${item.hashCode()}"
                        }
                    },
                ) { index ->
                    val item = items.getOrNull(index) ?: return@HorizontalPager
                    PagerItemContent(
                        viewer = viewer,
                        item = item,
                        manga = manga,
                        downloadManager = downloadManager,
                        onRetryTransition = onRetryTransition,
                    )
                }
            }
        }
    }
}

@Composable
private fun PagerItemContent(
    viewer: PagerViewer,
    item: Any,
    manga: Manga?,
    downloadManager: DownloadManager,
    onRetryTransition: (ReaderChapter) -> Unit,
) {
    val unwrapped = if (item is Pair<*, *>) item.first else item
    val extra = if (item is Pair<*, *>) item.second as? ReaderPage else null

    when (unwrapped) {
        is ReaderPage -> {
            PagerPageItem(
                viewer = viewer,
                page = unwrapped,
                extraPage = extra,
            )
        }
        is ChapterTransition -> {
            ReaderTransitionPage(
                transition = unwrapped,
                manga = manga,
                downloadManager = downloadManager,
                onRetry = onRetryTransition,
                onTap = { viewer.activity.toggleMenu() },
            )
        }
    }
}
