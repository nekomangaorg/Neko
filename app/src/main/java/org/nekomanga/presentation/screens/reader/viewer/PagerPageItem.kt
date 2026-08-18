package org.nekomanga.presentation.screens.reader.viewer

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
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

    val pageHolder =
        remember(page, extraPage) {
            PagerPageHolder(viewer, page, extraPage).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
        }

    DisposableEffect(pageHolder) { onDispose { pageHolder.recycle() } }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { pageHolder },
            update = { holder ->
                // Trigger image properties refresh when any of the observed preferences change
                if (imageScaleType > 0 && zoomStart > 0) {
                    holder.updateImageProperties()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
