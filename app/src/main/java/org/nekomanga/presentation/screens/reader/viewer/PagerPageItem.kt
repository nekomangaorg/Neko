package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size as CoilSize
import coil3.transform.Transformation
import eu.kanade.tachiyomi.data.coil.CropBordersTransformation
import eu.kanade.tachiyomi.data.coil.RotateWidePageTransformation
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderColorFilter
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.util.system.GLUtil
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsStateWithLifecycle as preferenceCollectAsStateWithLifecycle
import org.nekomanga.presentation.theme.Size
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
    isActive: Boolean = false,
) {
    val context = LocalContext.current

    // Trigger page loading
    LaunchedEffect(page) {
        if (page.status == Page.State.QUEUE) {
            page.chapter.pageLoader?.loadPage(page)
        }
    }
    LaunchedEffect(extraPage) {
        if (extraPage != null && extraPage.status == Page.State.QUEUE) {
            extraPage.chapter.pageLoader?.loadPage(extraPage)
        }
    }

    val pageStatus by page.statusFlow.collectAsStateWithLifecycle(Page.State.QUEUE)
    val pageProgress by page.progressFlow.collectAsStateWithLifecycle(0)

    val extraPageStatus by
        (extraPage?.statusFlow ?: emptyFlow()).collectAsStateWithLifecycle(Page.State.READY)
    val extraPageProgress by (extraPage?.progressFlow ?: emptyFlow()).collectAsStateWithLifecycle(0)

    val retryGeneration by page.retryGenerationFlow.collectAsStateWithLifecycle()
    val loadErrors = remember(page, extraPage) { PagerImageLoadErrors() }

    val isError =
        pageStatus == Page.State.ERROR ||
            (extraPage != null && extraPageStatus == Page.State.ERROR) ||
            loadErrors.hasError
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
        loadErrors.clear()
        page.retry()
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
            modifier
                .fillMaxSize()
                .background(config.backgroundColor)
                .pagerTapNavigation(
                    config = config,
                    page = page,
                    extraPage = extraPage,
                ),
        contentAlignment = Alignment.Center,
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()
        val shouldRotateWide =
            RotateWidePageTransformation.shouldRotateForViewport(
                rotateWide = config.doublePageRotate,
                viewportWidth = viewportWidthPx,
                viewportHeight = viewportHeightPx,
            )

        var autoZoomApplied by
            rememberSaveable(
                page.chapter.chapter.id,
                page.index,
                constraints.maxWidth,
                constraints.maxHeight,
                shouldRotateWide,
                config.doublePageRotateReverse,
            ) {
                mutableStateOf(false)
            }

        val zoomSpec = remember { ZoomSpec(maxZoomFactor = 5f) }
        val zoomableState =
            key(constraints.maxWidth, constraints.maxHeight) {
                rememberZoomableState(zoomSpec = zoomSpec)
            }

        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current
        val panEpsilonPx = remember(density) { with(density) { Size.extraTiny.toPx() } }
        val panDelegate =
            remember(
                zoomableState,
                viewportWidthPx,
                viewportHeightPx,
                config.animatedTransitions,
                config.doubleTapAnimDuration,
                panEpsilonPx,
            ) {
                ZoomablePanDelegate(
                    zoomableState = zoomableState,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                    scope = coroutineScope,
                    animated = config.animatedTransitions,
                    animationDurationMillis = config.doubleTapAnimDuration,
                    panEpsilonPx = panEpsilonPx,
                )
            }

        if (config.navigateToPan) {
            DisposableEffect(panDelegate, isActive) {
                if (isActive) {
                    config.onActivePanDelegateChanged?.invoke(panDelegate, true)
                }
                onDispose {
                    if (isActive) {
                        config.onActivePanDelegateChanged?.invoke(panDelegate, false)
                    }
                }
            }
        }

        SmartPageBackground(
            source =
                smartBackgroundSource(
                    page = page,
                    extraPage = extraPage,
                    isRtl = config.isRtl,
                    invertDoublePages = config.invertDoublePages,
                    rotateWide = shouldRotateWide,
                    rotateReverse = config.doublePageRotateReverse,
                ),
            baseColor = config.smartBackgroundBaseColor,
            isReady = isReady,
            isError = isError,
            colorFilter = config.colorFilter,
        )

        // A retry does not change the image requests, and the images load again only for a
        // changed request, so each retry gets new images.
        key(retryGeneration) {
            if (extraPage != null) {
                DoublePageLayout(
                    page = page,
                    extraPage = extraPage,
                    config = config,
                    zoomableState = zoomableState,
                    contentScale = contentScale,
                    doubleClickToZoomListener = doubleClickToZoomListener,
                    constraints = constraints,
                    isReady = isReady,
                    onImageError = loadErrors::onError,
                    onImageSuccess = loadErrors::onSuccess,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (page.firstHalf != null) {
                SplitPageLayout(
                    page = page,
                    config = config,
                    zoomableState = zoomableState,
                    doubleClickToZoomListener = doubleClickToZoomListener,
                    onImageError = loadErrors::onError,
                    onImageSuccess = loadErrors::onSuccess,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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

                LaunchedEffect(
                    isReady,
                    config.landscapeZoom,
                    config.imageScaleType,
                    shouldRotateWide,
                ) {
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
                                val targetScale =
                                    (viewportHeightPx / bounds.height).coerceIn(1f, 3f)
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
                                    zoomableState.zoomTo(
                                        zoomFactor = targetScale,
                                        centroid = centroid,
                                    )
                                }
                            }
                        }
                        autoZoomApplied = true
                    }
                }

                val model =
                    remember(
                        page,
                        loadErrors,
                        config.cropBorders,
                        shouldRotateWide,
                        config.doublePageRotateReverse,
                    ) {
                        ImageRequest.Builder(context)
                            .data(page)
                            // ZoomableAsyncImage replaces maxBitmapSize with ORIGINAL because it
                            // expects to sub-sample, which never happens for a ReaderPage. It keeps
                            // size, so the canvas size cap goes there. Without the cap, a page
                            // taller than the texture size decodes at full size and draws blank as
                            // a hardware bitmap or is too large for the canvas as a software one.
                            .size(
                                CoilSize(GLUtil.maxCanvasTextureSize, GLUtil.maxCanvasTextureSize)
                            )
                            .scale(Scale.FIT)
                            .precision(Precision.INEXACT)
                            .crossfade(true)
                            // ZoomableAsyncImage has no error callback, but the request it rebuilds
                            // keeps this listener.
                            .listener(
                                onError = { _, result ->
                                    loadErrors.onError(page, result.throwable)
                                },
                                onSuccess = { _, _ -> loadErrors.onSuccess(page) },
                            )
                            .apply {
                                val transformations = mutableListOf<Transformation>()
                                if (config.cropBorders) {
                                    transformations.add(
                                        CropBordersTransformation(cropTopBottom = true)
                                    )
                                }
                                if (shouldRotateWide) {
                                    transformations.add(
                                        RotateWidePageTransformation(
                                            rotateWide = true,
                                            reverse = config.doublePageRotateReverse,
                                        )
                                    )
                                }
                                if (transformations.isNotEmpty()) {
                                    transformations(transformations)
                                }
                            }
                            .build()
                    }

                ZoomableAsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = contentScale,
                    alignment = imageAlignment,
                    state = imageState,
                    colorFilter = config.colorFilter,
                    onDoubleClick = doubleClickToZoomListener,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        ReaderPageLoadingOverlay(status = combinedStatus, progress = combinedProgress)

        ReaderPageErrorOverlay(
            visible = isError,
            onRetry = onRetry,
            message = page.errorMessage ?: extraPage?.errorMessage ?: loadErrors.message,
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
    val imageScaleType by readerPreferences.imageScaleType().preferenceCollectAsStateWithLifecycle()
    val doublePageGap by readerPreferences.doublePageGap().preferenceCollectAsStateWithLifecycle()
    val invertDoublePages by
        readerPreferences.invertDoublePages().preferenceCollectAsStateWithLifecycle()
    val readerThemePref by readerPreferences.readerTheme().preferenceCollectAsStateWithLifecycle()
    val landscapeZoom by readerPreferences.landscapeZoom().preferenceCollectAsStateWithLifecycle()
    val navigateToPan by readerPreferences.navigateToPan().preferenceCollectAsStateWithLifecycle()
    val zoomStart by readerPreferences.zoomStart().preferenceCollectAsStateWithLifecycle()
    val cropBorders by readerPreferences.cropBorders().preferenceCollectAsStateWithLifecycle()
    val grayscale by readerPreferences.grayscale().preferenceCollectAsStateWithLifecycle()
    val invertedColors by readerPreferences.invertedColors().preferenceCollectAsStateWithLifecycle()
    val doublePageRotate by
        readerPreferences.doublePageRotate().preferenceCollectAsStateWithLifecycle()
    val doublePageRotateReverse by
        readerPreferences.doublePageRotateReverse().preferenceCollectAsStateWithLifecycle()

    val themeBackground = MaterialTheme.colorScheme.background
    val backgroundColor =
        remember(readerThemePref, themeBackground) {
            ReaderTheme.fromPreference(readerThemePref).color(themeBackground)
        }
    val smartBackgroundBaseColor =
        remember(readerThemePref, themeBackground) {
            ReaderTheme.fromPreference(readerThemePref).smartBaseColor(themeBackground)
        }
    val colorFilter =
        remember(grayscale, invertedColors) {
            ReaderColorFilter.getColorFilter(grayscale, invertedColors)
        }

    val config =
        PagerViewerConfigUiModel(
            backgroundColor = backgroundColor,
            smartBackgroundBaseColor = smartBackgroundBaseColor,
            colorFilter = colorFilter,
            isRtl = viewer.isRtl,
            imageScaleType = imageScaleType,
            doublePageGap = doublePageGap,
            invertDoublePages = invertDoublePages,
            doublePageRotate = doublePageRotate,
            doublePageRotateReverse = doublePageRotateReverse,
            landscapeZoom = landscapeZoom,
            zoomStart = zoomStart,
            navigateToPan = navigateToPan,
            doubleTapAnimDuration = viewer.config.doubleTapAnimDuration,
            longTapEnabled = viewer.config.longTapEnabled,
            menuVisible = viewer.activity.menuVisible,
            cropBorders = cropBorders,
            navigator = viewer.config.navigator,
            onToggleMenu = remember(viewer) { { viewer.activity.toggleMenu() } },
            onNavigateAdjacent =
                remember(viewer) {
                    { forward -> if (forward) viewer.moveToNext() else viewer.moveToPrevious() }
                },
            onActivePanDelegateChanged = { delegate, active ->
                if (active) {
                    viewer.panDelegate = delegate
                } else if (viewer.panDelegate == delegate) {
                    viewer.panDelegate = null
                }
            },
            onPageLongTap = remember(viewer) { { p, ep -> viewer.activity.onPageLongTap(p, ep) } },
            onWidePageDetected = remember(viewer) { { p -> viewer.splitDoublePages(p) } },
        )

    val isActive = viewer.currentPagePosition == viewer.controller.findPageIndex(viewer.items, page)

    PagerPageItem(
        page = page,
        config = config,
        extraPage = extraPage,
        modifier = modifier,
        isActive = isActive,
    )
}

private object SmartFitContentScale : ContentScale {
    override fun computeScaleFactor(
        srcSize: ComposeSize,
        dstSize: ComposeSize,
    ): ScaleFactor {
        return if (
            srcSize.isSpecified && !srcSize.isEmpty() && dstSize.isSpecified && !dstSize.isEmpty()
        ) {
            val contentRatio = srcSize.width / srcSize.height
            val screenRatio = dstSize.width / dstSize.height
            if (contentRatio > screenRatio) {
                ContentScale.FillWidth.computeScaleFactor(srcSize, dstSize)
            } else {
                ContentScale.FillHeight.computeScaleFactor(srcSize, dstSize)
            }
        } else {
            ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
        }
    }
}
