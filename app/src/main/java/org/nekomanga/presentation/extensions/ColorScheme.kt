package org.nekomanga.presentation.extensions

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import kotlin.math.ln
import org.nekomanga.presentation.theme.Size

fun ColorScheme.surfaceColorAtElevationCustomColor(
    color: Color,
    elevation: Dp,
): Color {
    if (elevation == Size.none) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return color.copy(alpha = alpha).compositeOver(surface)
}
