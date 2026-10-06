package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderNavCommand
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.areTransitionsEquivalent as modelAreTransitionsEquivalent
import eu.kanade.tachiyomi.ui.reader.model.continuesInto
import eu.kanade.tachiyomi.ui.reader.model.isEquivalentTo
import eu.kanade.tachiyomi.ui.reader.model.isSameChapter as modelIsSameChapter
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonActiveItemResolver
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonScrollAnchorResolver
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonScrollGatingPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.nekomanga.presentation.theme.Size

internal class ScrollAnchorState(
    var item: ReaderUiItem? = null,
    var offset: Int = 0,
)

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
    val tapClaim = remember { TapNavigationClaim() }

    val currentItems by rememberUpdatedState(items)
    val activeChapterId by rememberUpdatedState(config.activeChapterId)
    val currentConfig by rememberUpdatedState(config)
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)
    val currentOnTransitionSelected by rememberUpdatedState(onTransitionSelected)
    val currentOnActiveItemChanged by rememberUpdatedState(onActiveItemChanged)

    val scrollAnchorState = remember { ScrollAnchorState(items.getOrNull(config.initialIndex), 0) }
    var lastActiveItem by remember { mutableStateOf<ReaderUiItem?>(null) }
    var lastDispatchedPage by remember { mutableStateOf<ReaderPage?>(null) }
    var lastProcessedItems by remember { mutableStateOf(items) }

    // 1. Consume unidirectional programmatic navigation commands
    LaunchedEffect(navCommands) {
        navCommands.collect { cmd ->
            executeWebtoonNavCommand(
                command = cmd,
                lazyListState = lazyListState,
                items = currentItems,
                config = config,
                scrollAnchorState = scrollAnchorState,
                zoomScale = zoomState.scale,
            )
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
                lastFirstVisibleItem = scrollAnchorState.item,
                lastFirstVisibleOffset = scrollAnchorState.offset,
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
                scrollAnchorState.item = target.item
                scrollAnchorState.offset = liveOffset
            }
        }
    }

    SideEffect { lastProcessedItems = items }

    LaunchedEffect(items) {
        val currentFirstItem = items.getOrNull(lazyListState.firstVisibleItemIndex)
        val expectedItem = scrollAnchorState.item
        if (expectedItem != null && currentFirstItem?.isEquivalentTo(expectedItem) != true) {
            val targetIndex = items.indexOfFirst { it.isEquivalentTo(expectedItem) }
            if (targetIndex != -1 && targetIndex != lazyListState.firstVisibleItemIndex) {
                lazyListState.scrollToItem(targetIndex, scrollAnchorState.offset)
            }
        }
    }

    // 4. Track first visible item and offset for scroll anchor preservation
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }
            .collect { (firstIndex, firstOffset) ->
                currentItems.getOrNull(firstIndex)?.let { scrollAnchorState.item = it }
                scrollAnchorState.offset = firstOffset
            }
    }

    // 5. Resolve active item & dispatch page selections with stationary scroll guard
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
            activeIndex to currentItems.getOrNull(activeIndex)
        }
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.first == new.first && old.second == new.second }
            .collect { (activeIndex, item) ->
                if (item != null) {
                    val activeItemChanged = lastActiveItem != item
                    lastActiveItem = item
                    if (activeItemChanged) {
                        currentOnActiveItemChanged(activeIndex)

                        val currentPage =
                            when (item) {
                                is ReaderUiItem.Page -> item.page
                                is ReaderUiItem.SplitPage -> item.page
                                is ReaderUiItem.Transition -> null
                            }

                        if (currentPage != null) {
                            val shouldDispatch =
                                WebtoonScrollGatingPolicy.shouldDispatchPageSelection(
                                    activeChapterId = activeChapterId,
                                    candidateChapterId = currentPage.chapter.chapter.id,
                                    isScrollInProgress = lazyListState.isScrollInProgress,
                                )
                            if (shouldDispatch && currentPage != lastDispatchedPage) {
                                lastDispatchedPage = currentPage
                                currentOnPageSelected(currentPage)
                            }
                            // Trigger adjacent chapter preloading near end of chapter
                            val pages = currentPage.chapter.pages
                            if (
                                pages != null && currentPage.chapter.chapter.id == activeChapterId
                            ) {
                                val threshold = maxOf(5, currentConfig.preloadPageAmount)
                                if (pages.size - currentPage.number < threshold) {
                                    val nextTransition =
                                        currentItems.firstOrNull {
                                            it is ReaderUiItem.Transition &&
                                                it.transition is ChapterTransition.Next
                                        } as? ReaderUiItem.Transition
                                    nextTransition?.transition?.to?.let { nextChapter ->
                                        currentConfig.onRequestPreloadChapter?.invoke(nextChapter)
                                    }
                                }
                            }
                        } else if (item is ReaderUiItem.Transition) {
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
        val entries = remember(items) { items.toWebtoonListEntries() }
        val pageGapModifier = remember { Modifier.padding(bottom = Size.medium) }

        LazyColumn(
            state = lazyListState,
            contentPadding = effectiveContentPadding,
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
                        tapClaim = tapClaim,
                        onToggleMenu = currentConfig.onToggleMenu,
                        onNavigateAdjacent = onNavigateAdjacent,
                    ),
        ) {
            items(
                items = entries,
                key = { it.item.key("webtoon") },
            ) { entry ->
                val item = entry.item
                val gapModifier =
                    if (config.hasGaps && entry.hasGapBelow) pageGapModifier else Modifier
                when (item) {
                    is ReaderUiItem.Page -> {
                        WebtoonPageItem(
                            page = item.page,
                            backgroundColor = config.backgroundColor,
                            cropBorders = config.cropBorders,
                            colorFilter = config.colorFilter,
                            onLongClick = { onPageLongTap(item.page) },
                            tapClaim = tapClaim,
                            modifier = gapModifier,
                        )
                    }
                    is ReaderUiItem.SplitPage -> {
                        WebtoonPageItem(
                            split = item.split,
                            backgroundColor = config.backgroundColor,
                            cropBorders = config.cropBorders,
                            colorFilter = config.colorFilter,
                            onLongClick = { onPageLongTap(item.page) },
                            tapClaim = tapClaim,
                            modifier = gapModifier,
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
                                gapModifier
                                    .fillMaxWidth()
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

internal fun calculateDefaultWebtoonIndex(
    items: List<ReaderUiItem>,
    currentChapterId: Long?,
    requestedPage: Int?,
): Int {
    if (requestedPage != null && requestedPage >= 0) {
        val pageMatch = resolveItemIndexForPage(items, currentChapterId, requestedPage)
        if (pageMatch != null) return pageMatch
    }

    val defaultIndex =
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

    return defaultIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
}

internal fun isSameChapter(a: ReaderChapter, b: ReaderChapter): Boolean = modelIsSameChapter(a, b)

internal fun areTransitionsEquivalent(a: ChapterTransition, b: ChapterTransition): Boolean =
    modelAreTransitionsEquivalent(a, b)

internal fun areItemsEquivalent(a: ReaderUiItem, b: ReaderUiItem): Boolean = a.isEquivalentTo(b)

/** A webtoon list item and whether it gets a page gap below it when page gaps are on. */
internal data class WebtoonListEntry(val item: ReaderUiItem, val hasGapBelow: Boolean)

/**
 * Every item gets a page gap below it except the last one and a slice that continues into the next
 * slice of the same tall page, so a split page reads as one image.
 */
internal fun List<ReaderUiItem>.toWebtoonListEntries(): List<WebtoonListEntry> =
    mapIndexed { index, item ->
        val next = getOrNull(index + 1)
        WebtoonListEntry(item, hasGapBelow = next != null && !item.continuesInto(next))
    }

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

internal suspend fun executeWebtoonNavCommand(
    command: ReaderNavCommand,
    lazyListState: LazyListState,
    items: List<ReaderUiItem>,
    config: WebtoonViewerConfigUiModel,
    scrollAnchorState: ScrollAnchorState,
    zoomScale: Float = 1f,
) {
    try {
        when (command) {
            is ReaderNavCommand.ScrollToItem -> {
                val target = command.itemIndex.coerceIn(0, items.lastIndex)
                if (command.animated && config.animatedTransitions) {
                    lazyListState.animateScrollToItem(target)
                } else {
                    lazyListState.scrollToItem(target)
                }
                items.getOrNull(target)?.let {
                    scrollAnchorState.item = it
                    scrollAnchorState.offset = 0
                }
            }
            is ReaderNavCommand.ScrollToPage -> {
                val targetChapterId = command.chapterId ?: config.activeChapterId
                val target =
                    resolveItemIndexForPage(items, targetChapterId, command.pageIndex)
                        ?: command.pageIndex.coerceIn(0, items.lastIndex)
                if (command.animated && config.animatedTransitions) {
                    lazyListState.animateScrollToItem(target)
                } else {
                    lazyListState.scrollToItem(target)
                }
                items.getOrNull(target)?.let {
                    scrollAnchorState.item = it
                    scrollAnchorState.offset = 0
                }
            }
            is ReaderNavCommand.SnapToPage -> {
                val targetChapterId = command.chapterId ?: config.activeChapterId
                val target =
                    resolveItemIndexForPage(items, targetChapterId, command.pageIndex)
                        ?: command.pageIndex.coerceIn(0, items.lastIndex)
                lazyListState.scrollToItem(target)
                items.getOrNull(target)?.let {
                    scrollAnchorState.item = it
                    scrollAnchorState.offset = 0
                }
            }
            is ReaderNavCommand.StepPage -> {
                val delta = calculateWebtoonStepDelta(lazyListState.layoutInfo.viewportSize.height)
                val scrollAmount = if (command.forward) delta else -delta
                val effectiveScrollAmount = calculateEffectiveScrollAmount(scrollAmount, zoomScale)
                if (config.animatedTransitions) {
                    lazyListState.animateScrollBy(effectiveScrollAmount)
                } else {
                    lazyListState.scrollBy(effectiveScrollAmount)
                }
            }
            is ReaderNavCommand.ScrollByDelta -> {
                val scrollAmount = calculateEffectiveScrollAmount(command.delta, zoomScale)
                if (config.animatedTransitions) {
                    lazyListState.animateScrollBy(scrollAmount)
                } else {
                    lazyListState.scrollBy(scrollAmount)
                }
            }
        }
    } catch (_: CancellationException) {
        // Scroll command interrupted by user gesture or next navigation
    }
}

internal fun calculateWebtoonStepDelta(viewportHeight: Int): Float =
    if (viewportHeight > 0) viewportHeight * 0.9f else 500f

internal fun calculateEffectiveScrollAmount(scrollAmount: Float, zoomScale: Float): Float =
    if (zoomScale > 0f) scrollAmount / zoomScale else scrollAmount
