package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import coil3.imageLoader
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ComposePagerViewer(
    viewer: PagerViewer,
    items: List<ReaderUiItem>,
    isRtl: Boolean,
    isVertical: Boolean,
    manga: MangaItem?,
    downloadManager: DownloadManager,
    onPageSelected: (ReaderPage, Boolean) -> Unit,
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

    key(viewer, currentChapterId, isRtl, isVertical) {
        val defaultPageIndex =
            items
                .indexOfFirst { item ->
                    item is ReaderUiItem.Page &&
                        item.page.chapter.chapter.id == currentChapterId &&
                        (item.page.index == 0 || item.extraPage?.index == 0)
                }
                .takeIf { it != -1 }
                ?: items
                    .mapIndexedNotNull { index, item ->
                        if (
                            item is ReaderUiItem.Page &&
                                item.page.chapter.chapter.id == currentChapterId
                        ) {
                            index to minOf(item.page.index, item.extraPage?.index ?: Int.MAX_VALUE)
                        } else {
                            null
                        }
                    }
                    .minByOrNull { it.second }
                    ?.first
                ?: items.indexOfFirst { it is ReaderUiItem.Page }.takeIf { it != -1 }
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

        var lastActiveItem by remember { mutableStateOf<ReaderUiItem?>(null) }
        var isTransitioning by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(currentChapterId) {
            viewer.prevTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
            viewer.nextTransition?.to?.let { viewer.activity.requestPreloadChapter(it) }
        }

        LaunchedEffect(items) {
            val activeItem = lastActiveItem
            if (activeItem != null) {
                val newIndex = items.indexOfFirst { item ->
                    if (item is ReaderUiItem.Page && activeItem is ReaderUiItem.Page) {
                        item.page.chapter.chapter.id == activeItem.page.chapter.chapter.id &&
                            item.page.index == activeItem.page.index &&
                            item.page.firstHalf == activeItem.page.firstHalf
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
                        false
                    }
                }
                if (newIndex != -1 && newIndex != pagerState.currentPage) {
                    pagerState.scrollToPage(newIndex)
                }
            }
        }

        val readerPreferences: ReaderPreferences = remember { Injekt.get() }
        val animatedTransitions by readerPreferences.animatedPageTransitions().collectAsState()
        val readerTheme by readerPreferences.readerTheme().collectAsState()
        val themeBackground = MaterialTheme.colorScheme.background
        val backgroundColor =
            remember(readerTheme, themeBackground) {
                ReaderTheme.fromPreference(readerTheme).color(themeBackground)
            }

        // Sync programmatic page changes (slider, TOC, etc.)
        LaunchedEffect(viewer.requestedPagePosition) {
            val req = viewer.requestedPagePosition ?: return@LaunchedEffect
            val target = req.first
            if (target in items.indices) {
                if (pagerState.currentPage != target) {
                    val useAnimation = req.second && animatedTransitions
                    if (useAnimation) {
                        pagerState.animateScrollToPage(target)
                    } else {
                        pagerState.scrollToPage(target)
                    }
                }
                viewer.requestedPagePosition = null
            }
        }

        val context = LocalContext.current
        val preloadedKeys = remember { mutableSetOf<String>() }

        // Preload initial batch of pages when items are loaded or updated
        LaunchedEffect(items) {
            val startIndex =
                (viewer.requestedPagePosition?.first ?: defaultPageIndex).coerceIn(
                    0,
                    (items.size - 1).coerceAtLeast(0),
                )
            val preloadEnd = minOf(items.size - 1, startIndex + 4)
            for (i in startIndex..preloadEnd) {
                val item = items.getOrNull(i) ?: continue
                val key = item.key("pager")
                if (preloadedKeys.add(key)) {
                    when (item) {
                        is ReaderUiItem.Page -> {
                            launch { item.page.chapter.pageLoader?.loadPage(item.page) }
                            context.imageLoader.enqueue(
                                ImageRequest.Builder(context).data(item.page).build()
                            )
                            item.extraPage?.let { extra ->
                                launch { extra.chapter.pageLoader?.loadPage(extra) }
                                context.imageLoader.enqueue(
                                    ImageRequest.Builder(context).data(extra).build()
                                )
                            }
                        }
                        is ReaderUiItem.SplitPage -> {
                            launch { item.page.chapter.pageLoader?.loadPage(item.page) }
                            context.imageLoader.enqueue(
                                ImageRequest.Builder(context).data(item.split).build()
                            )
                        }
                        is ReaderUiItem.Transition -> Unit
                    }
                }
            }
        }

        // Track active page changes and preload upcoming/previous pages with debouncing
        LaunchedEffect(pagerState, items) {
            var preloadJob: Job? = null
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { pageIndex ->
                    val item = items.getOrNull(pageIndex)
                    if (item != null) {
                        lastActiveItem = item
                        when (item) {
                            is ReaderUiItem.Page -> {
                                onPageSelected(item.page, item.extraPage != null)
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
                                onPageSelected(item.page, false)
                            }
                            is ReaderUiItem.Transition -> {
                                onTransitionSelected(item.transition)
                            }
                        }

                        // Debounced, cancellable preload window: 2 pages behind, 4 pages ahead
                        preloadJob?.cancel()
                        preloadJob = launch {
                            delay(50L)
                            val preloadStart = (pageIndex - 2).coerceAtLeast(0)
                            val preloadEnd = (pageIndex + 4).coerceAtMost(items.lastIndex)
                            for (i in preloadStart..preloadEnd) {
                                val preloadItem = items.getOrNull(i) ?: continue
                                val key = preloadItem.key("pager")
                                if (preloadedKeys.add(key)) {
                                    when (preloadItem) {
                                        is ReaderUiItem.Page -> {
                                            launch {
                                                preloadItem.page.chapter.pageLoader?.loadPage(
                                                    preloadItem.page
                                                )
                                            }
                                            context.imageLoader.enqueue(
                                                ImageRequest.Builder(context)
                                                    .data(preloadItem.page)
                                                    .build()
                                            )
                                            preloadItem.extraPage?.let { extra ->
                                                launch { extra.chapter.pageLoader?.loadPage(extra) }
                                                context.imageLoader.enqueue(
                                                    ImageRequest.Builder(context)
                                                        .data(extra)
                                                        .build()
                                                )
                                            }
                                        }
                                        is ReaderUiItem.SplitPage -> {
                                            launch {
                                                preloadItem.page.chapter.pageLoader?.loadPage(
                                                    preloadItem.page
                                                )
                                            }
                                            context.imageLoader.enqueue(
                                                ImageRequest.Builder(context)
                                                    .data(preloadItem.split)
                                                    .build()
                                            )
                                        }
                                        is ReaderUiItem.Transition -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
        }

        val density = LocalDensity.current
        val thresholdPx = with(density) { Size.huge.toPx() }

        val nestedScrollConnection =
            remember(pagerState, items, isVertical, isRtl, thresholdPx) {
                object : NestedScrollConnection {
                    var accumulatedOverscroll = 0f

                    private fun checkAndTrigger(delta: Float) {
                        val currentIndex = pagerState.currentPage
                        val currentItem = items.getOrNull(currentIndex)

                        if (currentItem is ReaderUiItem.Transition) {
                            val transition = currentItem.transition
                            val toChapter = transition.to
                            if (toChapter != null) {
                                accumulatedOverscroll += delta
                                val isTrigger =
                                    if (isRtl) {
                                        if (transition is ChapterTransition.Prev) {
                                            accumulatedOverscroll < -thresholdPx
                                        } else {
                                            accumulatedOverscroll > thresholdPx
                                        }
                                    } else {
                                        if (transition is ChapterTransition.Prev) {
                                            accumulatedOverscroll > thresholdPx
                                        } else {
                                            accumulatedOverscroll < -thresholdPx
                                        }
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

        val useAnimation = animatedTransitions
        val flingBehavior =
            if (useAnimation) {
                PagerDefaults.flingBehavior(state = pagerState)
            } else {
                PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = snap<Float>(),
                )
            }

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .nestedScroll(nestedScrollConnection)
        ) {
            if (isVertical) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    flingBehavior = flingBehavior,
                    key = { index -> items.getOrNull(index)?.key("pager") ?: "pager_null_$index" },
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
                    flingBehavior = flingBehavior,
                    key = { index -> items.getOrNull(index)?.key("pager") ?: "pager_null_$index" },
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
    item: ReaderUiItem,
    manga: MangaItem?,
    downloadManager: DownloadManager,
    onRetryTransition: (ReaderChapter) -> Unit,
) {
    when (item) {
        is ReaderUiItem.Page -> {
            PagerPageItem(
                viewer = viewer,
                page = item.page,
                extraPage = item.extraPage,
            )
        }
        is ReaderUiItem.SplitPage -> {
            PagerPageItem(
                viewer = viewer,
                page = item.page,
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
                        ViewerNavigation.NavigationRegion.MENU -> viewer.activity.toggleMenu()
                        ViewerNavigation.NavigationRegion.NEXT -> {
                            if (viewer.activity.menuVisible) viewer.activity.hideMenu()
                            viewer.moveToNext()
                        }
                        ViewerNavigation.NavigationRegion.PREV -> {
                            if (viewer.activity.menuVisible) viewer.activity.hideMenu()
                            viewer.moveToPrevious()
                        }
                        ViewerNavigation.NavigationRegion.RIGHT -> {
                            if (viewer.activity.menuVisible) viewer.activity.hideMenu()
                            viewer.moveRight()
                        }
                        ViewerNavigation.NavigationRegion.LEFT -> {
                            if (viewer.activity.menuVisible) viewer.activity.hideMenu()
                            viewer.moveLeft()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
