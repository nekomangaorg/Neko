package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size as CoilSize
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.util.system.GLUtil
import kotlin.math.hypot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Pure stateless Composable rendering an individual paginated reader item (single page, paired
 * spread, or split double page). Completely decoupled from Service Locators and View hierarchies.
 */
@Composable
fun PagerPageItem(
    page: ReaderPage,
    config: PagerViewerConfigUiModel,
    extraPage: ReaderPage? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val viewConfiguration = remember(context) { ViewConfiguration.get(context) }
    val touchSlopPx = remember(viewConfiguration) { viewConfiguration.scaledTouchSlop.toDouble() }
    val doubleTapSlopPx =
        remember(viewConfiguration) { viewConfiguration.scaledDoubleTapSlop.toDouble() }
    val doubleTapTimeoutMs = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }
    val longPressTimeoutMs = remember { ViewConfiguration.getLongPressTimeout().toLong() }

    // Trigger page loading
    LaunchedEffect(page) { page.chapter.pageLoader?.loadPage(page) }
    LaunchedEffect(extraPage) {
        if (extraPage != null) {
            extraPage.chapter.pageLoader?.loadPage(extraPage)
        }
    }

    val pageStatus by page.statusFlow.collectAsStateWithLifecycle(Page.State.QUEUE)
    val pageProgress by page.progressFlow.collectAsStateWithLifecycle(0)

    val extraPageStatus by
        (extraPage?.statusFlow ?: emptyFlow()).collectAsStateWithLifecycle(Page.State.READY)
    val extraPageProgress by (extraPage?.progressFlow ?: emptyFlow()).collectAsStateWithLifecycle(0)

    val isError =
        pageStatus == Page.State.ERROR || (extraPage != null && extraPageStatus == Page.State.ERROR)
    val isReady =
        pageStatus == Page.State.READY && (extraPage == null || extraPageStatus == Page.State.READY)

    val combinedStatus =
        when {
            isError -> Page.State.ERROR
            isReady -> Page.State.READY
            pageStatus == Page.State.DOWNLOAD_IMAGE ||
                extraPageStatus == Page.State.DOWNLOAD_IMAGE -> Page.State.DOWNLOAD_IMAGE
            pageStatus == Page.State.LOAD_PAGE || extraPageStatus == Page.State.LOAD_PAGE ->
                Page.State.LOAD_PAGE
            else -> Page.State.QUEUE
        }
    val combinedProgress =
        if (extraPage == null) pageProgress else (pageProgress + extraPageProgress) / 2

    val imageAlignment =
        remember(config.zoomStart, config.isRtl) {
            val zoomType =
                when (config.zoomStart) {
                    1 -> if (config.isRtl) PagerConfig.ZoomType.Right else PagerConfig.ZoomType.Left
                    2 -> PagerConfig.ZoomType.Left
                    3 -> PagerConfig.ZoomType.Right
                    else -> PagerConfig.ZoomType.Center
                }

            Alignment { size, space, _ ->
                val x =
                    if (size.width > space.width) {
                        when (zoomType) {
                            PagerConfig.ZoomType.Left -> 0
                            PagerConfig.ZoomType.Right -> space.width - size.width
                            PagerConfig.ZoomType.Center -> (space.width - size.width) / 2
                        }
                    } else {
                        (space.width - size.width) / 2
                    }

                val y =
                    if (size.height > space.height) {
                        0
                    } else {
                        (space.height - size.height) / 2
                    }

                IntOffset(x, y)
            }
        }

    val contentScale =
        remember(config.imageScaleType) {
            when (config.imageScaleType) {
                1 -> ContentScale.Fit
                2 -> ContentScale.FillBounds
                3 -> ContentScale.FillWidth
                4 -> ContentScale.FillHeight
                5 -> ContentScale.None
                6 -> SmartFitContentScale
                else -> ContentScale.Fit
            }
        }

    val onRetry: () -> Unit = {
        page.chapter.pageLoader?.retryPage(page)
        extraPage?.chapter?.pageLoader?.retryPage(extraPage)
    }

    val doubleClickToZoomListener =
        remember(config.doubleTapAnimDuration) {
            if (config.doubleTapAnimDuration > 0) {
                DoubleClickToZoomListener.cycle(maxZoomFactor = 2.5f)
            } else {
                DoubleClickToZoomListener { _, _ -> }
            }
        }

    BoxWithConstraints(
        modifier =
            modifier.fillMaxSize().background(config.backgroundColor).pointerInput(
                config,
                page,
                extraPage,
            ) {
                var lastTapTime = 0L
                var lastTapOffset = Offset.Zero

                awaitEachGesture {
                    val down =
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                    val downPos = down.position
                    var isLongPressTriggered = false
                    var isMovementPastSlop = false
                    var pointerUp: PointerInputChange? = null

                    try {
                        withTimeout(longPressTimeoutMs) {
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) break
                                val moveDistance =
                                    hypot(
                                        (change.position.x - downPos.x).toDouble(),
                                        (change.position.y - downPos.y).toDouble(),
                                    )
                                if (moveDistance > touchSlopPx) {
                                    isMovementPastSlop = true
                                    break
                                }
                                if (!change.pressed) {
                                    pointerUp = change
                                    break
                                }
                            }
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (!isMovementPastSlop && (config.menuVisible || config.longTapEnabled)) {
                            config.onPageLongTap?.invoke(page, extraPage)
                            isLongPressTriggered = true
                        }
                        while (currentEvent.changes.any { it.pressed }) {
                            awaitPointerEvent(pass = PointerEventPass.Initial)
                        }
                    }

                    if (pointerUp == null && !isLongPressTriggered) {
                        while (currentEvent.changes.any { it.pressed }) {
                            awaitPointerEvent(pass = PointerEventPass.Initial)
                        }
                    }

                    if (!isLongPressTriggered && !isMovementPastSlop && pointerUp != null) {
                        val up = pointerUp!!
                        val upPos = up.position
                        val upTime = System.currentTimeMillis()
                        val distance =
                            hypot(
                                (upPos.x - downPos.x).toDouble(),
                                (upPos.y - downPos.y).toDouble(),
                            )

                        if (distance < touchSlopPx * 1.5) {
                            val screenWidth = size.width.toFloat()
                            val screenHeight = size.height.toFloat()

                            if (screenWidth > 0 && screenHeight > 0) {
                                val pos =
                                    PointF(
                                        upPos.x / screenWidth,
                                        upPos.y / screenHeight,
                                    )
                                val navigator = config.navigator
                                val action = navigator.getAction(pos)

                                val isDoubleTap =
                                    (upTime - lastTapTime < doubleTapTimeoutMs) &&
                                        (hypot(
                                            (upPos.x - lastTapOffset.x).toDouble(),
                                            (upPos.y - lastTapOffset.y).toDouble(),
                                        ) < doubleTapSlopPx) &&
                                        (config.doubleTapAnimDuration > 0)

                                if (isDoubleTap) {
                                    if (config.menuVisible) {
                                        config.onToggleMenu()
                                    }
                                    lastTapTime = 0L
                                    lastTapOffset = Offset.Zero
                                } else {
                                    lastTapTime = upTime
                                    lastTapOffset = upPos

                                    when (action) {
                                        ViewerNavigation.NavigationRegion.NEXT -> {
                                            up.consume()
                                            if (config.menuVisible) {
                                                config.onToggleMenu()
                                            }
                                            config.onNavigateAdjacent(true)
                                        }
                                        ViewerNavigation.NavigationRegion.PREV -> {
                                            up.consume()
                                            if (config.menuVisible) {
                                                config.onToggleMenu()
                                            }
                                            config.onNavigateAdjacent(false)
                                        }
                                        ViewerNavigation.NavigationRegion.RIGHT -> {
                                            up.consume()
                                            if (config.menuVisible) {
                                                config.onToggleMenu()
                                            }
                                            if (config.isRtl) {
                                                config.onNavigateAdjacent(false)
                                            } else {
                                                config.onNavigateAdjacent(true)
                                            }
                                        }
                                        ViewerNavigation.NavigationRegion.LEFT -> {
                                            up.consume()
                                            if (config.menuVisible) {
                                                config.onToggleMenu()
                                            }
                                            if (config.isRtl) {
                                                config.onNavigateAdjacent(true)
                                            } else {
                                                config.onNavigateAdjacent(false)
                                            }
                                        }
                                        ViewerNavigation.NavigationRegion.MENU -> {
                                            config.onToggleMenu()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()
        var autoZoomApplied by
            rememberSaveable(
                page.chapter.chapter.id,
                page.index,
                constraints.maxWidth,
                constraints.maxHeight,
            ) {
                mutableStateOf(false)
            }

        val zoomSpec = remember { ZoomSpec(maxZoomFactor = 5f) }

        if (extraPage != null) {
            val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)
            DoublePageLayout(
                page = page,
                extraPage = extraPage,
                config = config,
                zoomableState = zoomableState,
                contentScale = contentScale,
                doubleClickToZoomListener = doubleClickToZoomListener,
                constraints = constraints,
                isReady = isReady,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (page.firstHalf != null) {
            val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)
            SplitPageLayout(
                page = page,
                config = config,
                zoomableState = zoomableState,
                doubleClickToZoomListener = doubleClickToZoomListener,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)
            val imageState = rememberZoomableImageState(zoomableState)

            val singlePageZoomType =
                remember(config.zoomStart, config.isRtl) {
                    when (config.zoomStart) {
                        1 ->
                            if (config.isRtl) PagerConfig.ZoomType.Right
                            else PagerConfig.ZoomType.Left
                        2 -> PagerConfig.ZoomType.Left
                        3 -> PagerConfig.ZoomType.Right
                        else -> PagerConfig.ZoomType.Center
                    }
                }

            LaunchedEffect(isReady, config.landscapeZoom, config.imageScaleType) {
                if (
                    !autoZoomApplied &&
                        isReady &&
                        config.landscapeZoom &&
                        config.imageScaleType == 1 &&
                        viewportWidthPx > 0f &&
                        viewportHeightPx > 0f
                ) {
                    @Suppress("DEPRECATION")
                    val bounds =
                        withTimeoutOrNull(2000L) {
                            snapshotFlow { zoomableState.transformedContentBounds }
                                .filter { !it.isEmpty }
                                .first()
                        }

                    if (bounds != null) {
                        val isLandscape = bounds.width > bounds.height
                        if (isLandscape && bounds.height < viewportHeightPx) {
                            val targetScale = (viewportHeightPx / bounds.height).coerceIn(1f, 3f)
                            if (targetScale > 1.05f) {
                                val centroid =
                                    when (singlePageZoomType) {
                                        PagerConfig.ZoomType.Right ->
                                            Offset(viewportWidthPx, viewportHeightPx / 2f)
                                        PagerConfig.ZoomType.Left ->
                                            Offset(0f, viewportHeightPx / 2f)
                                        PagerConfig.ZoomType.Center ->
                                            Offset(viewportWidthPx / 2f, viewportHeightPx / 2f)
                                    }
                                zoomableState.zoomTo(zoomFactor = targetScale, centroid = centroid)
                            }
                        }
                    }
                    autoZoomApplied = true
                }
            }

            val model =
                remember(page) {
                    ImageRequest.Builder(context)
                        .data(page)
                        .size(CoilSize.ORIGINAL)
                        .maxBitmapSize(CoilSize(GLUtil.maxTextureSize, GLUtil.maxTextureSize))
                        .precision(Precision.EXACT)
                        .crossfade(true)
                        .build()
                }

            ZoomableAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = contentScale,
                alignment = imageAlignment,
                state = imageState,
                onDoubleClick = doubleClickToZoomListener,
                modifier = Modifier.fillMaxSize(),
            )
        }

        ReaderPageLoadingOverlay(status = combinedStatus, progress = combinedProgress)

        ReaderPageErrorOverlay(
            visible = isError,
            onRetry = onRetry,
            message = page.errorMessage ?: extraPage?.errorMessage,
        )
    }
}

/** Legacy compatibility overload for [PagerPageItem] passing [PagerViewer]. */
@Deprecated("Use stateless PagerPageItem with PagerViewerConfigUiModel")
@Composable
fun PagerPageItem(
    viewer: PagerViewer,
    page: ReaderPage,
    extraPage: ReaderPage? = null,
    modifier: Modifier = Modifier,
) {
    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val imageScaleType by readerPreferences.imageScaleType().collectAsState()
    val doublePageGap by readerPreferences.doublePageGap().collectAsState()
    val invertDoublePages by readerPreferences.invertDoublePages().collectAsState()
    val readerThemePref by readerPreferences.readerTheme().collectAsState()
    val landscapeZoom by readerPreferences.landscapeZoom().collectAsState()
    val zoomStart by readerPreferences.zoomStart().collectAsState()

    val themeBackground = MaterialTheme.colorScheme.background
    val backgroundColor =
        remember(readerThemePref, themeBackground) {
            ReaderTheme.fromPreference(readerThemePref).color(themeBackground)
        }

    val config =
        PagerViewerConfigUiModel(
            backgroundColor = backgroundColor,
            isRtl = viewer.isRtl,
            imageScaleType = imageScaleType,
            doublePageGap = doublePageGap,
            invertDoublePages = invertDoublePages,
            landscapeZoom = landscapeZoom,
            zoomStart = zoomStart,
            doubleTapAnimDuration = viewer.config.doubleTapAnimDuration,
            longTapEnabled = viewer.config.longTapEnabled,
            menuVisible = viewer.activity.menuVisible,
            navigator = viewer.config.navigator,
            onToggleMenu = { viewer.activity.toggleMenu() },
            onNavigateAdjacent = { forward ->
                if (forward) viewer.moveToNext() else viewer.moveToPrevious()
            },
            onPageLongTap = { p, ep -> viewer.activity.onPageLongTap(p, ep) },
        )

    PagerPageItem(
        page = page,
        config = config,
        extraPage = extraPage,
        modifier = modifier,
    )
}

private object SmartFitContentScale : ContentScale {
    override fun computeScaleFactor(
        srcSize: ComposeSize,
        dstSize: ComposeSize,
    ): ScaleFactor {
        return if (srcSize.isSpecified && !srcSize.isEmpty()) {
            if (srcSize.height > srcSize.width) {
                ContentScale.FillWidth.computeScaleFactor(srcSize, dstSize)
            } else {
                ContentScale.FillHeight.computeScaleFactor(srcSize, dstSize)
            }
        } else {
            ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
        }
    }
}
