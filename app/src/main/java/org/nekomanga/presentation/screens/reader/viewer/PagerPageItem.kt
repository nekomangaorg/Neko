package org.nekomanga.presentation.screens.reader.viewer

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun PagerPageItem(
    viewer: PagerViewer,
    page: ReaderPage,
    extraPage: ReaderPage? = null,
    modifier: Modifier = Modifier,
) {
    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val imageScaleType by readerPreferences.imageScaleType().collectAsState()
    val zoomStart by readerPreferences.zoomStart().collectAsState()
    val cropBorders by readerPreferences.cropBorders().collectAsState()
    val landscapeZoom by readerPreferences.landscapeZoom().collectAsState()
    val doublePageGap by readerPreferences.doublePageGap().collectAsState()
    val invertDoublePages by readerPreferences.invertDoublePages().collectAsState()
    val readerTheme by readerPreferences.readerTheme().collectAsState()

    val gapKey = if (extraPage != null) doublePageGap else 0
    val invertKey = if (extraPage != null) invertDoublePages else false

    val pageHolder =
        remember(page, extraPage, gapKey, invertKey, readerTheme) {
            PagerPageHolder(viewer, page, extraPage).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
        }

    DisposableEffect(pageHolder) { onDispose { pageHolder.recycle() } }

    val zoomType =
        remember(zoomStart, viewer) {
            when (zoomStart) {
                1 ->
                    when (viewer) {
                        is L2RPagerViewer -> PagerConfig.ZoomType.Left
                        is R2LPagerViewer -> PagerConfig.ZoomType.Right
                        else -> PagerConfig.ZoomType.Center
                    }
                2 -> PagerConfig.ZoomType.Left
                3 -> PagerConfig.ZoomType.Right
                else -> PagerConfig.ZoomType.Center
            }
        }

    key(pageHolder) {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { pageHolder },
                update = { holder ->
                    holder.updateReaderTheme(readerTheme)
                    holder.updateImageConfig(
                        holder.createConfig(
                            scaleType = imageScaleType,
                            cropBorders = cropBorders,
                            zoomType = zoomType,
                            landscapeZoom = landscapeZoom,
                        )
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
