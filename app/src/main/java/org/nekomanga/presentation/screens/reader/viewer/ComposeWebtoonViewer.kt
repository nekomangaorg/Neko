package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.domain.ResolveChapterTransitionUiModelUseCase
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderNavCommand
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.areTransitionsEquivalent as modelAreTransitionsEquivalent
import eu.kanade.tachiyomi.ui.reader.model.isEquivalentTo
import eu.kanade.tachiyomi.ui.reader.model.isSameChapter as modelIsSameChapter
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonActiveItemResolver
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonScrollAnchorResolver
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsStateWithLifecycle
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Pure stateless Jetpack Compose viewer for continuous Webtoon vertical reading. Decoupled from
 * legacy View models, DownloadManager, and Service Locators.
 */
@Composable
fun ComposeWebtoonViewer(
    items: List<ReaderUiItem>,
    config: WebtoonViewerConfigUiModel,
    navCommands: Flow<ReaderNavCommand>,
    onActiveItemChanged: (Int) -> Unit,
    onPageSelected: (ReaderPage) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onPageLongTap: (ReaderPage) -> Unit,
    onNavigateAdjacent: (forward: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = config.initialIndex)
    val zoomState = rememberWebtoonZoomState()
    val coroutineScope = rememberCoroutineScope()

    val currentItems by rememberUpdatedState(items)
    val activeChapterId by rememberUpdatedState(config.activeChapterId)
    val currentConfig by rememberUpdatedState(config)
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)
    val currentOnTransitionSelected by rememberUpdatedState(onTransitionSelected)
    val currentOnActiveItemChanged by rememberUpdatedState(onActiveItemChanged)

    var lastFirstVisibleItem by remember { mutableStateOf(items.getOrNull(config.initialIndex)) }
    var lastFirstVisibleOffset by remember { mutableIntStateOf(0) }
    var lastActiveItem by remember { mutableStateOf<ReaderUiItem?>(null) }
    var lastProcessedItems by remember { mutableStateOf(items) }

    // 1. Consume unidirectional programmatic navigation commands
    LaunchedEffect(navCommands) {
        navCommands.collect { cmd ->
            when (cmd) {
                is ReaderNavCommand.ScrollToPage -> {
                    if (cmd.animated && config.animatedTransitions) {
                        lazyListState.animateScrollToItem(cmd.pageIndex)
                    } else {
                        lazyListState.scrollToItem(cmd.pageIndex)
                    }
                    currentItems.getOrNull(cmd.pageIndex)?.let {
                        lastFirstVisibleItem = it
                        lastFirstVisibleOffset = 0
                    }
                }
                is ReaderNavCommand.SnapToPage -> {
                    lazyListState.scrollToItem(cmd.pageIndex)
                    currentItems.getOrNull(cmd.pageIndex)?.let {
                        lastFirstVisibleItem = it
                        lastFirstVisibleOffset = 0
                    }
                }
                is ReaderNavCommand.ScrollByDelta -> {
                    val scrollAmount =
                        if (zoomState.scale > 0f) cmd.delta / zoomState.scale else cmd.delta
                    if (config.animatedTransitions) {
                        lazyListState.animateScrollBy(scrollAmount)
                    } else {
                        lazyListState.scrollBy(scrollAmount)
                    }
                }
            }
        }
    }

    // 2. Eagerly preload next chapter when chapter or items update
    LaunchedEffect(config.activeChapterId, items) {
        val nextTransition =
            items.firstOrNull {
                it is ReaderUiItem.Transition && it.transition is ChapterTransition.Next
            } as? ReaderUiItem.Transition
        nextTransition?.transition?.to?.let { nextChapter ->
            config.onRequestPreloadChapter?.invoke(nextChapter)
        }
    }

    // 3. Maintain scroll anchor across item mutations, prepends, splits, and chapter transitions
    if (items !== lastProcessedItems) {
        val target =
            WebtoonScrollAnchorResolver.resolveReanchorTarget(
                items = items,
                lastFirstVisibleItem = lastFirstVisibleItem,
                lastFirstVisibleOffset = lastFirstVisibleOffset,
                lastActiveItem = lastActiveItem,
                activeChapterId = activeChapterId,
                currentFirstVisibleIndex = lazyListState.firstVisibleItemIndex,
                previousItems = lastProcessedItems,
            )
        if (target != null) {
            val currentFirstItem = lastProcessedItems.getOrNull(lazyListState.firstVisibleItemIndex)
            val shouldScroll =
                target.index != lazyListState.firstVisibleItemIndex ||
                    (currentFirstItem?.let { it::class != target.item::class } == true &&
                        target.offset != lazyListState.firstVisibleItemScrollOffset)
            if (shouldScroll) {
                val liveOffset =
                    if (currentFirstItem?.isEquivalentTo(target.item) == true) {
                        lazyListState.firstVisibleItemScrollOffset
                    } else {
                        target.offset
                    }
                lazyListState.requestScrollToItem(target.index, liveOffset)
                lastFirstVisibleItem = target.item
                lastFirstVisibleOffset = liveOffset
            }
        }
    }

    SideEffect { lastProcessedItems = items }

    LaunchedEffect(items) {
        val currentFirstItem = items.getOrNull(lazyListState.firstVisibleItemIndex)
        val expectedItem = lastFirstVisibleItem
        if (expectedItem != null && currentFirstItem?.isEquivalentTo(expectedItem) != true) {
            val targetIndex = items.indexOfFirst { it.isEquivalentTo(expectedItem) }
            if (targetIndex != -1 && targetIndex != lazyListState.firstVisibleItemIndex) {
                lazyListState.scrollToItem(targetIndex, lastFirstVisibleOffset)
            }
        }
    }

    // 4. Resolve active item & dispatch page selections with stationary scroll guard
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            if (currentItems !== lastProcessedItems) return@snapshotFlow null
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow null
            if (layoutInfo.totalItemsCount != currentItems.size) return@snapshotFlow null
            val isOutOfSync = visibleItems.any { info ->
                info.index !in currentItems.indices ||
                    currentItems[info.index].key("webtoon") != info.key
            }
            if (isOutOfSync) return@snapshotFlow null

            val activeIndex =
                WebtoonActiveItemResolver.resolveActiveIndex(
                    visibleItems = visibleItems,
                    currentItems = currentItems,
                    activeChapterId = activeChapterId,
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                    firstVisibleIndex = lazyListState.firstVisibleItemIndex,
                    firstVisibleScrollOffset = lazyListState.firstVisibleItemScrollOffset,
                )
            val firstVisibleItem = currentItems.getOrNull(lazyListState.firstVisibleItemIndex)
            val firstVisibleOffset = lazyListState.firstVisibleItemScrollOffset
            Triple(
                activeIndex,
                currentItems.getOrNull(activeIndex),
                firstVisibleItem to firstVisibleOffset,
            )
        }
            .filterNotNull()
            .distinctUntilChanged { old, new ->
                old.first == new.first &&
                    old.third.first?.key("webtoon") == new.third.first?.key("webtoon") &&
                    old.third.second == new.third.second
            }
            .collect { (activeIndex, item, firstVisiblePair) ->
                val (firstVisibleItem, firstVisibleOffset) = firstVisiblePair
                if (firstVisibleItem != null) {
                    lastFirstVisibleItem = firstVisibleItem
                    lastFirstVisibleOffset = firstVisibleOffset
                }
                if (item != null) {
                    val activeItemChanged = lastActiveItem != item
                    lastActiveItem = item
                    if (activeItemChanged) {
                        currentOnActiveItemChanged(activeIndex)
                        when (item) {
                            is ReaderUiItem.Page -> {
                                if (
                                    item.page.chapter.chapter.id == activeChapterId ||
                                        lazyListState.isScrollInProgress
                                ) {
                                    currentOnPageSelected(item.page)
                                }
                                val pages = item.page.chapter.pages
                                if (
                                    pages != null && item.page.chapter.chapter.id == activeChapterId
                                ) {
                                    val threshold = maxOf(5, currentConfig.preloadPageAmount)
                                    if (pages.size - item.page.number < threshold) {
                                        val nextTransition =
                                            currentItems.firstOrNull {
                                                it is ReaderUiItem.Transition &&
                                                    it.transition is ChapterTransition.Next
                                            } as? ReaderUiItem.Transition
                                        nextTransition?.transition?.to?.let { nextChapter ->
                                            currentConfig.onRequestPreloadChapter?.invoke(
                                                nextChapter
                                            )
                                        }
                                    }
                                }
                            }
                            is ReaderUiItem.SplitPage -> {
                                if (
                                    item.page.chapter.chapter.id == activeChapterId ||
                                        lazyListState.isScrollInProgress
                                ) {
                                    currentOnPageSelected(item.page)
                                }
                                val pages = item.page.chapter.pages
                                if (
                                    pages != null && item.page.chapter.chapter.id == activeChapterId
                                ) {
                                    val threshold = maxOf(5, currentConfig.preloadPageAmount)
                                    if (pages.size - item.page.number < threshold) {
                                        val nextTransition =
                                            currentItems.firstOrNull {
                                                it is ReaderUiItem.Transition &&
                                                    it.transition is ChapterTransition.Next
                                            } as? ReaderUiItem.Transition
                                        nextTransition?.transition?.to?.let { nextChapter ->
                                            currentConfig.onRequestPreloadChapter?.invoke(
                                                nextChapter
                                            )
                                        }
                                    }
                                }
                            }
                            is ReaderUiItem.Transition -> {
                                currentOnTransitionSelected(item.transition)
                                val toChapter = item.transition.to
                                if (toChapter != null) {
                                    currentConfig.onRequestPreloadChapter?.invoke(toChapter)
                                }
                            }
                        }
                    }
                }
            }
    }

    LaunchedEffect(config.enableZoomOut) {
        if (!config.enableZoomOut && zoomState.scale < 1f) {
            zoomState.reset()
        }
    }

    // 5. Declarative Render Tree
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().background(config.backgroundColor).clipToBounds(),
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val effectiveContentPadding =
            calculateEffectiveContentPadding(
                sidePadding = config.sidePadding,
                sidePaddingPercent = config.sidePaddingPercent,
                maxWidth = maxWidth,
                contentPadding = config.contentPadding,
                layoutDirection = layoutDirection,
            )

        LazyColumn(
            state = lazyListState,
            contentPadding = effectiveContentPadding,
            verticalArrangement =
                Arrangement.spacedBy(if (config.hasGaps) Size.medium else Size.none),
            modifier =
                Modifier.fillMaxSize()
                    .layout { measurable, constraints ->
                        val scale = zoomState.scale
                        val targetHeight =
                            if (scale in 0.01f..0.999f) {
                                (constraints.maxHeight / scale).toInt()
                            } else {
                                constraints.maxHeight
                            }
                        val placeable =
                            measurable.measure(
                                constraints.copy(minHeight = targetHeight, maxHeight = targetHeight)
                            )
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .webtoonOverscrollNavigation(
                        lazyListState = lazyListState,
                        items = currentItems,
                        onNavigateToChapter = currentConfig.onNavigateToChapter,
                        onNavigateAdjacent = onNavigateAdjacent,
                    )
                    .webtoonZoomable(
                        state = zoomState,
                        enableZoomOut = config.enableZoomOut,
                        coroutineScope = coroutineScope,
                    )
                    .webtoonTapNavigation(
                        navigator = currentConfig.navigator,
                        zoomState = zoomState,
                        enableDoubleTapZoom = currentConfig.doubleTapAnimDuration > 0,
                        doubleTapAnimDuration = currentConfig.doubleTapAnimDuration,
                        menuVisible = currentConfig.menuVisible,
                        coroutineScope = coroutineScope,
                        onToggleMenu = currentConfig.onToggleMenu,
                        onNavigateAdjacent = onNavigateAdjacent,
                    ),
        ) {
            items(
                items = items,
                key = { it.key("webtoon") },
            ) { item ->
                when (item) {
                    is ReaderUiItem.Page -> {
                        WebtoonPageItem(
                            page = item.page,
                            backgroundColor = config.backgroundColor,
                            onLongClick = { onPageLongTap(item.page) },
                        )
                    }
                    is ReaderUiItem.SplitPage -> {
                        WebtoonPageItem(
                            split = item.split,
                            backgroundColor = config.backgroundColor,
                            onLongClick = { onPageLongTap(item.page) },
                        )
                    }
                    is ReaderUiItem.Transition -> {
                        val uiModel =
                            item.transitionUiModel ?: ChapterTransitionUiModel.from(item.transition)
                        ReaderTransitionPage(
                            uiModel = uiModel,
                            onRetry = {
                                item.transition.to?.let { currentConfig.onRetryTransition(it) }
                            },
                            onTap = { currentConfig.onToggleMenu() },
                            onCardClick = {
                                val targetChapter = item.transition.to?.chapter
                                if (targetChapter != null) {
                                    val navTarget =
                                        if (item.transition is ChapterTransition.Prev) {
                                            ChapterNavTarget.End
                                        } else {
                                            ChapterNavTarget.Start
                                        }
                                    currentConfig.onNavigateToChapter?.invoke(
                                        targetChapter,
                                        navTarget,
                                    )
                                }
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .defaultMinSize(minHeight = maxHeight / 2)
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

/**
 * Compatibility overload bridging legacy [WebtoonViewer] to the stateless [ComposeWebtoonViewer].
 */
@Deprecated("Use ComposeWebtoonViewer with WebtoonViewerConfigUiModel directly")
@Composable
fun ComposeWebtoonViewer(
    viewer: WebtoonViewer,
    items: List<ReaderUiItem>,
    manga: MangaItem?,
    downloadManager: DownloadManager,
    onPageSelected: (ReaderPage) -> Unit,
    onTransitionSelected: (ChapterTransition) -> Unit,
    onRetryTransition: (ReaderChapter) -> Unit,
    onNavigateToChapter: ((Chapter, ChapterNavTarget) -> Unit)? = null,
    onRequestPreloadChapter: ((ReaderChapter) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val currentChapterId =
        (viewer.currentChapter
                ?: items
                    .firstOrNull { it is ReaderUiItem.Page }
                    ?.let { (it as ReaderUiItem.Page).page.chapter })
            ?.chapter
            ?.id

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

    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val readerTheme by readerPreferences.readerTheme().collectAsStateWithLifecycle()
    val webtoonSidePadding by readerPreferences.webtoonSidePadding().collectAsStateWithLifecycle()
    val animatedTransitions by
        readerPreferences.animatedPageTransitionsWebtoon().collectAsStateWithLifecycle()
    val disableGaps by readerPreferences.webtoonDisableGaps().collectAsStateWithLifecycle()
    val enableZoomOut by readerPreferences.webtoonEnableZoomOut().collectAsStateWithLifecycle()
    val preloadPageAmount by readerPreferences.preloadPageAmount().collectAsStateWithLifecycle()
    val themeBackground = MaterialTheme.colorScheme.background
    val backgroundColor =
        remember(readerTheme, themeBackground) {
            ReaderTheme.fromPreference(readerTheme).color(themeBackground)
        }

    val sidePaddingPercent =
        remember(webtoonSidePadding) { (webtoonSidePadding / 100f).coerceIn(0f, 0.25f) }

    val hasMargins = viewer.hasMargins && !disableGaps

    LaunchedEffect(currentChapterId) {
        viewer.nextTransition?.to?.let {
            onRequestPreloadChapter?.invoke(it) ?: viewer.activity.requestPreloadChapter(it)
        }
    }

    val config =
        WebtoonViewerConfigUiModel(
            initialIndex = initialItemIndex,
            activeChapterId = currentChapterId,
            backgroundColor = backgroundColor,
            contentPadding = PaddingValues(bottom = if (hasMargins) Size.medium else Size.none),
            sidePaddingPercent = sidePaddingPercent,
            hasGaps = hasMargins,
            enableZoomOut = enableZoomOut,
            animatedTransitions = animatedTransitions,
            doubleTapAnimDuration = viewer.config.doubleTapAnimDuration,
            longTapEnabled = viewer.config.longTapEnabled,
            menuVisible = viewer.activity.menuVisible,
            navigator = viewer.config.navigator,
            onToggleMenu = { viewer.activity.toggleMenu() },
            onRetryTransition = onRetryTransition,
            manga = manga,
            downloadManager = downloadManager,
            preloadPageAmount = preloadPageAmount,
            onNavigateToChapter = onNavigateToChapter,
            onRequestPreloadChapter =
                onRequestPreloadChapter
                    ?: { chapter ->
                        viewer.activity.requestPreloadChapter(chapter)
                    },
        )

    val navChannel = remember { Channel<ReaderNavCommand>(Channel.BUFFERED) }

    LaunchedEffect(viewer.requestedPagePosition) {
        val req = viewer.requestedPagePosition ?: return@LaunchedEffect
        navChannel.send(ReaderNavCommand.ScrollToPage(req.targetPage, req.animated))
        viewer.requestedPagePosition = null
    }

    LaunchedEffect(viewer.requestedScrollDelta) {
        val delta = viewer.requestedScrollDelta ?: return@LaunchedEffect
        navChannel.send(ReaderNavCommand.ScrollByDelta(delta.toFloat()))
        viewer.requestedScrollDelta = null
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

    ComposeWebtoonViewer(
        items = enrichedItems,
        config = config,
        navCommands = navChannel.receiveAsFlow(),
        onActiveItemChanged = { activeIndex -> viewer.updateActiveIndex(activeIndex) },
        onPageSelected = onPageSelected,
        onTransitionSelected = onTransitionSelected,
        onPageLongTap = { page ->
            if (viewer.activity.menuVisible || viewer.config.longTapEnabled) {
                viewer.activity.onPageLongTap(page)
            }
        },
        onNavigateAdjacent = { forward ->
            if (forward) viewer.moveToNext() else viewer.moveToPrevious()
        },
        modifier = modifier,
    )
}

internal fun isSameChapter(a: ReaderChapter, b: ReaderChapter): Boolean = modelIsSameChapter(a, b)

internal fun areTransitionsEquivalent(a: ChapterTransition, b: ChapterTransition): Boolean =
    modelAreTransitionsEquivalent(a, b)

internal fun areItemsEquivalent(a: ReaderUiItem, b: ReaderUiItem): Boolean = a.isEquivalentTo(b)

internal fun calculateEffectiveContentPadding(
    sidePadding: Dp,
    sidePaddingPercent: Float,
    maxWidth: Dp,
    contentPadding: PaddingValues,
    layoutDirection: LayoutDirection,
): PaddingValues {
    val horizontalPadding =
        if (sidePadding > Size.none) {
            sidePadding
        } else {
            maxWidth * sidePaddingPercent
        }
    return PaddingValues(
        start = horizontalPadding + contentPadding.calculateStartPadding(layoutDirection),
        end = horizontalPadding + contentPadding.calculateEndPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        bottom = contentPadding.calculateBottomPadding(),
    )
}
