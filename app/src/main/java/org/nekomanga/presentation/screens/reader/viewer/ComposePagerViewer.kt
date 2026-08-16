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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import kotlinx.coroutines.launch

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

    key(viewer::class, currentChapterId, items.size > 1, isRtl, isVertical) {
        val defaultPageIndex =
            items
                .indexOfFirst {
                    val p = if (it is Pair<*, *>) it.first else it
                    (p as? ReaderPage)?.chapter?.chapter?.id == currentChapterId
                }
                .takeIf { it != -1 }
                ?: items
                    .indexOfFirst {
                        it is ReaderPage || (it is Pair<*, *> && it.first is ReaderPage)
                    }
                    .takeIf { it != -1 }
                ?: 0

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

        var lastActiveItem by remember { mutableStateOf<Any?>(null) }
        var isTransitioning by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(currentChapterId) {
            viewer.adapter.prevTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
            viewer.adapter.nextTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
        }

        LaunchedEffect(items) {
            val activeItem = lastActiveItem
            if (activeItem != null) {
                val newIndex = items.indexOfFirst { item ->
                    val itemUnwrapped = if (item is Pair<*, *>) item.first else item
                    val activeUnwrapped =
                        if (activeItem is Pair<*, *>) activeItem.first else activeItem
                    if (itemUnwrapped is ReaderPage && activeUnwrapped is ReaderPage) {
                        itemUnwrapped.chapter.chapter.id == activeUnwrapped.chapter.chapter.id &&
                            itemUnwrapped.index == activeUnwrapped.index
                    } else if (
                        itemUnwrapped is ChapterTransition && activeUnwrapped is ChapterTransition
                    ) {
                        val itemIsPrev = itemUnwrapped is ChapterTransition.Prev
                        val activeIsPrev = activeUnwrapped is ChapterTransition.Prev
                        itemIsPrev == activeIsPrev &&
                            itemUnwrapped.from.chapter.id == activeUnwrapped.from.chapter.id &&
                            itemUnwrapped.to?.chapter?.id == activeUnwrapped.to?.chapter?.id
                    } else {
                        item == activeItem
                    }
                }
                if (newIndex != -1 && newIndex != pagerState.currentPage) {
                    pagerState.scrollToPage(newIndex)
                }
            }
        }

        LaunchedEffect(viewer.requestedPagePosition) {
            val req = viewer.requestedPagePosition ?: return@LaunchedEffect
            val target = req.first
            if (target in 0 until pagerState.pageCount) {
                if (target != pagerState.currentPage) {
                    if (req.second) {
                        pagerState.animateScrollToPage(target)
                    } else {
                        pagerState.scrollToPage(target)
                    }
                }
                viewer.requestedPagePosition = null
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
                    viewer.onPageChange(pageIndex)
                    if (pageIndex in items.indices) {
                        val item = items[pageIndex]
                        lastActiveItem = item
                        val unwrapped = if (item is Pair<*, *>) item.first else item
                        val extra = if (item is Pair<*, *>) item.second as? ReaderPage else null
                        when (unwrapped) {
                            is ReaderPage -> {
                                onPageSelected(unwrapped, extra != null)
                                val pages = unwrapped.chapter.pages
                                if (
                                    pages != null &&
                                        unwrapped.chapter == viewer.adapter.currentChapter
                                ) {
                                    if (pages.size - unwrapped.number < 5) {
                                        viewer.adapter.nextTransition?.to?.let {
                                            viewer.activity.requestPreloadChapter(it)
                                        }
                                    }
                                    if (unwrapped.number <= 5) {
                                        viewer.adapter.prevTransition?.to?.let {
                                            viewer.activity.requestPreloadChapter(it)
                                        }
                                    }
                                }
                            }
                            is ChapterTransition -> {
                                onTransitionSelected(unwrapped)
                            }
                        }
                    }
                }
        }

        val nestedScrollConnection =
            remember(pagerState, items, isVertical) {
                object : NestedScrollConnection {
                    var accumulatedOverscroll = 0f

                    private fun checkAndTrigger(delta: Float) {
                        val currentIndex = pagerState.currentPage
                        val currentItem = items.getOrNull(currentIndex)
                        val unwrapped =
                            if (currentItem is Pair<*, *>) currentItem.first else currentItem

                        if (unwrapped is ChapterTransition) {
                            val toChapter = unwrapped.to
                            if (toChapter != null) {
                                accumulatedOverscroll += delta
                                val isTrigger =
                                    if (unwrapped is ChapterTransition.Prev) {
                                        accumulatedOverscroll > 40f
                                    } else {
                                        accumulatedOverscroll < -40f
                                    }
                                if (isTrigger && !isTransitioning) {
                                    accumulatedOverscroll = 0f
                                    isTransitioning = true
                                    coroutineScope.launch {
                                        try {
                                            viewer.activity.loadChapter(toChapter.chapter)
                                        } finally {
                                            isTransitioning = false
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        if (source == NestedScrollSource.UserInput) {
                            val delta = if (isVertical) available.y else available.x
                            val isAtStartEdge = pagerState.currentPage == 0 && delta > 0
                            val isAtEndEdge =
                                pagerState.currentPage == pagerState.pageCount - 1 && delta < 0
                            if (isAtStartEdge || isAtEndEdge) {
                                checkAndTrigger(delta)
                            }
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        if (source == NestedScrollSource.UserInput) {
                            val delta = if (isVertical) available.y else available.x
                            if (delta != 0f) {
                                checkAndTrigger(delta)
                            }
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        accumulatedOverscroll = 0f
                        return Velocity.Zero
                    }
                }
            }

        Box(modifier = modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
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
            val coroutineScope = rememberCoroutineScope()
            ReaderTransitionPage(
                transition = unwrapped,
                manga = manga,
                downloadManager = downloadManager,
                onRetry = onRetryTransition,
                onTap = {
                    val toChapter = unwrapped.to
                    if (toChapter != null) {
                        coroutineScope.launch { viewer.activity.loadChapter(toChapter.chapter) }
                    } else {
                        viewer.activity.toggleMenu()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
