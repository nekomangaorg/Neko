package org.nekomanga.presentation.components

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class NekoColors {
    companion object {
        val outline = Color(0xFF79747E)
        const val highAlphaHighContrast = 1f
        const val highAlphaLowContrast = .87f
        const val mediumAlphaHighContrast = .74f
        const val mediumAlphaLowContrast = .6f
        const val disabledAlphaHighContrast = .38f
        const val disabledAlphaLowContrast = .38f
        const val veryLowContrast = .1f
    }
}

object ChartColors {
    val one = Color(0xFF45818e)
    val two = Color(0xFF3BAEEA)
    val three = Color(0xFF7BD555)
    val four = Color(0xFFd29d2f)
    val five = Color(0xFFF17575)
    val six = Color(0xFFe65da1)
    val seven = Color(0xFF55e2cf)
    val eight = Color(0xFF8a56e2)
    val nine = Color(0xFFb699b9)
    val ten = Color(0xFF30a58e)
    val eleven = Color(0xFFff00ff)
    val twelve = Color(0xFFde1f62)
}

object PrimaryColorRippleTheme : RippleTheme {

    @Composable
    override fun defaultColor(): Color = MaterialTheme.colorScheme.primary

    @Composable
    override fun rippleAlpha() = RippleAlpha(
        draggedAlpha = 0.9f,
        focusedAlpha = 0.9f,
        hoveredAlpha = 0.9f,
        pressedAlpha = 0.9f,
    )
}

class DynamicRippleTheme(val color: Color) : RippleTheme {
    @Composable
    override fun defaultColor(): Color = color

    @Composable
    override fun rippleAlpha() = RippleAlpha(
        draggedAlpha = 0.9f,
        focusedAlpha = 0.9f,
        hoveredAlpha = 0.9f,
        pressedAlpha = 0.9f,
    )
}

fun dynamicTextSelectionColor(color: Color) = TextSelectionColors(
    handleColor = color,
    backgroundColor = color.copy(alpha = NekoColors.disabledAlphaHighContrast),
)


