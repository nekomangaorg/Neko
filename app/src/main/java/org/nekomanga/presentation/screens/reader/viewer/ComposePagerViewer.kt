package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    pagerState: PagerState = rememberPagerState(pageCount = { items.size }),
) {
    LaunchedEffect(pagerState, items) {
        snapshotFlow { pagerState.currentPage }
            .collect { pageIndex ->
                if (pageIndex in items.indices) {
                    when (val currentItem = items[pageIndex]) {
                        is ReaderPage -> onPageSelected(currentItem, false)
                        is Pair<*, *> -> {
                            val first = currentItem.first as? ReaderPage
                            val second = currentItem.second as? ReaderPage
                            if (first != null) {
                                onPageSelected(first, second != null)
                            }
                        }
                        is ChapterTransition -> onTransitionSelected(currentItem)
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
                key = { index -> items.getOrNull(index).hashCode() },
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
                reverseLayout = isRtl,
                beyondViewportPageCount = 1,
                key = { index -> items.getOrNull(index).hashCode() },
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

@Composable
private fun PagerItemContent(
    viewer: PagerViewer,
    item: Any,
    manga: Manga?,
    downloadManager: DownloadManager,
    onRetryTransition: (ReaderChapter) -> Unit,
) {
    when (item) {
        is ReaderPage -> {
            PagerPageItem(
                viewer = viewer,
                page = item,
                extraPage = null,
            )
        }
        is Pair<*, *> -> {
            val first = item.first as? ReaderPage
            val second = item.second as? ReaderPage
            if (first != null) {
                PagerPageItem(
                    viewer = viewer,
                    page = first,
                    extraPage = second,
                )
            }
        }
        is ChapterTransition -> {
            ReaderTransitionPage(
                transition = item,
                manga = manga,
                downloadManager = downloadManager,
                onRetry = onRetryTransition,
            )
        }
    }
}
