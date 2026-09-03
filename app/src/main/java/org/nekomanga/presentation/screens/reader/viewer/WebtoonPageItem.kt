package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.util.system.ThemeUtil
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun WebtoonPageItem(
    page: ReaderPage,
    modifier: Modifier = Modifier,
) {
    WebtoonPageContent(
        page = page,
        imageData = page,
        modifier = modifier,
    )
}

@Composable
fun WebtoonPageItem(
    split: ReaderPageSplit,
    modifier: Modifier = Modifier,
) {
    WebtoonPageContent(
        page = split.page,
        imageData = split,
        modifier = modifier,
    )
}

@Composable
private fun WebtoonPageContent(
    page: ReaderPage,
    imageData: Any,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val readerThemePref by readerPreferences.readerTheme().collectAsState()

    LaunchedEffect(page) { page.chapter.pageLoader?.loadPage(page) }

    val pageStatus by page.statusFlow.collectAsStateWithLifecycle(Page.State.QUEUE)
    val pageProgress by page.progressFlow.collectAsStateWithLifecycle(0)

    val isError = pageStatus == Page.State.ERROR

    val backgroundColor =
        remember(readerThemePref) {
            val theme = ReaderTheme.fromPreference(readerThemePref)
            when (theme) {
                ReaderTheme.SMART_BY_THEME -> Color.Transparent
                else -> Color(ThemeUtil.readerBackgroundColor(readerThemePref, context))
            }
        }

    val onRetry: () -> Unit = { page.chapter.pageLoader?.retryPage(page) }

    val model =
        remember(imageData) {
            ImageRequest.Builder(context).data(imageData).crossfade(true).build()
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Size.extraLarge * 10)
                .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )

        ReaderPageLoadingOverlay(status = pageStatus, progress = pageProgress)

        ReaderPageErrorOverlay(visible = isError, onRetry = onRetry)
    }
}
