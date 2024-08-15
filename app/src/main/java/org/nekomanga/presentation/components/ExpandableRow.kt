package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.theme.Size

@Composable
fun ExpandableRow(
    rowText: String,
    isExpanded: Boolean,
    disabled: Boolean,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable {
                    if (!disabled) {
                        focusManager.clearFocus()
                        onClick()
                    }
                }
                .padding(Size.small, 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = rowText,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (!disabled) textColor
                else MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast),
        )
        val icon =
            when (isExpanded) {
                true -> Icons.Default.ExpandLess
                false -> Icons.Default.ExpandMore
            }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
                if (!disabled) textColor
                else MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast),
        )
    }
}
