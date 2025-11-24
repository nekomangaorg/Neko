package org.nekomanga.presentation.functions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun getTopAppBarColor(title: String, altAppBarColor: Boolean): Triple<Color, Color, Boolean> {
    return when {
        title.isEmpty() && !altAppBarColor ->
            Triple(
                Color.Transparent,
                Color.Black,
                (MaterialTheme.colorScheme.surface.luminance() > 0.5f),
            )
        title.isNotEmpty() && !altAppBarColor ->
            Triple(
                MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                MaterialTheme.colorScheme.onSurface,
                (MaterialTheme.colorScheme.surface.copy(alpha = .7f).luminance() > 0.5f),
            )
        else ->
            Triple(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .7f),
                MaterialTheme.colorScheme.onSecondaryContainer,
                (MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .7f).luminance() > 0.5f),
            )
    }
}
