package org.nekomanga.presentation.screens.reader.viewer

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer

@Composable
fun WebtoonPageItem(
    viewer: WebtoonViewer,
    item: Any,
    modifier: Modifier = Modifier,
) {
    val pageHolder =
        remember(item) {
            val imageView =
                ReaderPageImageView(viewer.activity, isWebtoon = true).apply {
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                }
            WebtoonPageHolder(imageView, viewer).apply { bind(item) }
        }

    DisposableEffect(pageHolder) { onDispose { pageHolder.recycle() } }

    Box(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            factory = { pageHolder.itemView },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
