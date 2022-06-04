package org.nekomanga.presentation.components

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
    }
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


