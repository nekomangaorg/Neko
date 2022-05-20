@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumTouchTargetSize
import androidx.compose.material3.toColor
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * This is a box around a CombinedClickableIcon Button, in which the long click of the button with show the tooltip
 */
@Composable
fun TooltipBox(
    toolTipLabel: String,
    icon: ImageVector,
    isEnabled: Boolean = true,
    buttonClicked: () -> Unit,
) {
    val showTooltip = remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    CombinedClickableIconButton(
        enabled = isEnabled,
        modifier = Modifier.iconButtonCombinedClickable(
            toolTipLabel = toolTipLabel,
            onClick = buttonClicked,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showTooltip.value = true
            },
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = toolTipLabel,
        )
    }

    Tooltip(
        showTooltip,
        modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = .75f)),
    ) {
        Text(
            text = toolTipLabel,
            color = MaterialTheme.colorScheme.surface,
        )
    }
}

/**
 * This button doesnt override clickable, and allows you to pass a combined clickable to do long, double, and normal click
 */
@Composable
fun CombinedClickableIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
        modifier
            .minimumTouchTargetSize()
            .size(IconButtonTokens.StateLayerSize),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor =
            if (enabled) {
                IconButtonTokens.UnselectedIconColor.toColor()
            } else {
                IconButtonTokens.DisabledIconColor.toColor()
                    .copy(alpha = IconButtonTokens.DisabledIconOpacity)
            }
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * This button wraps combinedClickable with the remember ripple from a normal IconButton, and is to be used with the
 * CombinedClickableIconButton
 */
fun Modifier.iconButtonCombinedClickable(
    toolTipLabel: String,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) = composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(
            bounded = false,
            radius = IconButtonTokens.StateLayerSize / 2,
        ),
        onClickLabel = toolTipLabel,
        role = Role.Button,
        onClick = onClick,
        onLongClick = onLongClick,
        onDoubleClick = onDoubleClick,
    )
}
