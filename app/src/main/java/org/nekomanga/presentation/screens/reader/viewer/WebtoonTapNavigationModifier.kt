package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import kotlin.math.hypot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Modifier handling tap navigation regions, double-tap zoom gestures, and menu toggles for the
 * continuous Webtoon reader.
 */
fun Modifier.webtoonTapNavigation(
    navigator: ViewerNavigation,
    zoomState: WebtoonZoomState,
    enableDoubleTapZoom: Boolean = true,
    doubleTapAnimDuration: Int = 300,
    menuVisible: Boolean = false,
    coroutineScope: CoroutineScope,
    onToggleMenu: () -> Unit,
    onNavigateAdjacent: (forward: Boolean) -> Unit,
): Modifier = composed {
    val context = LocalContext.current
    val viewConfiguration = remember(context) { ViewConfiguration.get(context) }
    val touchSlopPx = remember(viewConfiguration) { viewConfiguration.scaledTouchSlop.toDouble() }
    val doubleTapSlopPx =
        remember(viewConfiguration) { viewConfiguration.scaledDoubleTapSlop.toDouble() }
    val doubleTapTimeoutMs = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }

    pointerInput(navigator, enableDoubleTapZoom, doubleTapAnimDuration, menuVisible) {
        var lastTapTime = 0L
        var lastTapOffset = Offset.Zero

        awaitEachGesture {
            val down =
                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
            val downPos = down.position
            var pointerUp: PointerInputChange? = null

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    pointerUp = change
                    break
                }
                val moveDistance =
                    hypot(
                        (change.position.x - downPos.x).toDouble(),
                        (change.position.y - downPos.y).toDouble(),
                    ) * zoomState.scale
                if (moveDistance > touchSlopPx) break
            }

            if (pointerUp != null) {
                val up = pointerUp
                val upPos = up.position
                val upTime = System.currentTimeMillis()
                val distance =
                    hypot(
                        (upPos.x - downPos.x).toDouble(),
                        (upPos.y - downPos.y).toDouble(),
                    )

                if (distance < touchSlopPx) {
                    val screenWidth = size.width.toFloat()
                    val screenHeight = size.height.toFloat()

                    if (screenWidth > 0 && screenHeight > 0) {
                        val pos = PointF(upPos.x / screenWidth, upPos.y / screenHeight)
                        val action = navigator.getAction(pos)

                        val isDoubleTap =
                            enableDoubleTapZoom &&
                                (upTime - lastTapTime < doubleTapTimeoutMs) &&
                                (hypot(
                                    (upPos.x - lastTapOffset.x).toDouble(),
                                    (upPos.y - lastTapOffset.y).toDouble(),
                                ) < doubleTapSlopPx) &&
                                (doubleTapAnimDuration > 0)

                        if (isDoubleTap) {
                            if (menuVisible) {
                                onToggleMenu()
                            }
                            lastTapTime = 0L
                            lastTapOffset = Offset.Zero
                            coroutineScope.launch {
                                zoomState.toggleDoubleTapZoom(
                                    tapPos = upPos,
                                    viewportWidth = size.width.toFloat(),
                                    animDuration = doubleTapAnimDuration,
                                )
                            }
                        } else {
                            lastTapTime = upTime
                            lastTapOffset = upPos
                            when (action) {
                                ViewerNavigation.NavigationRegion.MENU -> onToggleMenu()
                                ViewerNavigation.NavigationRegion.NEXT,
                                ViewerNavigation.NavigationRegion.RIGHT -> onNavigateAdjacent(true)
                                ViewerNavigation.NavigationRegion.PREV,
                                ViewerNavigation.NavigationRegion.LEFT -> onNavigateAdjacent(false)
                            }
                        }
                    } else {
                        onToggleMenu()
                    }
                }
            }
        }
    }
}
