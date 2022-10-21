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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.toColor
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.components.NekoColors

/**
 * This is a Tooltip Icon button, a wrapper around a CombinedClickableIcon Button, in which the long click of the button with show the tooltip
 */
@Composable
fun ToolTipIconButton(
    toolTipLabel: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
    isEnabled: Boolean = true,
    enabledTint: Color = MaterialTheme.colorScheme.onSurface,
    buttonClicked: () -> Unit = {},
) {
    require(icon != null || painter != null)

    val showTooltip = remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    CombinedClickableIconButton(
        enabled = isEnabled,
        enabledTint = enabledTint,
        modifier = modifier.iconButtonCombinedClickable(
            toolTipLabel = toolTipLabel,
            onClick = buttonClicked,
            isEnabled = isEnabled,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showTooltip.value = true
            },
        ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                modifier = iconModifier,
                contentDescription = toolTipLabel,
            )
        } else {
            Icon(
                painter = painter!!,
                modifier = iconModifier,
                contentDescription = toolTipLabel,
            )
        }
    }

    Tooltip(
        showTooltip,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
    ) {
        Text(
            text = toolTipLabel,
            color = MaterialTheme.colorScheme.onSurface,
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
    enabledTint: Color,
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
                enabledTint
            } else {
                IconButtonTokens.DisabledIconColor.toColor()
                    .copy(alpha = NekoColors.disabledAlphaLowContrast)
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
    isEnabled: Boolean,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) = composed {
    if (isEnabled) {
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
    } else {
        this
    }
}
