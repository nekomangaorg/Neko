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

object PieChartColors {
    val ongoing = Color(0xFF3BAEEA)
    val completed = Color(0x777BD555)
    val licensed = Color(0x77F79A63)
    val publicationComplete = Color(0x77d29d2f)
    val cancelled = Color(0x77E85D75)
    val hiatus = Color(0x77F17575)
    val unknown = Color(0x775b5b5b)
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


