package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Size
import coil3.ImageLoader
import coil3.compose.asPainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transitionFactory
import coil3.transition.CrossfadeTransition
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import me.saket.telephoto.zoomable.ZoomableImageSource
import me.saket.telephoto.zoomable.ZoomableImageSource.PainterDelegate
import me.saket.telephoto.zoomable.ZoomableImageSource.ResolveResult

/**
 * Loads a reader page for `ZoomableImage` with a single request.
 *
 * telephoto's Coil source loads the image a second time, skipping the memory cache, whenever it
 * cannot sub-sample the result. A ReaderPage never has a file or disk cache entry to sub-sample, so
 * with that source every page loads twice and a memory cache hit still ends in a fresh decode.
 *
 * This source runs the request as built, so the request must set its own size limits.
 */
internal class ReaderPageImageSource(
    private val request: ImageRequest,
    private val imageLoader: ImageLoader,
) : ZoomableImageSource {

    @Composable
    override fun resolve(canvasSize: Flow<Size>): ResolveResult {
        return produceState(ResolveResult(delegate = null), request, imageLoader) {
                value = loadReaderPageImage(imageLoader, request)
            }
            .value
    }
}

/** Executes [request] once and wraps the result the same way telephoto's Coil source does. */
internal suspend fun loadReaderPageImage(
    imageLoader: ImageLoader,
    request: ImageRequest,
): ResolveResult {
    val result = imageLoader.execute(request)
    val transitionFactory = request.transitionFactory
    return ResolveResult(
        delegate = PainterDelegate(result.image?.asPainter(request.context)),
        crossfadeDuration =
            if (result is SuccessResult && transitionFactory is CrossfadeTransition.Factory) {
                transitionFactory.durationMillis.milliseconds
            } else {
                Duration.ZERO
            },
    )
}
