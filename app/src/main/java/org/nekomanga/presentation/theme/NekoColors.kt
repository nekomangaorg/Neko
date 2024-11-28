package org.nekomanga.presentation.components

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class NekoColors {
    companion object {
        const val highAlphaHighContrast = 1f
        const val highAlphaLowContrast = .87f
        const val mediumAlphaHighContrast = .74f
        const val mediumAlphaLowContrast = .6f
        const val halfAlpha = .5f
        const val disabledAlphaHighContrast = .38f
        const val disabledAlphaLowContrast = .38f
        const val veryLowContrast = .1f
    }
}

object IconColors {
    val icon = Color(0xFF2A2121)
    val outline = Color(0xFFff124a)
    val background = Color(0xFFffffff)
}

object Outline {
    val color = Color(0XFF9D9D9D)
    val thickness = .75.dp
}

object ChartColors {
    val one = Color(0xFFa368f7)
    val two = Color(0xFF3BAEEA)
    val three = Color(0xFF7BD555)
    val four = Color(0xFFd29d2f)
    val five = Color(0xFFF17575)
    val six = Color(0xFFe65da1)
    val seven = Color(0xFF55e2cf)
    val eight = Color(0xFF45818e)
    val nine = Color(0xFFb699b9)
    val ten = Color(0xFF30a58e)
    val eleven = Color(0xFFff00ff)
    val twelve = Color(0xFFde1f62)
}

fun nekoRippleConfiguration(color: Color) = RippleConfiguration(color, nekoRippleAlpha)

@Composable
@ReadOnlyComposable
fun defaultRippleConfiguration(): RippleConfiguration {
    return nekoRippleConfiguration(MaterialTheme.colorScheme.primary)
}

private val nekoRippleAlpha =
    RippleAlpha(
        draggedAlpha = NekoColors.mediumAlphaLowContrast,
        focusedAlpha = NekoColors.mediumAlphaLowContrast,
        hoveredAlpha = NekoColors.mediumAlphaLowContrast,
        pressedAlpha = NekoColors.mediumAlphaLowContrast,
    )

fun dynamicTextSelectionColor(color: Color) =
    TextSelectionColors(
        handleColor = color,
        backgroundColor = color.copy(alpha = NekoColors.disabledAlphaHighContrast),
    )
