package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    coroutineScope: CoroutineScope? = null,
    onToggleMenu: () -> Unit,
    onNavigateAdjacent: (forward: Boolean) -> Unit,
): Modifier = composed {
    val context = LocalContext.current
    val scope = coroutineScope ?: rememberCoroutineScope()
    val viewConfiguration = remember(context) { ViewConfiguration.get(context) }
    val touchSlopPx = remember(viewConfiguration) { viewConfiguration.scaledTouchSlop.toDouble() }
    val doubleTapSlopPx =
        remember(viewConfiguration) { viewConfiguration.scaledDoubleTapSlop.toDouble() }
    val doubleTapTimeoutMs = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }
    val longPressTimeoutMs = remember { ViewConfiguration.getLongPressTimeout().toLong() }

    val currentNavigator by rememberUpdatedState(navigator)
    val currentMenuVisible by rememberUpdatedState(menuVisible)
    val currentOnToggleMenu by rememberUpdatedState(onToggleMenu)
    val currentOnNavigateAdjacent by rememberUpdatedState(onNavigateAdjacent)

    pointerInput(enableDoubleTapZoom, doubleTapAnimDuration) {
        var lastTapTime = 0L
        var lastTapOffset = Offset.Zero
        var pendingSingleTapJob: Job? = null

        awaitEachGesture {
            val down =
                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
            val downPos = down.position
            val downTime = System.currentTimeMillis()
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
                if (moveDistance > touchSlopPx) {
                    // User moved beyond touch slop; cancel any pending single-tap action
                    pendingSingleTapJob?.cancel()
                    pendingSingleTapJob = null
                    break
                }
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

                if (
                    distance < touchSlopPx &&
                        (upTime - downTime < longPressTimeoutMs) &&
                        !up.isConsumed
                ) {
                    val screenWidth = size.width.toFloat()
                    val screenHeight = size.height.toFloat()

                    if (screenWidth > 0 && screenHeight > 0) {
                        val pos = PointF(upPos.x / screenWidth, upPos.y / screenHeight)
                        val action = currentNavigator.getAction(pos)

                        val isDoubleTap =
                            enableDoubleTapZoom &&
                                (upTime - lastTapTime < doubleTapTimeoutMs) &&
                                (hypot(
                                    (upPos.x - lastTapOffset.x).toDouble(),
                                    (upPos.y - lastTapOffset.y).toDouble(),
                                ) < doubleTapSlopPx) &&
                                (doubleTapAnimDuration > 0)

                        if (isDoubleTap) {
                            // Disambiguation success: cancel pending single tap so it never fires
                            pendingSingleTapJob?.cancel()
                            pendingSingleTapJob = null
                            lastTapTime = 0L
                            lastTapOffset = Offset.Zero

                            if (currentMenuVisible) {
                                currentOnToggleMenu()
                            }
                            scope.launch {
                                zoomState.toggleDoubleTapZoom(
                                    tapPos = upPos,
                                    viewportWidth = screenWidth,
                                    animDuration = doubleTapAnimDuration,
                                )
                            }
                        } else {
                            lastTapTime = upTime
                            lastTapOffset = upPos

                            if (enableDoubleTapZoom && doubleTapAnimDuration > 0) {
                                // Wait for potential second tap before executing single-tap action
                                pendingSingleTapJob?.cancel()
                                pendingSingleTapJob = scope.launch {
                                    delay(doubleTapTimeoutMs)
                                    dispatchTapAction(
                                        action = action,
                                        onToggleMenu = currentOnToggleMenu,
                                        onNavigateAdjacent = currentOnNavigateAdjacent,
                                    )
                                }
                            } else {
                                dispatchTapAction(
                                    action = action,
                                    onToggleMenu = currentOnToggleMenu,
                                    onNavigateAdjacent = currentOnNavigateAdjacent,
                                )
                            }
                        }
                    } else {
                        currentOnToggleMenu()
                    }
                }
            }
        }
    }
}

private fun dispatchTapAction(
    action: ViewerNavigation.NavigationRegion,
    onToggleMenu: () -> Unit,
    onNavigateAdjacent: (forward: Boolean) -> Unit,
) {
    when (action) {
        ViewerNavigation.NavigationRegion.MENU -> onToggleMenu()
        ViewerNavigation.NavigationRegion.NEXT,
        ViewerNavigation.NavigationRegion.RIGHT -> onNavigateAdjacent(true)
        ViewerNavigation.NavigationRegion.PREV,
        ViewerNavigation.NavigationRegion.LEFT -> onNavigateAdjacent(false)
    }
}
