package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.domain.ResolveChapterTransitionUiModelUseCase
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPreloadController
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapterTransitionState
import eu.kanade.tachiyomi.ui.reader.model.ReaderNavCommand
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.isEquivalentTo
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerScrollAnchorResolver
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsStateWithLifecycle
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Pure stateless Jetpack Compose viewer for paginated reading (horizontal LTR/RTL or vertical).
 * Decoupled from legacy View models, DownloadManager, and Service Locators.
 */
@Composable
fun ComposePagerViewer(
    items: List<ReaderUiItem>,
    config: PagerViewerConfigUiModel,
    onActiveItemChanged: (Int) -> Unit,
    onPageSelected: (ReaderPage, Boolean) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    modifier: Modifier = Modifier,
    navCommands: Flow<ReaderNavCommand>? = null,
    isNavigating: Boolean = false,
) {
    val pagerState =
        rememberPagerState(
            initialPage = config.initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            pageCount = { items.size },
        )

    var lastActiveItem by remember { mutableStateOf(items.getOrNull(config.initialIndex)) }
    var lastProcessedItems by remember { mutableStateOf(items) }

    val currentItems by rememberUpdatedState(items)
    val currentConfig by rememberUpdatedState(config)
    val currentIsNavigating by rememberUpdatedState(isNavigating)

    // 1. Immediate pre-measure re-anchor during composition to eliminate 1-frame flashes
    if (items !== lastProcessedItems) {
        val target =
            PagerScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastActiveItem = lastActiveItem,
                currentVisibleIndex = pagerState.currentPage,
                previousItems = lastProcessedItems,
            )
        lastProcessedItems = items
        if (target != null && target.index != pagerState.currentPage) {
            pagerState.requestScrollToPage(target.index)
            lastActiveItem = target.item
        }
    }

    // 2. Fallback post-composition anchor sync
    LaunchedEffect(items) {
        val currentItem = items.getOrNull(pagerState.currentPage)
        val activeItem = lastActiveItem
        if (currentItem != null && activeItem != null && !currentItem.isEquivalentTo(activeItem)) {
            val target =
                PagerScrollAnchorResolver.resolveReanchorTarget(
                    items = items,
                    lastActiveItem = lastActiveItem,
                    currentVisibleIndex = pagerState.currentPage,
                    previousItems = lastProcessedItems,
                )
            if (target != null && target.index != pagerState.currentPage) {
                pagerState.scrollToPage(target.index)
                lastActiveItem = target.item
            }
        }
    }

    // 3. Consume unidirectional programmatic navigation commands
    LaunchedEffect(navCommands) {
        navCommands?.collect { command ->
            when (command) {
                is ReaderNavCommand.ScrollToPage -> {
                    val target =
                        if (command.pageIndex in items.indices) {
                            command.pageIndex
                        } else {
                            items
                                .indexOfFirst {
                                    it is ReaderUiItem.Page &&
                                        it.page.chapter.chapter.id == config.activeChapterId &&
                                        (it.page.index == command.pageIndex ||
                                            it.extraPage?.index == command.pageIndex)
                                }
                                .takeIf { it != -1 } ?: command.pageIndex
                        }
                    if (target in items.indices && pagerState.currentPage != target) {
                        if (command.animated && config.animatedTransitions) {
                            pagerState.animateScrollToPage(
                                page = target,
                                animationSpec =
                                    tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            )
                        } else {
                            pagerState.scrollToPage(target)
                        }
                    }
                }
                is ReaderNavCommand.SnapToPage -> {
                    val target =
                        if (command.pageIndex in items.indices) {
                            command.pageIndex
                        } else {
                            items
                                .indexOfFirst {
                                    it is ReaderUiItem.Page &&
                                        it.page.chapter.chapter.id == config.activeChapterId &&
                                        (it.page.index == command.pageIndex ||
                                            it.extraPage?.index == command.pageIndex)
                                }
                                .takeIf { it != -1 } ?: command.pageIndex
                        }
                    if (target in items.indices && pagerState.currentPage != target) {
                        pagerState.scrollToPage(target)
                    }
                }
                is ReaderNavCommand.StepPage -> {
                    val step =
                        if (config.isRtl && !config.isVertical) {
                            if (command.forward) -1 else 1
                        } else {
                            if (command.forward) 1 else -1
                        }
                    val target = pagerState.currentPage + step
                    if (target in items.indices) {
                        if (config.animatedTransitions) {
                            pagerState.animateScrollToPage(
                                page = target,
                                animationSpec =
                                    tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            )
                        } else {
                            pagerState.scrollToPage(target)
                        }
                    }
                }
                is ReaderNavCommand.ScrollByDelta -> {}
            }
        }
    }

    // 4. Track active page changes and dispatch selections
    LaunchedEffect(pagerState, items) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val item = currentItems.getOrNull(pageIndex) ?: return@collect
                lastActiveItem = item
                onActiveItemChanged(pageIndex)

                when (item) {
                    is ReaderUiItem.Page -> {
                        onPageSelected(item.page, item.extraPage != null)
                        val pages = item.page.chapter.pages
                        if (
                            pages != null && item.page.chapter.chapter.id == config.activeChapterId
                        ) {
                            val threshold = maxOf(5, config.preloadPageAmount)
                            if (pages.size - item.page.number < threshold) {
                                val nextTransition =
                                    currentItems.firstOrNull {
                                        it is ReaderUiItem.Transition &&
                                            it.transition is ChapterTransition.Next
                                    } as? ReaderUiItem.Transition
                                nextTransition?.transition?.to?.let {
                                    config.onRequestPreloadChapter?.invoke(it)
                                }
                            }
                            if (item.page.number <= threshold) {
                                val prevTransition =
                                    currentItems.firstOrNull {
                                        it is ReaderUiItem.Transition &&
                                            it.transition is ChapterTransition.Prev
                                    } as? ReaderUiItem.Transition
                                prevTransition?.transition?.to?.let {
                                    config.onRequestPreloadChapter?.invoke(it)
                                }
                            }
                        }
                    }
                    is ReaderUiItem.SplitPage -> onPageSelected(item.page, false)
                    is ReaderUiItem.Transition -> onTransitionSelected(item.transition)
                }
            }
    }

    // 5. Eagerly preload adjacent chapters when items update
    LaunchedEffect(config.activeChapterId, items) {
        val nextTransition =
            items.firstOrNull {
                it is ReaderUiItem.Transition && it.transition is ChapterTransition.Next
            } as? ReaderUiItem.Transition
        nextTransition?.transition?.to?.let { nextChapter ->
            config.onRequestPreloadChapter?.invoke(nextChapter)
        }
        val prevTransition =
            items.firstOrNull {
                it is ReaderUiItem.Transition && it.transition is ChapterTransition.Prev
            } as? ReaderUiItem.Transition
        prevTransition?.transition?.to?.let { prevChapter ->
            config.onRequestPreloadChapter?.invoke(prevChapter)
        }
    }

    val density = LocalDensity.current
    val thresholdPx = with(density) { Size.huge.toPx() }

    val flingBehavior =
        if (config.animatedTransitions) {
            PagerDefaults.flingBehavior(state = pagerState)
        } else {
            PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = snap(),
            )
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(config.backgroundColor)
                .pagerOverscrollNavigation(
                    pagerState = pagerState,
                    isRtl = config.isRtl,
                    isVertical = config.isVertical,
                    items = items,
                    thresholdPx = thresholdPx,
                    isNavigating = currentIsNavigating,
                    onNavigateToChapter = { ch, target ->
                        config.onNavigateToChapter?.invoke(ch, target)
                    },
                )
    ) {
        if (config.isVertical) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                flingBehavior = flingBehavior,
                key = { index -> items.getOrNull(index)?.key("pager") ?: "pager_null_$index" },
            ) { index ->
                val item = items.getOrNull(index) ?: return@VerticalPager
                PagerItemContent(item = item, config = currentConfig)
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
                PagerItemContent(item = item, config = currentConfig)
            }
        }
    }
}

@Composable
private fun PagerItemContent(
    item: ReaderUiItem,
    config: PagerViewerConfigUiModel,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is ReaderUiItem.Page -> {
            PagerPageItem(
                page = item.page,
                config = config,
                extraPage = item.extraPage,
                modifier = modifier,
            )
        }
        is ReaderUiItem.SplitPage -> {
            PagerPageItem(
                page = item.page,
                config = config,
                modifier = modifier,
            )
        }
        is ReaderUiItem.Transition -> {
            val uiModel = item.transitionUiModel
            val downloadManager = config.downloadManager
            if (uiModel != null) {
                ReaderTransitionPage(
                    uiModel = uiModel,
                    onRetry = { item.transition.to?.let { config.onRetryTransition(it) } },
                    onTap = { pos: PointF ->
                        val navigator = config.navigator
                        when (navigator.getAction(pos)) {
                            ViewerNavigation.NavigationRegion.MENU -> config.onToggleMenu()
                            ViewerNavigation.NavigationRegion.NEXT -> {
                                if (config.menuVisible) config.onToggleMenu()
                                config.onNavigateAdjacent(true)
                            }
                            ViewerNavigation.NavigationRegion.PREV -> {
                                if (config.menuVisible) config.onToggleMenu()
                                config.onNavigateAdjacent(false)
                            }
                            ViewerNavigation.NavigationRegion.RIGHT -> {
                                if (config.menuVisible) config.onToggleMenu()
                                if (config.isRtl) {
                                    config.onNavigateAdjacent(false)
                                } else {
                                    config.onNavigateAdjacent(true)
                                }
                            }
                            ViewerNavigation.NavigationRegion.LEFT -> {
                                if (config.menuVisible) config.onToggleMenu()
                                if (config.isRtl) {
                                    config.onNavigateAdjacent(true)
                                } else {
                                    config.onNavigateAdjacent(false)
                                }
                            }
                        }
                    },
                    modifier = modifier.fillMaxSize(),
                )
            } else if (downloadManager != null) {
                ReaderTransitionPage(
                    transition = item.transition,
                    manga = config.manga,
                    downloadManager = downloadManager,
                    onRetry = config.onRetryTransition,
                    onTap = { pos: PointF ->
                        val navigator = config.navigator
                        when (navigator.getAction(pos)) {
                            ViewerNavigation.NavigationRegion.MENU -> config.onToggleMenu()
                            ViewerNavigation.NavigationRegion.NEXT -> {
                                if (config.menuVisible) config.onToggleMenu()
                                config.onNavigateAdjacent(true)
                            }
                            ViewerNavigation.NavigationRegion.PREV -> {
                                if (config.menuVisible) config.onToggleMenu()
                                config.onNavigateAdjacent(false)
                            }
                            ViewerNavigation.NavigationRegion.RIGHT -> {
                                if (config.menuVisible) config.onToggleMenu()
                                if (config.isRtl) {
                                    config.onNavigateAdjacent(false)
                                } else {
                                    config.onNavigateAdjacent(true)
                                }
                            }
                            ViewerNavigation.NavigationRegion.LEFT -> {
                                if (config.menuVisible) config.onToggleMenu()
                                if (config.isRtl) {
                                    config.onNavigateAdjacent(true)
                                } else {
                                    config.onNavigateAdjacent(false)
                                }
                            }
                        }
                    },
                    modifier = modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Legacy compatibility overload for [ComposePagerViewer] integrating [PagerViewer]. */
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
    onNavigateToChapter: (Chapter, ChapterNavTarget) -> Unit,
    onRequestPreloadChapter: (ReaderChapter) -> Unit,
    onRetryTransition: (ReaderChapter) -> Unit,
    modifier: Modifier = Modifier,
    transitionState: ReaderChapterTransitionState = ReaderChapterTransitionState.Idle,
    navCommands: Flow<ReaderNavCommand>? = null,
    preloadController: ReaderPreloadController? = null,
) {
    val currentChapterId =
        (viewer.currentChapter
                ?: items
                    .firstOrNull { it is ReaderUiItem.Page }
                    ?.let { (it as ReaderUiItem.Page).page.chapter })
            ?.chapter
            ?.id

    val currentChapter = viewer.currentChapter
    val defaultPageIndex =
        remember(items, currentChapterId, currentChapter?.requestedPage) {
            if (currentChapter != null && currentChapter.requestedPage > 0) {
                items
                    .indexOfFirst { item ->
                        item is ReaderUiItem.Page &&
                            item.page.chapter.chapter.id == currentChapterId &&
                            (item.page.index == currentChapter.requestedPage ||
                                item.extraPage?.index == currentChapter.requestedPage)
                    }
                    .takeIf { it != -1 }
            } else {
                null
            }
                ?: items
                    .indexOfFirst { item ->
                        item is ReaderUiItem.Page &&
                            item.page.chapter.chapter.id == currentChapterId &&
                            (item.page.index == 0 || item.extraPage?.index == 0)
                    }
                    .takeIf { it != -1 }
                ?: run {
                    var minPageIndex = Int.MAX_VALUE
                    var targetItemIndex = -1
                    for (i in items.indices) {
                        val item = items[i]
                        if (
                            item is ReaderUiItem.Page &&
                                item.page.chapter.chapter.id == currentChapterId
                        ) {
                            val pageMin =
                                minOf(item.page.index, item.extraPage?.index ?: Int.MAX_VALUE)
                            if (pageMin < minPageIndex) {
                                minPageIndex = pageMin
                                targetItemIndex = i
                            }
                        }
                    }
                    targetItemIndex.takeIf { it != -1 }
                }
                ?: items.indexOfFirst { it is ReaderUiItem.Page }.takeIf { it != -1 }
                ?: 0
        }

    val initialPage =
        (viewer.requestedPagePosition?.first ?: defaultPageIndex).coerceIn(
            0,
            (items.size - 1).coerceAtLeast(0),
        )

    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val animatedTransitions by
        readerPreferences.animatedPageTransitions().collectAsStateWithLifecycle()
    val imageScaleType by readerPreferences.imageScaleType().collectAsStateWithLifecycle()
    val doublePageGap by readerPreferences.doublePageGap().collectAsStateWithLifecycle()
    val invertDoublePages by readerPreferences.invertDoublePages().collectAsStateWithLifecycle()
    val readerTheme by readerPreferences.readerTheme().collectAsStateWithLifecycle()
    val landscapeZoom by readerPreferences.landscapeZoom().collectAsStateWithLifecycle()
    val zoomStart by readerPreferences.zoomStart().collectAsStateWithLifecycle()
    val preloadPageAmount by readerPreferences.preloadPageAmount().collectAsStateWithLifecycle()

    val themeBackground = MaterialTheme.colorScheme.background
    val backgroundColor =
        remember(readerTheme, themeBackground) {
            ReaderTheme.fromPreference(readerTheme).color(themeBackground)
        }

    val config =
        PagerViewerConfigUiModel(
            initialIndex = initialPage,
            activeChapterId = currentChapterId,
            backgroundColor = backgroundColor,
            isRtl = isRtl,
            isVertical = isVertical,
            animatedTransitions = animatedTransitions,
            imageScaleType = imageScaleType,
            doublePages = viewer.config.doublePages,
            shiftDoublePage = viewer.config.shiftDoublePage,
            invertDoublePages = invertDoublePages,
            doublePageGap = doublePageGap,
            doublePageRotate = viewer.config.doublePageRotate,
            doublePageRotateReverse = viewer.config.doublePageRotateReverse,
            zoomStart = zoomStart,
            landscapeZoom = landscapeZoom,
            doubleTapAnimDuration = viewer.config.doubleTapAnimDuration,
            longTapEnabled = viewer.config.longTapEnabled,
            menuVisible = viewer.activity.menuVisible,
            navigator = viewer.config.navigator,
            preloadPageAmount = preloadPageAmount,
            onToggleMenu = remember(viewer) { { viewer.activity.toggleMenu() } },
            onNavigateAdjacent =
                remember(viewer) {
                    { forward -> if (forward) viewer.moveToNext() else viewer.moveToPrevious() }
                },
            onRetryTransition = onRetryTransition,
            onNavigateToChapter = onNavigateToChapter,
            onRequestPreloadChapter = onRequestPreloadChapter,
            onPageLongTap = remember(viewer) { { p, ep -> viewer.activity.onPageLongTap(p, ep) } },
            onWidePageDetected = remember(viewer) { { page -> viewer.splitDoublePages(page) } },
            manga = manga,
            downloadManager = downloadManager,
        )

    val navChannel = remember { Channel<ReaderNavCommand>(Channel.BUFFERED) }

    LaunchedEffect(viewer.requestedPagePosition) {
        val req = viewer.requestedPagePosition ?: return@LaunchedEffect
        navChannel.send(ReaderNavCommand.ScrollToPage(req.first, req.second))
        viewer.requestedPagePosition = null
    }

    val transitionResolver =
        remember(downloadManager) { ResolveChapterTransitionUiModelUseCase(downloadManager) }
    val enrichedItems =
        remember(items, manga, transitionResolver) {
            items.map { item ->
                if (item is ReaderUiItem.Transition && item.transitionUiModel == null) {
                    item.copy(transitionUiModel = transitionResolver(item.transition, manga))
                } else {
                    item
                }
            }
        }

    val effectiveNavCommands =
        remember(navCommands) {
            if (navCommands != null) {
                merge(navChannel.receiveAsFlow(), navCommands)
            } else {
                navChannel.receiveAsFlow()
            }
        }

    val effectivePreloadController = preloadController

    LaunchedEffect(enrichedItems, preloadPageAmount, isRtl, effectivePreloadController) {
        effectivePreloadController?.onPositionChanged(
            currentIndex = initialPage,
            items = enrichedItems,
            preloadAmount = preloadPageAmount,
            isRtl = isRtl,
            isWebtoon = false,
        )
    }

    val isNavigating =
        transitionState is ReaderChapterTransitionState.Loading ||
            transitionState is ReaderChapterTransitionState.Settling

    ComposePagerViewer(
        items = enrichedItems,
        config = config,
        onActiveItemChanged = { activeIndex ->
            viewer.currentPagePosition = activeIndex
            effectivePreloadController?.onPositionChanged(
                currentIndex = activeIndex,
                items = enrichedItems,
                preloadAmount = preloadPageAmount,
                isRtl = isRtl,
                isWebtoon = false,
            )
        },
        onPageSelected = onPageSelected,
        onTransitionSelected = onTransitionSelected,
        modifier = modifier,
        navCommands = effectiveNavCommands,
        isNavigating = isNavigating,
    )
}
