package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPanDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomableState

/** Implementation of [PagerPanDelegate] backed by a Telephoto [ZoomableState]. */
class ZoomablePanDelegate(
    private val zoomableState: ZoomableState,
    private val viewportWidthPx: Float,
    private val viewportHeightPx: Float,
    private val scope: CoroutineScope,
) : PagerPanDelegate {

    private var panJob: Job? = null

    @Suppress("DEPRECATION")
    private val contentBounds: Rect
        get() = zoomableState.transformedContentBounds

    override fun canPanLeft(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.left < -PAN_EPSILON
    }

    override fun canPanRight(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.right > viewportWidthPx + PAN_EPSILON
    }

    override fun canPanUp(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.top < -PAN_EPSILON
    }

    override fun canPanDown(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.bottom > viewportHeightPx + PAN_EPSILON
    }

    override fun panLeft() {
        val bounds = contentBounds
        if (bounds.isEmpty) return
        val panAmount = minOf(viewportWidthPx, -bounds.left)
        if (panAmount > 1f) {
            panJob?.cancel()
            panJob = scope.launch {
                zoomableState.panBy(
                    offset = Offset(panAmount, 0f),
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    override fun panRight() {
        val bounds = contentBounds
        if (bounds.isEmpty) return
        val panAmount = minOf(viewportWidthPx, bounds.right - viewportWidthPx)
        if (panAmount > 1f) {
            panJob?.cancel()
            panJob = scope.launch {
                zoomableState.panBy(
                    offset = Offset(-panAmount, 0f),
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    override fun panUp() {
        val bounds = contentBounds
        if (bounds.isEmpty) return
        val panAmount = minOf(viewportHeightPx, -bounds.top)
        if (panAmount > 1f) {
            panJob?.cancel()
            panJob = scope.launch {
                zoomableState.panBy(
                    offset = Offset(0f, panAmount),
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    override fun panDown() {
        val bounds = contentBounds
        if (bounds.isEmpty) return
        val panAmount = minOf(viewportHeightPx, bounds.bottom - viewportHeightPx)
        if (panAmount > 1f) {
            panJob?.cancel()
            panJob = scope.launch {
                zoomableState.panBy(
                    offset = Offset(0f, -panAmount),
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    private companion object {
        private const val PAN_EPSILON = 5f
    }
}
