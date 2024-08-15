package org.nekomanga.presentation.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.ThresholdConfig
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

@Composable
fun NekoSwipeToDismiss(
    state: DismissState,
    modifier: Modifier = Modifier,
    directions: Set<DismissDirection> =
        setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
    dismissThresholds: (DismissDirection) -> ThresholdConfig = { FractionalThreshold(.3f) },
    background: @Composable RowScope.() -> Unit,
    dismissContent: @Composable RowScope.() -> Unit,
) =
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth.toFloat()
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        val anchors = mutableMapOf(0f to DismissValue.Default)
        if (DismissDirection.StartToEnd in directions)
            anchors += width to DismissValue.DismissedToEnd
        if (DismissDirection.EndToStart in directions)
            anchors += -width to DismissValue.DismissedToStart

        val thresholds = { from: DismissValue, to: DismissValue ->
            dismissThresholds(getDismissDirection(from, to)!!)
        }
        val minFactor =
            if (DismissDirection.EndToStart in directions)
                SwipeableDefaults.StandardResistanceFactor
            else SwipeableDefaults.StiffResistanceFactor
        val maxFactor =
            if (DismissDirection.StartToEnd in directions)
                SwipeableDefaults.StandardResistanceFactor
            else SwipeableDefaults.StiffResistanceFactor
        Box(
            Modifier.swipeable(
                state = state,
                anchors = anchors,
                thresholds = thresholds,
                orientation = Orientation.Horizontal,
                enabled = state.currentValue == DismissValue.Default,
                reverseDirection = isRtl,
                resistance =
                    ResistanceConfig(
                        basis = width,
                        factorAtMin = minFactor,
                        factorAtMax = maxFactor,
                    ),
                velocityThreshold = SwipeableDefaults.VelocityThreshold * 100,
            ),
        ) {
            Row(
                content = background,
                modifier = Modifier.matchParentSize(),
            )
            Row(
                content = dismissContent,
                modifier = Modifier.offset { IntOffset(state.offset.value.roundToInt(), 0) },
            )
        }
    }

private fun getDismissDirection(from: DismissValue, to: DismissValue): DismissDirection? {
    return when {
        // settled at the default state
        from == to && from == DismissValue.Default -> null
        // has been dismissed to the end
        from == to && from == DismissValue.DismissedToEnd -> DismissDirection.StartToEnd
        // has been dismissed to the start
        from == to && from == DismissValue.DismissedToStart -> DismissDirection.EndToStart
        // is currently being dismissed to the end
        from == DismissValue.Default && to == DismissValue.DismissedToEnd ->
            DismissDirection.StartToEnd
        // is currently being dismissed to the start
        from == DismissValue.Default && to == DismissValue.DismissedToStart ->
            DismissDirection.EndToStart
        // has been dismissed to the end but is now animated back to default
        from == DismissValue.DismissedToEnd && to == DismissValue.Default ->
            DismissDirection.StartToEnd
        // has been dismissed to the start but is now animated back to default
        from == DismissValue.DismissedToStart && to == DismissValue.Default ->
            DismissDirection.EndToStart
        else -> null
    }
}
