package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size as CoilSize
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.settings.ReaderTheme
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.ThemeUtil
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Strongly typed target for webtoon rendering to avoid untyped Any? smuggling. */
sealed interface WebtoonImageTarget {
    data class Page(val page: ReaderPage) : WebtoonImageTarget

    data class Slice(val split: ReaderPageSplit) : WebtoonImageTarget
}

@Composable
fun WebtoonPageItem(
    page: ReaderPage,
    onCheckAndSplitPage: (suspend (ReaderPage) -> Boolean)? = null,
    isAlreadyChecked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isSplitCheckRequired = onCheckAndSplitPage != null && !isAlreadyChecked
    var isSplitChecked by remember(page) { mutableStateOf(!isSplitCheckRequired) }

    LaunchedEffect(page) { page.chapter.pageLoader?.loadPage(page) }

    val pageStatus by page.statusFlow.collectAsStateWithLifecycle(Page.State.QUEUE)
    val pageProgress by page.progressFlow.collectAsStateWithLifecycle(0)

    LaunchedEffect(page, pageStatus) {
        if (pageStatus == Page.State.READY && isSplitCheckRequired && !isSplitChecked) {
            val wasSplit = onCheckAndSplitPage(page)
            if (!wasSplit) {
                isSplitChecked = true
            }
        } else if (pageStatus == Page.State.ERROR) {
            isSplitChecked = true
        }
    }

    WebtoonPageContent(
        page = page,
        initialRatio = page.aspectRatio,
        onRatioCalculated = { ratio, _ -> page.aspectRatio = ratio },
        target = if (isSplitChecked) WebtoonImageTarget.Page(page) else null,
        pageStatus = pageStatus,
        pageProgress = pageProgress,
        modifier = modifier,
    )
}

@Composable
fun WebtoonPageItem(
    split: ReaderPageSplit,
    modifier: Modifier = Modifier,
) {
    val page = split.page
    LaunchedEffect(page) { page.chapter.pageLoader?.loadPage(page) }

    val pageStatus by page.statusFlow.collectAsStateWithLifecycle(Page.State.QUEUE)
    val pageProgress by page.progressFlow.collectAsStateWithLifecycle(0)

    WebtoonPageContent(
        page = page,
        initialRatio = split.aspectRatio,
        onRatioCalculated = { ratio, height ->
            split.aspectRatio = ratio
            split.displayedHeight = height
        },
        target = WebtoonImageTarget.Slice(split),
        pageStatus = pageStatus,
        pageProgress = pageProgress,
        modifier = modifier,
    )
}

@Composable
private fun WebtoonPageContent(
    page: ReaderPage,
    initialRatio: Float,
    onRatioCalculated: (Float, Int) -> Unit,
    target: WebtoonImageTarget?,
    pageStatus: Page.State,
    pageProgress: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val readerThemePref by readerPreferences.readerTheme().collectAsState()

    val isError = pageStatus == Page.State.ERROR

    var intrinsicRatio by remember(target) { mutableFloatStateOf(initialRatio) }

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
        remember(target) {
            val modelData =
                when (target) {
                    is WebtoonImageTarget.Page -> target.page
                    is WebtoonImageTarget.Slice -> target.split
                    null -> null
                }
            ImageRequest.Builder(context)
                .data(modelData)
                .size(CoilSize.ORIGINAL)
                .maxBitmapSize(CoilSize(GLUtil.maxTextureSize, GLUtil.maxTextureSize))
                .precision(Precision.EXACT)
                .crossfade(true)
                .build()
        }

    val sizeModifier =
        if (intrinsicRatio > 0f) {
            Modifier.aspectRatio(intrinsicRatio)
        } else {
            Modifier.heightIn(min = Size.extraLarge * 10)
        }

    Box(
        modifier = modifier.fillMaxWidth().then(sizeModifier).background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            filterQuality = FilterQuality.High,
            modifier =
                Modifier.fillMaxWidth()
                    .then(
                        if (intrinsicRatio > 0f) Modifier.aspectRatio(intrinsicRatio) else Modifier
                    ),
            onSuccess = { state ->
                val img = state.result.image
                if (img.width > 0 && img.height > 0) {
                    val ratio = img.width.toFloat() / img.height.toFloat()
                    intrinsicRatio = ratio
                    onRatioCalculated(ratio, img.height)
                }
            },
        )

        ReaderPageLoadingOverlay(status = pageStatus, progress = pageProgress)

        ReaderPageErrorOverlay(visible = isError, onRetry = onRetry)
    }
}
