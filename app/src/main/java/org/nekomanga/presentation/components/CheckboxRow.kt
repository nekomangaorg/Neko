package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

@Composable
fun CheckboxRow(
    checkedState: Boolean,
    checkedChange: (Boolean) -> Unit,
    rowText: String,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    Row(
        modifier = modifier.clickable {
            if (!disabled) {
                checkedChange(!checkedState)
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checkedState,
            onCheckedChange = checkedChange,
            enabled = !disabled,
            colors = CheckboxDefaults.colors(
                checkedColor = themeColorState.buttonColor,
                checkmarkColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Gap(4.dp)
        Text(
            text = rowText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (!disabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast),
        )
    }
}
