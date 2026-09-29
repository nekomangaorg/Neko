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
                val downTime = System.currentTimeMillis()

                when (
                    evaluateDownPointer(
                        hasActivePendingJob = pendingNavJob?.isActive == true,
                        downPos = downPos,
                        lastTapOffset = lastTapOffset,
                        downTime = downTime,
                        lastTapTime = lastTapTime,
                        doubleTapTimeoutMs = doubleTapTimeoutMs,
                        doubleTapSlopSquared = doubleTapSlopSquared,
                    )
                ) {
                    DownPointerAction.CANCEL_PENDING_FOR_DOUBLE_TAP -> {
                        pendingNavJob?.cancel()
                        pendingNavJob = null
                        pendingNavAction = null
                    }
                    DownPointerAction.FLUSH_PENDING_AND_START_NEW -> {
                        pendingNavJob?.cancel()
                        pendingNavJob = null
                        pendingNavAction?.invoke()
                        pendingNavAction = null
                        lastTapTime = 0L
                        lastTapOffset = Offset.Zero
                    }
                    DownPointerAction.NO_OP -> {}
                }

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
                                val slopResult =
                                    evaluatePointerMovement(
                                        downPos = downPos,
                                        currentPos = change.position,
                                        isPressed = change.pressed,
                                        touchSlopSquared = touchSlopSquared,
                                    )
                                when (slopResult) {
                                    PointerSlopResult.VALID_TAP_UP -> {
                                        pointerUp = change
                                        break
                                    }
                                    PointerSlopResult.MOVEMENT_PAST_SLOP -> {
                                        isMovementPastSlop = true
                                        break
                                    }
                                    PointerSlopResult.WITHIN_SLOP_PRESSED -> {
                                        // Still within touch slop, continue gesture tracking
                                    }
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

                    if (distanceSquared <= touchSlopSquared) {
                        val screenWidth = size.width.toFloat()
                        val screenHeight = size.height.toFloat()

                        if (screenWidth > 0 && screenHeight > 0) {
                            val pos = PointF(upPos.x / screenWidth, upPos.y / screenHeight)
                            val action = currentConfig.navigator.getAction(pos)

                            val isDouble =
                                isDoubleTap(
                                    upTime = upTime,
                                    lastTapTime = lastTapTime,
                                    doubleTapTimeoutMs = doubleTapTimeoutMs,
                                    upPos = upPos,
                                    lastTapOffset = lastTapOffset,
                                    doubleTapSlopSquared = doubleTapSlopSquared,
                                    hasDoubleTapAnimation = currentConfig.doubleTapAnimDuration > 0,
                                )

                            when (
                                resolveTapAction(
                                    action = action,
                                    menuVisible = currentConfig.menuVisible,
                                    isDoubleTap = isDouble,
                                    hasDoubleTapAnimation = currentConfig.doubleTapAnimDuration > 0,
                                )
                            ) {
                                TapActionResolution.TOGGLE_MENU_IMMEDIATE -> {
                                    pendingNavJob?.cancel()
                                    pendingNavJob = null
                                    pendingNavAction = null
                                    currentConfig.onToggleMenu()
                                    lastTapTime = 0L
                                    lastTapOffset = Offset.Zero
                                }
                                TapActionResolution.CANCEL_MENU_FOR_ZOOM -> {
                                    pendingNavJob?.cancel()
                                    pendingNavJob = null
                                    pendingNavAction = null
                                    lastTapTime = 0L
                                    lastTapOffset = Offset.Zero
                                }
                                TapActionResolution.SCHEDULE_MENU_DELAYED -> {
                                    if (pendingNavJob?.isActive == true) {
                                        pendingNavJob?.cancel()
                                        pendingNavJob = null
                                        pendingNavAction?.invoke()
                                        pendingNavAction = null
                                    }

                                    lastTapTime = upTime
                                    lastTapOffset = upPos

                                    val executeMenu: () -> Unit = {
                                        currentConfig.onToggleMenu()
                                        lastTapTime = 0L
                                        lastTapOffset = Offset.Zero
                                    }

                                    pendingNavAction = executeMenu
                                    pendingNavJob = scope.launch {
                                        delay(doubleTapTimeoutMs)
                                        executeMenu()
                                        pendingNavJob = null
                                        pendingNavAction = null
                                    }
                                }
                                TapActionResolution.CANCEL_NAV_FOR_ZOOM -> {
                                    pendingNavJob?.cancel()
                                    pendingNavJob = null
                                    pendingNavAction = null
                                    lastTapTime = 0L
                                    lastTapOffset = Offset.Zero
                                }
                                TapActionResolution.SCHEDULE_NAV_DELAYED -> {
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

                                    pendingNavAction = executeNav
                                    pendingNavJob = scope.launch {
                                        delay(doubleTapTimeoutMs)
                                        executeNav()
                                        pendingNavJob = null
                                        pendingNavAction = null
                                    }
                                }
                                TapActionResolution.NAVIGATE_IMMEDIATELY -> {
                                    up.consume()

                                    if (pendingNavJob?.isActive == true) {
                                        pendingNavJob?.cancel()
                                        pendingNavJob = null
                                        pendingNavAction?.invoke()
                                        pendingNavAction = null
                                    }
                                    lastTapTime = 0L
                                    lastTapOffset = Offset.Zero

                                    dispatchNavigation(
                                        action = action,
                                        isRtl = currentConfig.isRtl,
                                        menuVisible = currentConfig.menuVisible,
                                        onToggleMenu = currentConfig.onToggleMenu,
                                        onNavigateAdjacent = currentConfig.onNavigateAdjacent,
                                    )
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

internal enum class PointerSlopResult {
    VALID_TAP_UP,
    MOVEMENT_PAST_SLOP,
    WITHIN_SLOP_PRESSED,
}

internal fun evaluatePointerMovement(
    downPos: Offset,
    currentPos: Offset,
    isPressed: Boolean,
    touchSlopSquared: Float,
): PointerSlopResult {
    val distanceSquared = (currentPos - downPos).getDistanceSquared()
    return if (!isPressed) {
        if (distanceSquared <= touchSlopSquared) {
            PointerSlopResult.VALID_TAP_UP
        } else {
            PointerSlopResult.MOVEMENT_PAST_SLOP
        }
    } else {
        if (distanceSquared > touchSlopSquared) {
            PointerSlopResult.MOVEMENT_PAST_SLOP
        } else {
            PointerSlopResult.WITHIN_SLOP_PRESSED
        }
    }
}

internal enum class DownPointerAction {
    CANCEL_PENDING_FOR_DOUBLE_TAP,
    FLUSH_PENDING_AND_START_NEW,
    NO_OP,
}

internal fun evaluateDownPointer(
    hasActivePendingJob: Boolean,
    downPos: Offset,
    lastTapOffset: Offset,
    downTime: Long,
    lastTapTime: Long,
    doubleTapTimeoutMs: Long,
    doubleTapSlopSquared: Float,
): DownPointerAction {
    if (!hasActivePendingJob) return DownPointerAction.NO_OP
    val timeSinceLastTap = downTime - lastTapTime
    val distToLastTapSq = (downPos - lastTapOffset).getDistanceSquared()
    return if (
        timeSinceLastTap in 0 until doubleTapTimeoutMs && distToLastTapSq < doubleTapSlopSquared
    ) {
        DownPointerAction.CANCEL_PENDING_FOR_DOUBLE_TAP
    } else {
        DownPointerAction.FLUSH_PENDING_AND_START_NEW
    }
}

internal enum class TapActionResolution {
    TOGGLE_MENU_IMMEDIATE,
    SCHEDULE_MENU_DELAYED,
    CANCEL_MENU_FOR_ZOOM,
    NAVIGATE_IMMEDIATELY,
    SCHEDULE_NAV_DELAYED,
    CANCEL_NAV_FOR_ZOOM,
}

internal fun resolveTapAction(
    action: NavigationRegion,
    menuVisible: Boolean,
    isDoubleTap: Boolean,
    hasDoubleTapAnimation: Boolean,
): TapActionResolution {
    return if (action == NavigationRegion.MENU) {
        if (menuVisible) {
            TapActionResolution.TOGGLE_MENU_IMMEDIATE
        } else if (isDoubleTap) {
            TapActionResolution.CANCEL_MENU_FOR_ZOOM
        } else if (hasDoubleTapAnimation) {
            TapActionResolution.SCHEDULE_MENU_DELAYED
        } else {
            TapActionResolution.TOGGLE_MENU_IMMEDIATE
        }
    } else {
        if (menuVisible) {
            TapActionResolution.NAVIGATE_IMMEDIATELY
        } else if (isDoubleTap) {
            TapActionResolution.CANCEL_NAV_FOR_ZOOM
        } else if (hasDoubleTapAnimation) {
            TapActionResolution.SCHEDULE_NAV_DELAYED
        } else {
            TapActionResolution.NAVIGATE_IMMEDIATELY
        }
    }
}

internal fun isDoubleTap(
    upTime: Long,
    lastTapTime: Long,
    doubleTapTimeoutMs: Long,
    upPos: Offset,
    lastTapOffset: Offset,
    doubleTapSlopSquared: Float,
    hasDoubleTapAnimation: Boolean,
): Boolean {
    val tapDistanceSquared = (upPos - lastTapOffset).getDistanceSquared()
    return (upTime - lastTapTime < doubleTapTimeoutMs) &&
        (tapDistanceSquared < doubleTapSlopSquared) &&
        hasDoubleTapAnimation
}
