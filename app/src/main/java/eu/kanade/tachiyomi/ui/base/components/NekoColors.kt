package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class NekoColors() {
    companion object {
        val outline = Color(0xFF79747E)
    }
}

object CoverRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color = MaterialTheme.colors.primary

    @Composable
    override fun rippleAlpha() = RippleAlpha(
        draggedAlpha = 0.9f,
        focusedAlpha = 0.9f,
        hoveredAlpha = 0.9f,
        pressedAlpha = 0.9f,
    )
}