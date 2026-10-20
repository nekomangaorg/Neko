package org.nekomanga.presentation.screens.reader

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import org.nekomanga.presentation.components.UiText

@Immutable
data class NavigationRegionUiModel(
    val id: String,
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float,
    val label: UiText,
    val color: Color,
)
