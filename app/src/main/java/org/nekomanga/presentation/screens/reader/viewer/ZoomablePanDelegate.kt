package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPanDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.zoomable.Viewport
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.spatial.CoordinateSpace

/** Implementation of [PagerPanDelegate] backed by a Telephoto [ZoomableState]. */
class ZoomablePanDelegate(
    private val zoomableState: ZoomableState,
    private val viewportWidthPx: Float,
    private val viewportHeightPx: Float,
    private val scope: CoroutineScope,
    private val animated: Boolean = true,
    private val animationDurationMillis: Int = 250,
    private val panEpsilonPx: Float = 5f,
    private val contentBoundsProvider: () -> Rect = {
        @OptIn(ExperimentalTelephotoApi::class)
        with(zoomableState.coordinateSystem) {
            contentBounds(clipToViewport = false).rectIn(CoordinateSpace.Viewport)
        }
    },
) : PagerPanDelegate {

    private var panJob: Job? = null

    private val contentBounds: Rect
        get() = contentBoundsProvider()

    private val animationSpec: AnimationSpec<Offset>
        get() =
            if (animated) {
                tween(
                    durationMillis = animationDurationMillis.coerceAtLeast(50),
                    easing = FastOutSlowInEasing,
                )
            } else {
                snap()
            }

    override fun canPanLeft(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.left < -panEpsilonPx
    }

    override fun canPanRight(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.right > viewportWidthPx + panEpsilonPx
    }

    override fun canPanUp(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.top < -panEpsilonPx
    }

    override fun canPanDown(): Boolean {
        val bounds = contentBounds
        return !bounds.isEmpty && bounds.bottom > viewportHeightPx + panEpsilonPx
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
                    animationSpec = animationSpec,
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
                    animationSpec = animationSpec,
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
                    animationSpec = animationSpec,
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
                    animationSpec = animationSpec,
                )
            }
        }
    }
}
