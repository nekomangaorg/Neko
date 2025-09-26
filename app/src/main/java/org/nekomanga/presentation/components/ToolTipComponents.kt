package org.nekomanga.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.nekomanga.presentation.theme.Size

/**
 * This is a Tooltip Icon button, a wrapper around a CombinedClickableIcon Button, in which the long
 * click of the button with show the tooltip
 */
@Composable
fun ToolTipButton(
    toolTipLabel: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
    isEnabled: Boolean = true,
    enabledTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
) {
    require(icon != null || painter != null)

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val textFieldTooltipState = rememberTooltipState()

    val longClick: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch { textFieldTooltipState.show() }
    }

    val clickableModifier =
        if (isEnabled) {
            Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                onClickLabel = toolTipLabel,
                role = Role.Button,
                onClick = onClick,
                onLongClick = longClick,
            )
        } else {
            Modifier
        }

    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below, Size.tiny),
        state = textFieldTooltipState,
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(
                    modifier = Modifier.padding(Size.tiny),
                    style = MaterialTheme.typography.bodyLarge,
                    text = toolTipLabel,
                )
            }
        },
    ) {
        Box(
            modifier = modifier.size(40.dp).then(clickableModifier),
            contentAlignment = Alignment.Center,
        ) {
            val contentColor =
                if (isEnabled) {
                    enabledTint
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = NekoColors.disabledAlphaLowContrast
                    )
                }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                when {
                    icon != null -> {
                        Icon(
                            imageVector = icon,
                            modifier = iconModifier,
                            contentDescription = toolTipLabel,
                        )
                    }
                    painter != null -> {
                        Icon(
                            painter = painter,
                            modifier = iconModifier,
                            contentDescription = toolTipLabel,
                        )
                    }
                }
            }
        }
    }
}
