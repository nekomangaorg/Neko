package org.nekomanga.presentation.screens.reader.viewer

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun WebtoonPageItem(
    viewer: WebtoonViewer,
    item: Any,
    modifier: Modifier = Modifier,
) {
    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val cropBordersWebtoon by readerPreferences.cropBordersWebtoon().collectAsState()
    val cropBorders by readerPreferences.cropBorders().collectAsState()
    val sidePadding by readerPreferences.webtoonSidePadding().collectAsState()
    val webtoonPageLayout by readerPreferences.webtoonPageLayout().collectAsState()
    val invertDoublePages by readerPreferences.webtoonInvertDoublePages().collectAsState()
    val readerTheme by readerPreferences.readerTheme().collectAsState()

    val pageHolder =
        remember(item, webtoonPageLayout, invertDoublePages, readerTheme) {
            val imageView =
                ReaderPageImageView(viewer.activity, isWebtoon = true).apply {
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                }
            WebtoonPageHolder(imageView, viewer).apply { bind(item) }
        }

    DisposableEffect(pageHolder) { onDispose { pageHolder.recycle() } }

    key(pageHolder) {
        Box(modifier = modifier.fillMaxWidth().heightIn(min = 400.dp)) {
            AndroidView(
                factory = { pageHolder.itemView },
                update = { holder ->
                    // Trigger layout & config update when observed preferences change
                    if (
                        sidePadding >= 0 &&
                            (cropBordersWebtoon || !cropBordersWebtoon) &&
                            (cropBorders || !cropBorders)
                    ) {
                        pageHolder.updateImageProperties()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
