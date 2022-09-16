package org.nekomanga.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun NoRippleText(
    text: String,
    maxLines: Int = Integer.MAX_VALUE,
    color: Color = LocalTextStyle.current.color,
    style: TextStyle = LocalTextStyle.current,
    onClick: (String) -> Unit = {},
    onLongClick: (String) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val noRippleInteraction = remember { MutableInteractionSource() }
    Text(
        text = text,
        modifier = Modifier
            .indication(indication = null, interactionSource = noRippleInteraction)
            .combinedClickable(
                interactionSource = noRippleInteraction,
                indication = null,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(text)
                },
                onClick = { onClick(text) },
            ),
        maxLines = maxLines,
        color = color,
        style = style,
        overflow = TextOverflow.Ellipsis,

    )
}
