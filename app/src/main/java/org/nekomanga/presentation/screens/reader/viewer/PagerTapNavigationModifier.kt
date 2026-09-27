package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import android.view.ViewConfiguration as AndroidViewConfiguration
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
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Modifier handling tap navigation, double-tap zoom disambiguation, and long-press actions for
 * paginated Compose reader pages ([PagerPageItem]).
 */
fun Modifier.pagerTapNavigation(
    config: PagerViewerConfigUiModel,
    page: ReaderPage,
    extraPage: ReaderPage? = null,
    coroutineScope: CoroutineScope? = null,
): Modifier = composed {
    val context = LocalContext.current
    val scope = coroutineScope ?: rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current
    val touchSlop = viewConfiguration.touchSlop
    val touchSlopSquared = touchSlop * touchSlop
    val doubleTapSlop =
        remember(context) { AndroidViewConfiguration.get(context).scaledDoubleTapSlop.toFloat() }
    val doubleTapSlopSquared = doubleTapSlop * doubleTapSlop
    val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis

    val currentConfig by rememberUpdatedState(config)
    val currentPage by rememberUpdatedState(page)
    val currentExtraPage by rememberUpdatedState(extraPage)

    pointerInput(
        page.chapter.chapter.id,
        page.index,
        extraPage?.chapter?.chapter?.id,
        extraPage?.index,
    ) {
        var lastTapTime = 0L
        var lastTapOffset = Offset.Zero
        var pendingNavJob: Job? = null
        var pendingNavAction: (() -> Unit)? = null

        try {
            awaitEachGesture {
                val down =
                    awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                val downPos = down.position
                var isLongPressTriggered = false
                var isMovementPastSlop = false
                var isMultiTouch = currentEvent.changes.count { it.pressed } > 1
                var pointerUp: PointerInputChange? = null

                if (!isMultiTouch) {
                    try {
                        withTimeout(longPressTimeoutMs) {
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                                // Cancel long-press evaluation immediately if multiple pointers
                                // active
                                if (event.changes.count { it.pressed } > 1) {
                                    isMultiTouch = true
                                    break
                                }

                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val moveDistanceSquared =
                                    (change.position - downPos).getDistanceSquared()
                                if (moveDistanceSquared > touchSlopSquared) {
                                    isMovementPastSlop = true
                                    break
                                }
                                if (!change.pressed) {
                                    pointerUp = change
                                    break
                                }
                            }
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (
                            !isMovementPastSlop &&
                                !isMultiTouch &&
                                (currentConfig.menuVisible || currentConfig.longTapEnabled)
                        ) {
                            currentConfig.onPageLongTap?.invoke(currentPage, currentExtraPage)
                            isLongPressTriggered = true
                        }
                        while (currentEvent.changes.any { it.pressed }) {
                            awaitPointerEvent(pass = PointerEventPass.Initial)
                        }
                    }
                }

                if ((pointerUp == null || isMultiTouch) && !isLongPressTriggered) {
                    while (currentEvent.changes.any { it.pressed }) {
                        awaitPointerEvent(pass = PointerEventPass.Initial)
                    }
                }

                if (isMovementPastSlop || isMultiTouch || isLongPressTriggered) {
                    pendingNavJob?.cancel()
                    pendingNavJob = null
                    pendingNavAction = null
                    lastTapTime = 0L
                    lastTapOffset = Offset.Zero
                }

                if (
                    !isLongPressTriggered &&
                        !isMovementPastSlop &&
                        !isMultiTouch &&
                        pointerUp != null
                ) {
                    val up = pointerUp!!
                    val upPos = up.position
                    val upTime = System.currentTimeMillis()
                    val distanceSquared = (upPos - downPos).getDistanceSquared()

                    if (distanceSquared < touchSlopSquared) {
                        val screenWidth = size.width.toFloat()
                        val screenHeight = size.height.toFloat()

                        if (screenWidth > 0 && screenHeight > 0) {
                            val pos = PointF(upPos.x / screenWidth, upPos.y / screenHeight)
                            val action = currentConfig.navigator.getAction(pos)

                            val tapDistanceSquared = (upPos - lastTapOffset).getDistanceSquared()
                            val isDoubleTap =
                                (upTime - lastTapTime < doubleTapTimeoutMs) &&
                                    (tapDistanceSquared < doubleTapSlopSquared) &&
                                    (currentConfig.doubleTapAnimDuration > 0)

                            if (isDoubleTap) {
                                // Double-tap detected: cancel pending single-tap navigation
                                // immediately
                                pendingNavJob?.cancel()
                                pendingNavJob = null
                                pendingNavAction = null

                                if (currentConfig.menuVisible) {
                                    currentConfig.onToggleMenu()
                                }
                                lastTapTime = 0L
                                lastTapOffset = Offset.Zero
                            } else {
                                // Flush previous pending tap if a new distinct tap arrives
                                if (pendingNavJob?.isActive == true) {
                                    pendingNavJob?.cancel()
                                    pendingNavJob = null
                                    pendingNavAction?.invoke()
                                    pendingNavAction = null
                                }

                                lastTapTime = upTime
                                lastTapOffset = upPos

                                val executeNav: () -> Unit = {
                                    dispatchNavigation(
                                        action = action,
                                        isRtl = currentConfig.isRtl,
                                        menuVisible = currentConfig.menuVisible,
                                        onToggleMenu = currentConfig.onToggleMenu,
                                        onNavigateAdjacent = currentConfig.onNavigateAdjacent,
                                    )
                                    lastTapTime = 0L
                                    lastTapOffset = Offset.Zero
                                }

                                if (currentConfig.doubleTapAnimDuration > 0) {
                                    pendingNavAction = executeNav
                                    pendingNavJob = scope.launch {
                                        delay(doubleTapTimeoutMs)
                                        executeNav()
                                        pendingNavJob = null
                                        pendingNavAction = null
                                    }
                                } else {
                                    executeNav()
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            if (pendingNavJob?.isActive == true) {
                pendingNavAction?.invoke()
            }
            pendingNavJob?.cancel()
            pendingNavJob = null
            pendingNavAction = null
        }
    }
}

internal fun dispatchNavigation(
    action: NavigationRegion,
    isRtl: Boolean,
    menuVisible: Boolean,
    onToggleMenu: () -> Unit,
    onNavigateAdjacent: (forward: Boolean) -> Unit,
) {
    if (action == NavigationRegion.MENU) {
        onToggleMenu()
        return
    }
    if (menuVisible) {
        onToggleMenu()
    }
    val forward =
        when (action) {
            NavigationRegion.NEXT -> true
            NavigationRegion.PREV -> false
            NavigationRegion.RIGHT -> !isRtl
            NavigationRegion.LEFT -> isRtl
        }
    onNavigateAdjacent(forward)
}
