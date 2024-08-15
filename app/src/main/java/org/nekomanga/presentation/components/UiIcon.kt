package org.nekomanga.presentation.components

import androidx.compose.ui.graphics.vector.ImageVector

sealed class UiIcon {
    data class Icon(val icon: ImageVector) : UiIcon()

    data class IIcon(val icon: com.mikepenz.iconics.typeface.IIcon) : UiIcon()
}
