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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderNavCommand
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.isEquivalentTo
import eu.kanade.tachiyomi.ui.reader.model.withoutCardModel
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPanDelegate
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerScrollAnchorResolver
import eu.kanade.tachiyomi.ui.reader.viewer.pager.tryStepPan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.nekomanga.presentation.theme.Size

private class PagerLayoutSyncAnchor(
    var lastProcessedItems: List<ReaderUiItem>,
    var lastProcessedChapterId: Long?,
    var lastActiveItem: ReaderUiItem?,
    var preMeasureHandled: Boolean = false,
)

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
    val initialPage = config.initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val pagerState =
        rememberPagerState(
            initialPage = initialPage,
            pageCount = { items.size },
        )

    val layoutAnchor = remember {
        PagerLayoutSyncAnchor(
            lastProcessedItems = items,
            lastProcessedChapterId = config.activeChapterId,
            lastActiveItem = items.getOrNull(initialPage),
        )
    }
    var pendingNavCommand by remember { mutableStateOf<ReaderNavCommand?>(null) }

    val currentItems by rememberUpdatedState(items)
    val currentConfig by rememberUpdatedState(config)
    val currentIsNavigating by rememberUpdatedState(isNavigating)

    var activePanDelegate by remember { mutableStateOf<PagerPanDelegate?>(null) }
    val pageItemConfig =
        remember(currentConfig) {
            currentConfig.copy(
                onActivePanDelegateChanged = { delegate, active ->
                    if (active) {
                        activePanDelegate = delegate
                    } else if (activePanDelegate == delegate) {
                        activePanDelegate = null
                    }
                    currentConfig.onActivePanDelegateChanged?.invoke(delegate, active)
                }
            )
        }

    suspend fun executeNavCommand(command: ReaderNavCommand): Boolean {
        return executeNavCommand(
            command = command,
            pagerState = pagerState,
            items = currentItems,
            config = currentConfig,
            panDelegate = activePanDelegate,
        )
    }

    // 1. Immediate pre-measure re-anchor during composition to eliminate 1-frame flashes
    val chapterChanged = config.activeChapterId != layoutAnchor.lastProcessedChapterId
    if (items !== layoutAnchor.lastProcessedItems || chapterChanged) {
        val pendingTarget =
            resolvePendingNavTarget(pendingNavCommand, items, config.activeChapterId)

        layoutAnchor.lastProcessedItems = items
        layoutAnchor.lastProcessedChapterId = config.activeChapterId

        if (pendingTarget != null) {
            pendingNavCommand = null
            layoutAnchor.preMeasureHandled = true
            pagerState.requestScrollToPage(pendingTarget)
            items.getOrNull(pendingTarget)?.let { layoutAnchor.lastActiveItem = it }
        } else if (chapterChanged) {
            layoutAnchor.preMeasureHandled = true
            val chapterInitialIndex =
                config.initialIndex.takeIf { it in items.indices }
                    ?: resolveItemIndexForPage(items, config.activeChapterId, 0)
                    ?: PagerScrollAnchorResolver.resolveChapterBoundaryIndex(
                        items = items,
                        chapterId = config.activeChapterId,
                        isRtl = config.isRtl && !config.isVertical,
                        boundary = PagerScrollAnchorResolver.ChapterBoundary.START,
                    )
                    ?: 0
            pagerState.requestScrollToPage(chapterInitialIndex)
            items.getOrNull(chapterInitialIndex)?.let { layoutAnchor.lastActiveItem = it }
        } else {
            val target =
                PagerScrollAnchorResolver.resolveReanchorTarget(
                    items = items,
                    lastActiveItem = layoutAnchor.lastActiveItem,
                    currentVisibleIndex = pagerState.currentPage,
                    previousItems = layoutAnchor.lastProcessedItems,
                    isRtl = config.isRtl && !config.isVertical,
                    activeChapterId = config.activeChapterId,
                )
            if (target != null && target.index != pagerState.currentPage) {
                layoutAnchor.preMeasureHandled = true
                pagerState.requestScrollToPage(target.index)
                layoutAnchor.lastActiveItem = target.item
            } else {
                layoutAnchor.preMeasureHandled = false
            }
        }
    }

    // 2. Fallback post-composition anchor sync
    LaunchedEffect(items, config.activeChapterId) {
        val pending = pendingNavCommand
        if (pending != null) {
            if (executeNavCommand(pending)) {
                pendingNavCommand = null
            }
        } else {
            val wasPreMeasureHandled = layoutAnchor.preMeasureHandled
            layoutAnchor.preMeasureHandled = false
            if (!wasPreMeasureHandled) {
                val currentItem = items.getOrNull(pagerState.currentPage)
                val activeItem = layoutAnchor.lastActiveItem
                if (
                    currentItem != null &&
                        activeItem != null &&
                        !currentItem.isEquivalentTo(activeItem)
                ) {
                    val target =
                        PagerScrollAnchorResolver.resolveReanchorTarget(
                            items = items,
                            lastActiveItem = activeItem,
                            currentVisibleIndex = pagerState.currentPage,
                            previousItems = layoutAnchor.lastProcessedItems,
                            isRtl = config.isRtl && !config.isVertical,
                            activeChapterId = config.activeChapterId,
                        )
                    if (target != null && target.index != pagerState.currentPage) {
                        try {
                            pagerState.scrollToPage(target.index)
                            layoutAnchor.lastActiveItem = target.item
                        } catch (_: CancellationException) {
                            // Re-anchor interrupted by user gesture
                        }
                    }
                }
            }
        }
    }

    // 3. Consume unidirectional programmatic navigation commands
    LaunchedEffect(navCommands) {
        navCommands?.collect { command ->
            try {
                if (!executeNavCommand(command)) {
                    pendingNavCommand = command
                } else {
                    pendingNavCommand = null
                }
            } catch (_: CancellationException) {
                // Interruption must never terminate the navCommands collector
            }
        }
    }

    // 4. Safety watchdog: clear stale pending navigation command after timeout
    LaunchedEffect(pendingNavCommand, currentIsNavigating) {
        if (pendingNavCommand != null) {
            if (currentIsNavigating) {
                // Generous 60-second safety backstop while a chapter is actively loading/settling
                // so slow network or disk loading does not prematurely drop valid user navigation
                // commands.
                delay(60000L)
                pendingNavCommand = null
            } else {
                // If viewer is idle and navigation was not consumed within 10s, clear stale command
                delay(10000L)
                pendingNavCommand = null
            }
        }
    }

    // 5. Invalidate deferred navigation if user manually intervenes by swiping
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress && !currentIsNavigating && pendingNavCommand != null) {
            pendingNavCommand = null
        }
    }

    // 6. Track active page changes, dispatch selections, and trigger threshold preloads. Keyed on
    // the items without their card models, so a transition card showing a new preload state does
    // not dispatch the selection and request the preload again.
    val itemsWithoutCards = remember(items) { items.map { it.withoutCardModel() } }
    LaunchedEffect(pagerState, itemsWithoutCards) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val item = currentItems.getOrNull(pageIndex) ?: return@collect
                layoutAnchor.lastActiveItem = item
                onActiveItemChanged(pageIndex)

                when (item) {
                    is ReaderUiItem.Page -> {
                        onPageSelected(item.page, item.extraPage != null)
                        val pages = item.page.chapter.pages
                        if (
                            pages != null &&
                                item.page.chapter.chapter.id == currentConfig.activeChapterId
                        ) {
                            val threshold = maxOf(5, currentConfig.preloadPageAmount)
                            if (pages.size - item.page.number < threshold) {
                                val nextTransition =
                                    currentItems.firstOrNull {
                                        it is ReaderUiItem.Transition &&
                                            it.transition is ChapterTransition.Next
                                    } as? ReaderUiItem.Transition
                                nextTransition?.transition?.to?.let {
                                    currentConfig.onRequestPreloadChapter?.invoke(it)
                                }
                            }
                            if (item.page.number <= threshold) {
                                val prevTransition =
                                    currentItems.firstOrNull {
                                        it is ReaderUiItem.Transition &&
                                            it.transition is ChapterTransition.Prev
                                    } as? ReaderUiItem.Transition
                                prevTransition?.transition?.to?.let {
                                    currentConfig.onRequestPreloadChapter?.invoke(it)
                                }
                            }
                        }
                    }
                    is ReaderUiItem.SplitPage -> onPageSelected(item.page, false)
                    is ReaderUiItem.Transition -> {
                        onTransitionSelected(item.transition)
                        item.transition.to?.let {
                            currentConfig.onRequestPreloadChapter?.invoke(it)
                        }
                    }
                }
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
                PagerItemContent(
                    item = item,
                    config = pageItemConfig,
                    isActive = index == pagerState.currentPage,
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
                    item = item,
                    config = pageItemConfig,
                    isActive = index == pagerState.currentPage,
                )
            }
        }
    }
}

@Composable
private fun PagerItemContent(
    item: ReaderUiItem,
    config: PagerViewerConfigUiModel,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is ReaderUiItem.Page -> {
            PagerPageItem(
                page = item.page,
                config = config,
                extraPage = item.extraPage,
                modifier = modifier,
                isActive = isActive,
            )
        }
        is ReaderUiItem.SplitPage -> {
            PagerPageItem(
                page = item.page,
                config = config,
                modifier = modifier,
                isActive = isActive,
            )
        }
        is ReaderUiItem.Transition -> {
            val uiModel = item.transitionUiModel ?: ChapterTransitionUiModel.from(item.transition)
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
        }
    }
}

/** Item index in [items] that a deferred [pending] command should land on, or null. */
internal fun resolvePendingNavTarget(
    pending: ReaderNavCommand?,
    items: List<ReaderUiItem>,
    activeChapterId: Long?,
): Int? =
    when (pending) {
        is ReaderNavCommand.SnapToPage ->
            resolveItemIndexForPage(items, pending.chapterId ?: activeChapterId, pending.pageIndex)
        is ReaderNavCommand.ScrollToPage ->
            resolveItemIndexForPage(items, pending.chapterId ?: activeChapterId, pending.pageIndex)
        is ReaderNavCommand.ScrollToItem -> {
            val item = pending.item
            if (item == null) {
                pending.itemIndex.takeIf { it in items.indices }
            } else {
                resolveScrollToItemIndex(items, pending.itemIndex, item, activeChapterId)
            }
        }
        else -> null
    }

/**
 * Index of [item] in [items] for a ScrollToItem that PagerViewer computed as [itemIndex] against
 * its newest list, or null to wait for another list.
 *
 * The item is followed to another index only when it belongs to [activeChapterId] or is a
 * transition card. A page of any other chapter can sit in this list as an adjacent chapter preview,
 * and landing on that copy selects its chapter from the wrong list, which starts the chapter
 * ping-pong again.
 */
internal fun resolveScrollToItemIndex(
    items: List<ReaderUiItem>,
    itemIndex: Int,
    item: ReaderUiItem,
    activeChapterId: Long?,
): Int? =
    when {
        items.getOrNull(itemIndex)?.isEquivalentTo(item) == true -> itemIndex
        item is ReaderUiItem.Transition || item.chapterId == activeChapterId ->
            items.indexOfFirst { it.isEquivalentTo(item) }.takeIf { it != -1 }
        else -> null
    }

internal fun calculateDefaultPagerIndex(
    items: List<ReaderUiItem>,
    currentChapterId: Long?,
    requestedPage: Int?,
): Int {
    if (requestedPage != null && requestedPage > 0) {
        val match = items.indexOfFirst { item ->
            when (item) {
                is ReaderUiItem.Page ->
                    item.page.chapter.chapter.id == currentChapterId &&
                        (item.page.index == requestedPage || item.extraPage?.index == requestedPage)
                is ReaderUiItem.SplitPage ->
                    item.page.chapter.chapter.id == currentChapterId &&
                        item.page.index == requestedPage
                else -> false
            }
        }
        if (match != -1) return match
    }

    val firstPageMatch = items.indexOfFirst { item ->
        when (item) {
            is ReaderUiItem.Page -> {
                item.page.chapter.chapter.id == currentChapterId &&
                    (item.page.index == 0 || item.extraPage?.index == 0)
            }
            is ReaderUiItem.SplitPage -> {
                item.page.chapter.chapter.id == currentChapterId && item.page.index == 0
            }
            else -> false
        }
    }
    if (firstPageMatch != -1) return firstPageMatch

    var minPageIndex = Int.MAX_VALUE
    var targetItemIndex = -1
    for (i in items.indices) {
        val item = items[i]
        val (chId, pageIdx) =
            when (item) {
                is ReaderUiItem.Page -> {
                    val pMin =
                        minOf(
                            item.page.index,
                            item.extraPage?.index ?: Int.MAX_VALUE,
                        )
                    item.page.chapter.chapter.id to pMin
                }
                is ReaderUiItem.SplitPage -> {
                    item.page.chapter.chapter.id to item.page.index
                }
                else -> null to Int.MAX_VALUE
            }
        if (chId == currentChapterId && pageIdx < minPageIndex) {
            minPageIndex = pageIdx
            targetItemIndex = i
        }
    }
    if (targetItemIndex != -1) return targetItemIndex

    val anyPage = items.indexOfFirst { it is ReaderUiItem.Page || it is ReaderUiItem.SplitPage }
    return if (anyPage != -1) anyPage else 0
}

internal fun resolveItemIndexForPage(
    items: List<ReaderUiItem>,
    targetChapterId: Long?,
    pageIndex: Int,
): Int? {
    if (items.isEmpty()) return null

    // 1. Try to match the exact page within the specified chapter
    if (targetChapterId != null && targetChapterId > 0) {
        val chapterMatch = items.indexOfFirst { item ->
            when (item) {
                is ReaderUiItem.Page -> {
                    item.chapterId == targetChapterId &&
                        (item.pageIndex == pageIndex || item.extraPage?.index == pageIndex)
                }
                is ReaderUiItem.SplitPage -> {
                    item.chapterId == targetChapterId && item.pageIndex == pageIndex
                }
                else -> false
            }
        }
        if (chapterMatch != -1) return chapterMatch

        // When a targetChapterId is specified, never fall through to match other chapters
        // or clamp across all items. Doing so would navigate to a completely wrong chapter.
        return null
    }

    // 2. Fallback (only when no specific chapter was targeted): match by page index across items
    val pageMatch = items.indexOfFirst { item ->
        when (item) {
            is ReaderUiItem.Page ->
                item.pageIndex == pageIndex || item.extraPage?.index == pageIndex
            is ReaderUiItem.SplitPage -> item.pageIndex == pageIndex
            else -> false
        }
    }
    if (pageMatch != -1) return pageMatch

    // 3. Fallback: clamp within bounds
    return pageIndex.coerceIn(0, items.lastIndex)
}

internal fun resolveTransitionIndexForChapter(
    items: List<ReaderUiItem>,
    targetChapterId: Long?,
): Int? {
    if (targetChapterId == null || targetChapterId <= 0L || items.isEmpty()) return null
    val index = items.indexOfFirst { item ->
        item is ReaderUiItem.Transition && item.transition.to?.chapter?.id == targetChapterId
    }
    return if (index != -1) index else null
}

internal suspend fun executeNavCommand(
    command: ReaderNavCommand,
    pagerState: PagerState,
    items: List<ReaderUiItem>,
    config: PagerViewerConfigUiModel,
    panDelegate: PagerPanDelegate? = null,
): Boolean {
    try {
        when (command) {
            is ReaderNavCommand.ScrollToItem -> {
                // PagerViewer computed the index against its newest list. The pager can still
                // hold the list from before a chapter switch, where that index is another
                // chapter's page; wait for the list that has the item.
                if (items.isEmpty()) return false
                val item = command.item
                val target =
                    if (item == null) {
                        command.itemIndex.coerceIn(0, items.lastIndex)
                    } else {
                        resolveScrollToItemIndex(
                            items,
                            command.itemIndex,
                            item,
                            config.activeChapterId,
                        ) ?: return false
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
                return true
            }
            is ReaderNavCommand.ScrollToPage -> {
                val targetChapterId = command.chapterId ?: config.activeChapterId
                val target = resolveItemIndexForPage(items, targetChapterId, command.pageIndex)
                if (target != null) {
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
                    return true
                } else {
                    val transitionIndex = resolveTransitionIndexForChapter(items, targetChapterId)
                    if (transitionIndex != null && pagerState.currentPage != transitionIndex) {
                        if (command.animated && config.animatedTransitions) {
                            pagerState.animateScrollToPage(
                                page = transitionIndex,
                                animationSpec =
                                    tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            )
                        } else {
                            pagerState.scrollToPage(transitionIndex)
                        }
                    }
                    return false
                }
            }
            is ReaderNavCommand.SnapToPage -> {
                val targetChapterId = command.chapterId ?: config.activeChapterId
                val target = resolveItemIndexForPage(items, targetChapterId, command.pageIndex)
                if (target != null) {
                    if (target in items.indices && pagerState.currentPage != target) {
                        pagerState.scrollToPage(target)
                    }
                    return true
                } else {
                    val transitionIndex = resolveTransitionIndexForChapter(items, targetChapterId)
                    if (transitionIndex != null && pagerState.currentPage != transitionIndex) {
                        pagerState.scrollToPage(transitionIndex)
                    }
                    return false
                }
            }
            is ReaderNavCommand.StepPage -> {
                if (config.navigateToPan && panDelegate != null) {
                    val panned =
                        panDelegate.tryStepPan(
                            isVertical = config.isVertical,
                            isRtl = config.isRtl,
                            forward = command.forward,
                        )
                    if (panned) return true
                }

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
                } else {
                    val item = items.getOrNull(pagerState.currentPage)
                    if (command.forward) {
                        val nextChapter =
                            (item as? ReaderUiItem.Transition)?.let {
                                if (it.transition is ChapterTransition.Next)
                                    it.transition.to?.chapter
                                else null
                            }
                        if (nextChapter != null && config.onNavigateToChapter != null) {
                            config.onNavigateToChapter.invoke(nextChapter, ChapterNavTarget.Start)
                        } else {
                            config.onNavigateAdjacentChapter?.invoke(true)
                        }
                    } else {
                        val prevChapter =
                            (item as? ReaderUiItem.Transition)?.let {
                                if (it.transition is ChapterTransition.Prev)
                                    it.transition.to?.chapter
                                else null
                            }
                        if (prevChapter != null && config.onNavigateToChapter != null) {
                            config.onNavigateToChapter.invoke(prevChapter, ChapterNavTarget.End)
                        } else {
                            config.onNavigateAdjacentChapter?.invoke(false)
                        }
                    }
                }
                return true
            }
            is ReaderNavCommand.ScrollByDelta -> return true
        }
    } catch (_: CancellationException) {
        // Scroll command was interrupted or superseded by another user gesture or navigation
        return true
    }
}
