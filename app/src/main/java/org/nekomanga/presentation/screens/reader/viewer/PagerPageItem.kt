package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.util.system.ThemeUtil
import kotlinx.coroutines.flow.emptyFlow
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun PagerPageItem(
    viewer: PagerViewer,
    page: ReaderPage,
    extraPage: ReaderPage? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val imageScaleType by readerPreferences.imageScaleType().collectAsState()
    val doublePageGap by readerPreferences.doublePageGap().collectAsState()
    val invertDoublePages by readerPreferences.invertDoublePages().collectAsState()
    val readerThemePref by readerPreferences.readerTheme().collectAsState()

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

    val contentScale =
        remember(imageScaleType) {
            when (imageScaleType) {
                1 -> ContentScale.Fit // Fit screen
                2 -> ContentScale.FillWidth // Fit width
                3 -> ContentScale.FillHeight // Fit height
                4 -> ContentScale.FillBounds // Stretch
                5 -> ContentScale.Inside // Original / Center
                else -> ContentScale.Fit
            }
        }

    val backgroundColor =
        remember(readerThemePref) {
            val theme = ReaderTheme.fromPreference(readerThemePref)
            when (theme) {
                ReaderTheme.SMART_BY_THEME -> Color.Transparent
                else -> Color(ThemeUtil.readerBackgroundColor(readerThemePref, context))
            }
        }

    val onRetry: () -> Unit = {
        page.chapter.pageLoader?.retryPage(page)
        extraPage?.chapter?.pageLoader?.retryPage(extraPage)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        if (extraPage == null) {
            val zoomableState = rememberZoomableState()
            val imageState = rememberZoomableImageState(zoomableState)

            ZoomableAsyncImage(
                model = ImageRequest.Builder(context).data(page).crossfade(true).build(),
                contentDescription = null,
                contentScale = contentScale,
                state = imageState,
                onClick = { offset ->
                    val pos =
                        PointF(
                            offset.x / widthPx,
                            offset.y / heightPx,
                        )
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
                onLongClick = {
                    if (viewer.activity.menuVisible || viewer.config.longTapEnabled) {
                        viewer.activity.onPageLongTap(page, null)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val first = if (invertDoublePages) extraPage else page
            val second = if (invertDoublePages) page else extraPage

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Size.tiny * doublePageGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(context).data(first).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onLongClick = {
                        if (viewer.activity.menuVisible || viewer.config.longTapEnabled) {
                            viewer.activity.onPageLongTap(page, extraPage)
                        }
                    },
                )
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(context).data(second).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onLongClick = {
                        if (viewer.activity.menuVisible || viewer.config.longTapEnabled) {
                            viewer.activity.onPageLongTap(page, extraPage)
                        }
                    },
                )
            }
        }

        ReaderPageLoadingOverlay(status = combinedStatus, progress = combinedProgress)

        ReaderPageErrorOverlay(visible = isError, onRetry = onRetry)
    }
}
