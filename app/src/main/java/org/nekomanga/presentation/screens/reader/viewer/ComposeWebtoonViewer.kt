package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
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
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonActiveItemResolver
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlin.math.hypot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
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
                }
                is ReaderNavCommand.SnapToPage -> lazyListState.scrollToItem(cmd.pageIndex)
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

    // 3. Resolve active item & dispatch page selections with stationary scroll guard
    LaunchedEffect(lazyListState, items, config.activeChapterId) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@snapshotFlow null
            WebtoonActiveItemResolver.resolveActiveIndex(
                visibleItems = layoutInfo.visibleItemsInfo,
                currentItems = items,
                activeChapterId = config.activeChapterId,
                viewportStartOffset = layoutInfo.viewportStartOffset,
                viewportEndOffset = layoutInfo.viewportEndOffset,
                firstVisibleIndex = lazyListState.firstVisibleItemIndex,
                firstVisibleScrollOffset = lazyListState.firstVisibleItemScrollOffset,
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { activeIndex ->
                onActiveItemChanged(activeIndex)
                val activeItem = items.getOrNull(activeIndex) ?: return@collect
                when (activeItem) {
                    is ReaderUiItem.Page -> {
                        if (
                            activeItem.page.chapter.chapter.id == config.activeChapterId ||
                                lazyListState.isScrollInProgress
                        ) {
                            onPageSelected(activeItem.page)
                        }
                        val pages = activeItem.page.chapter.pages
                        if (
                            pages != null &&
                                activeItem.page.chapter.chapter.id == config.activeChapterId
                        ) {
                            val threshold = maxOf(5, config.preloadPageAmount)
                            if (pages.size - activeItem.page.number < threshold) {
                                val nextTransition =
                                    items.firstOrNull {
                                        it is ReaderUiItem.Transition &&
                                            it.transition is ChapterTransition.Next
                                    } as? ReaderUiItem.Transition
                                nextTransition?.transition?.to?.let { nextChapter ->
                                    config.onRequestPreloadChapter?.invoke(nextChapter)
                                }
                            }
                        }
                    }
                    is ReaderUiItem.SplitPage -> {
                        if (
                            activeItem.page.chapter.chapter.id == config.activeChapterId ||
                                lazyListState.isScrollInProgress
                        ) {
                            onPageSelected(activeItem.page)
                        }
                        val pages = activeItem.page.chapter.pages
                        if (
                            pages != null &&
                                activeItem.page.chapter.chapter.id == config.activeChapterId
                        ) {
                            val threshold = maxOf(5, config.preloadPageAmount)
                            if (pages.size - activeItem.page.number < threshold) {
                                val nextTransition =
                                    items.firstOrNull {
                                        it is ReaderUiItem.Transition &&
                                            it.transition is ChapterTransition.Next
                                    } as? ReaderUiItem.Transition
                                nextTransition?.transition?.to?.let { nextChapter ->
                                    config.onRequestPreloadChapter?.invoke(nextChapter)
                                }
                            }
                        }
                    }
                    is ReaderUiItem.Transition -> {
                        onTransitionSelected(activeItem.transition)
                        val toChapter = activeItem.transition.to
                        if (toChapter != null) {
                            config.onRequestPreloadChapter?.invoke(toChapter)
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

    val context = LocalContext.current
    val viewConfiguration = remember(context) { ViewConfiguration.get(context) }
    val touchSlopPx = remember(viewConfiguration) { viewConfiguration.scaledTouchSlop.toDouble() }
    val doubleTapSlopPx =
        remember(viewConfiguration) { viewConfiguration.scaledDoubleTapSlop.toDouble() }
    val doubleTapTimeoutMs = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }

    val density = LocalDensity.current
    val overscrollThresholdPx = remember(density) { with(density) { (Size.huge * 2).toPx() } }
    val nestedScrollConnection =
        remember(items, config.onNavigateToChapter) {
            object : NestedScrollConnection {
                var pullDownOffset = 0f
                var pullUpOffset = 0f

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (pullDownOffset > 0 && available.y < 0) {
                        val consumedY = available.y.coerceAtLeast(-pullDownOffset)
                        pullDownOffset += consumedY
                        return Offset(0f, consumedY)
                    }
                    if (pullUpOffset > 0 && available.y > 0) {
                        val consumedY = available.y.coerceAtMost(pullUpOffset)
                        pullUpOffset -= consumedY
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        // Top overscroll (pulling down at the top)
                        if (
                            available.y > 0 &&
                                lazyListState.firstVisibleItemIndex == 0 &&
                                lazyListState.firstVisibleItemScrollOffset == 0
                        ) {
                            pullDownOffset += available.y
                            return Offset(0f, available.y)
                        }
                        // Bottom overscroll (pulling up at the bottom)
                        val layoutInfo = lazyListState.layoutInfo
                        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                        if (
                            available.y < 0 &&
                                lastVisibleItem != null &&
                                lastVisibleItem.index == items.lastIndex &&
                                (lastVisibleItem.offset + lastVisibleItem.size) <=
                                    layoutInfo.viewportEndOffset
                        ) {
                            pullUpOffset += -available.y
                            return Offset(0f, available.y)
                        }
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val downOffset = pullDownOffset
                    pullDownOffset = 0f
                    if (downOffset > overscrollThresholdPx) {
                        val prevChapter =
                            (items.firstOrNull {
                                    it is ReaderUiItem.Transition &&
                                        it.transition is ChapterTransition.Prev
                                } as? ReaderUiItem.Transition)
                                ?.transition
                                ?.to
                                ?.chapter
                                ?: (items.firstOrNull() as? ReaderUiItem.Page)
                                    ?.page
                                    ?.chapter
                                    ?.chapter
                        if (prevChapter != null) {
                            config.onNavigateToChapter?.invoke(prevChapter, ChapterNavTarget.End)
                            return available
                        }
                    }

                    val upOffset = pullUpOffset
                    pullUpOffset = 0f
                    if (upOffset > overscrollThresholdPx) {
                        val nextChapter =
                            (items.firstOrNull {
                                    it is ReaderUiItem.Transition &&
                                        it.transition is ChapterTransition.Next
                                } as? ReaderUiItem.Transition)
                                ?.transition
                                ?.to
                                ?.chapter
                                ?: (items.lastOrNull() as? ReaderUiItem.Page)
                                    ?.page
                                    ?.chapter
                                    ?.chapter
                        if (nextChapter != null) {
                            config.onNavigateToChapter?.invoke(nextChapter, ChapterNavTarget.Start)
                            return available
                        }
                    }

                    return Velocity.Zero
                }
            }
        }

    // 4. Declarative Render Tree
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().background(config.backgroundColor).clipToBounds(),
    ) {
        val horizontalPadding = maxWidth * config.sidePaddingPercent
        val columnHeight = if (zoomState.scale < 1f) maxHeight / zoomState.scale else maxHeight

        LazyColumn(
            state = lazyListState,
            contentPadding = config.contentPadding,
            verticalArrangement =
                Arrangement.spacedBy(if (config.hasGaps) Size.medium else Size.none),
            modifier =
                (if (zoomState.scale < 1f) {
                        Modifier.fillMaxWidth().requiredHeight(columnHeight)
                    } else {
                        Modifier.fillMaxSize()
                    })
                    .nestedScroll(nestedScrollConnection)
                    .webtoonZoomable(
                        state = zoomState,
                        enableZoomOut = config.enableZoomOut,
                        coroutineScope = coroutineScope,
                    )
                    .pointerInput(config.navigator) {
                        var lastTapTime = 0L
                        var lastTapOffset = Offset.Zero

                        awaitEachGesture {
                            val down =
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                            val downPos = down.position
                            var pointerUp: PointerInputChange? = null

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    pointerUp = change
                                    break
                                }
                                val moveDistance =
                                    hypot(
                                        (change.position.x - downPos.x).toDouble(),
                                        (change.position.y - downPos.y).toDouble(),
                                    ) * zoomState.scale
                                if (moveDistance > touchSlopPx) break
                            }

                            if (pointerUp != null) {
                                val up = pointerUp!!
                                val upPos = up.position
                                val upTime = System.currentTimeMillis()
                                val distance =
                                    hypot(
                                        (upPos.x - downPos.x).toDouble(),
                                        (upPos.y - downPos.y).toDouble(),
                                    )

                                if (distance < touchSlopPx) {
                                    val screenWidth = size.width.toFloat()
                                    val screenHeight = size.height.toFloat()

                                    if (screenWidth > 0 && screenHeight > 0) {
                                        val pos =
                                            PointF(upPos.x / screenWidth, upPos.y / screenHeight)
                                        val action = config.navigator.getAction(pos)

                                        val isDoubleTap =
                                            (upTime - lastTapTime < doubleTapTimeoutMs) &&
                                                (hypot(
                                                    (upPos.x - lastTapOffset.x).toDouble(),
                                                    (upPos.y - lastTapOffset.y).toDouble(),
                                                ) < doubleTapSlopPx) &&
                                                (config.doubleTapAnimDuration > 0)

                                        if (isDoubleTap) {
                                            if (config.menuVisible) config.onToggleMenu()
                                            lastTapTime = 0L
                                            lastTapOffset = Offset.Zero
                                            coroutineScope.launch {
                                                zoomState.toggleDoubleTapZoom(
                                                    tapPos = upPos,
                                                    viewportWidth = size.width.toFloat(),
                                                    animDuration = config.doubleTapAnimDuration,
                                                )
                                            }
                                        } else {
                                            lastTapTime = upTime
                                            lastTapOffset = upPos
                                            when (action) {
                                                ViewerNavigation.NavigationRegion.MENU ->
                                                    config.onToggleMenu()
                                                ViewerNavigation.NavigationRegion.NEXT,
                                                ViewerNavigation.NavigationRegion.RIGHT ->
                                                    onNavigateAdjacent(true)
                                                ViewerNavigation.NavigationRegion.PREV,
                                                ViewerNavigation.NavigationRegion.LEFT ->
                                                    onNavigateAdjacent(false)
                                            }
                                        }
                                    } else {
                                        config.onToggleMenu()
                                    }
                                }
                            }
                        }
                    },
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
                        ReaderTransitionPage(
                            transition = item.transition,
                            manga = config.manga,
                            downloadManager = config.downloadManager ?: Injekt.get(),
                            onRetry = config.onRetryTransition,
                            onTap = { config.onToggleMenu() },
                            onCardClick = {
                                val targetChapter = item.transition.to?.chapter
                                if (targetChapter != null) {
                                    val navTarget =
                                        if (item.transition is ChapterTransition.Prev) {
                                            ChapterNavTarget.End
                                        } else {
                                            ChapterNavTarget.Start
                                        }
                                    config.onNavigateToChapter?.invoke(targetChapter, navTarget)
                                }
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .defaultMinSize(minHeight = columnHeight / 2)
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

        if (horizontalPadding > Size.none) {
            Box(
                modifier =
                    Modifier.align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(horizontalPadding)
                        .background(config.backgroundColor)
            )
            Box(
                modifier =
                    Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(horizontalPadding)
                        .background(config.backgroundColor)
            )
        }
    }
}

/**
 * Compatibility overload bridging legacy [WebtoonViewer] to the stateless [ComposeWebtoonViewer].
 */
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
    val readerTheme by readerPreferences.readerTheme().collectAsState()
    val webtoonSidePadding by readerPreferences.webtoonSidePadding().collectAsState()
    val animatedTransitions by readerPreferences.animatedPageTransitionsWebtoon().collectAsState()
    val disableGaps by readerPreferences.webtoonDisableGaps().collectAsState()
    val enableZoomOut by readerPreferences.webtoonEnableZoomOut().collectAsState()
    val preloadPageAmount by readerPreferences.preloadPageAmount().collectAsState()
    val themeBackground = MaterialTheme.colorScheme.background
    val backgroundColor =
        remember(readerTheme, themeBackground) {
            ReaderTheme.fromPreference(readerTheme).color(themeBackground)
        }

    val sidePaddingPercent =
        remember(webtoonSidePadding) {
            when (webtoonSidePadding) {
                1 -> 0.05f
                2 -> 0.10f
                3 -> 0.15f
                4 -> 0.20f
                5 -> 0.25f
                else -> 0f
            }
        }

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

    ComposeWebtoonViewer(
        items = items,
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
