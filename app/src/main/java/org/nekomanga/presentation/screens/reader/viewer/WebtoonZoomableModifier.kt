package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal const val ZOOM_INTERACTION_THRESHOLD = 1.01f

/**
 * State holder managing scale, horizontal pan offset, and animations for the continuous Webtoon
 * viewer.
 */
@Stable
class WebtoonZoomState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
) {
    var scale by mutableFloatStateOf(initialScale)
    var offsetX by mutableFloatStateOf(initialOffsetX)

    internal val scrollConnection: NestedScrollConnection = WebtoonZoomScrollConnection(this)

    private var flingJob: Job? = null

    fun cancelFling() {
        flingJob?.cancel()
        flingJob = null
    }

    suspend fun reset() {
        cancelFling()
        scale = 1f
        offsetX = 0f
    }

    fun flingHorizontal(
        coroutineScope: CoroutineScope,
        velocityX: Float,
        maxOffsetX: Float,
    ) {
        cancelFling()
        flingJob = coroutineScope.launch {
            val animX = Animatable(offsetX)
            val targetX = (offsetX + velocityX * 0.25f).coerceIn(-maxOffsetX, maxOffsetX)
            animX.animateTo(
                targetValue = targetX,
                animationSpec =
                    tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing,
                    ),
            ) {
                offsetX = value
            }
        }
    }

    suspend fun toggleDoubleTapZoom(
        tapPos: Offset,
        viewportWidth: Float,
        animDuration: Int = 300,
    ) {
        cancelFling()
        val duration = animDuration.coerceAtLeast(100)
        if (scale > ZOOM_INTERACTION_THRESHOLD || scale < 0.95f) {
            coroutineScope {
                val animScale = Animatable(scale)
                val animX = Animatable(offsetX)
                launch { animScale.animateTo(1f, tween(duration)) { scale = value } }
                launch { animX.animateTo(0f, tween(duration)) { offsetX = value } }
            }
        } else {
            val targetScale = 2.5f
            val targetX = ((viewportWidth / 2f) - tapPos.x) * (targetScale - 1f)
            val maxOffsetX = (viewportWidth * (targetScale - 1f)) / 2f
            val boundedX = targetX.coerceIn(-maxOffsetX, maxOffsetX)

            coroutineScope {
                val animScale = Animatable(scale)
                val animX = Animatable(offsetX)
                launch { animScale.animateTo(targetScale, tween(duration)) { scale = value } }
                launch { animX.animateTo(boundedX, tween(duration)) { offsetX = value } }
            }
        }
    }
}

@Composable fun rememberWebtoonZoomState(): WebtoonZoomState = remember { WebtoonZoomState() }

/**
 * Modifier extracting multi-touch pinch-to-zoom, horizontal panning, velocity tracking, and fling
 * deceleration physics.
 */
fun Modifier.webtoonZoomable(
    state: WebtoonZoomState,
    enableZoomOut: Boolean = false,
    coroutineScope: CoroutineScope,
): Modifier =
    this.graphicsLayer {
            scaleX = state.scale
            scaleY = state.scale
            translationX = state.offsetX
            translationY = 0f
        }
        .nestedScroll(state.scrollConnection)
        .pointerInput(enableZoomOut) {
            val velocityTracker = VelocityTracker()
            awaitEachGesture {
                val minScale = if (enableZoomOut) 0.5f else 1f
                val maxScale = 3f

                val down = awaitFirstDown(requireUnconsumed = false)
                state.cancelFling()
                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val activePointers = event.changes.filter { it.pressed }

                    if (activePointers.size >= 2) {
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        val newScale = (state.scale * zoomChange).coerceIn(minScale, maxScale)
                        state.scale = newScale

                        if (newScale > 1f) {
                            val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                            state.offsetX =
                                (state.offsetX + panChange.x).coerceIn(
                                    -maxOffsetX,
                                    maxOffsetX,
                                )
                        } else {
                            state.offsetX = 0f
                        }

                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    } else if (activePointers.size == 1) {
                        val change = activePointers.first()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        if (state.scale > ZOOM_INTERACTION_THRESHOLD) {
                            val panX = change.position.x - change.previousPosition.x
                            if (panX != 0f) {
                                val maxOffsetX = (size.width * (state.scale - 1f)) / 2f
                                state.offsetX =
                                    (state.offsetX + panX).coerceIn(-maxOffsetX, maxOffsetX)
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })

                if (state.scale < 1f && !enableZoomOut) {
                    coroutineScope.launch {
                        val animScale = Animatable(state.scale)
                        val animX = Animatable(state.offsetX)
                        launch { animScale.animateTo(1f, tween(200)) { state.scale = value } }
                        launch { animX.animateTo(0f, tween(200)) { state.offsetX = value } }
                    }
                } else if (state.scale > ZOOM_INTERACTION_THRESHOLD) {
                    val velocity = velocityTracker.calculateVelocity()
                    val velocityX = velocity.x
                    if (abs(velocityX) > 100f) {
                        val maxOffsetX = (size.width * (state.scale - 1f)) / 2f
                        state.flingHorizontal(coroutineScope, velocityX, maxOffsetX)
                    }
                }
            }
        }

/**
 * NestedScrollConnection that scales vertical user dragging and fling velocities by (1 / scale)
 * when zoomed in, ensuring that vertical content movement directly tracks the finger 1:1 on screen
 * and maintains the same touch sensitivity as horizontal panning.
 */
internal class WebtoonZoomScrollConnection(
    private val state: WebtoonZoomState,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && state.scale > ZOOM_INTERACTION_THRESHOLD) {
            val consumedY = calculateZoomScrollConsumption(available.y, state.scale)
            return Offset(0f, consumedY)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.scale > ZOOM_INTERACTION_THRESHOLD) {
            val consumedY = calculateZoomScrollConsumption(available.y, state.scale)
            return Velocity(0f, consumedY)
        }
        return Velocity.Zero
    }
}

/**
 * Calculates the amount of scroll delta to consume before passing to LazyColumn so that when
 * LazyColumn is rendered with [scale], the visual movement on screen matches the user's finger
 * movement 1:1 (rather than moving [scale] times faster).
 */
internal fun calculateZoomScrollConsumption(availableDelta: Float, scale: Float): Float =
    if (scale > ZOOM_INTERACTION_THRESHOLD) availableDelta * (1f - 1f / scale) else 0f
