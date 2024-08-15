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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun CheckboxRow(
    checkedState: Boolean,
    checkedChange: (Boolean) -> Unit,
    rowText: String,
    modifier: Modifier = Modifier,
    rowTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    disabled: Boolean = false,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    Row(
        modifier =
            modifier.clickable {
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
            colors =
                CheckboxDefaults.colors(
                    checkedColor = themeColorState.buttonColor,
                    checkmarkColor = MaterialTheme.colorScheme.surface,
                ),
        )
        Gap(Size.tiny)
        Text(
            text = rowText,
            style = rowTextStyle,
            color =
                if (!disabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast),
        )
    }
}

@Composable
fun FilterChipWrapper(
    selected: Boolean,
    onClick: () -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    hideIcons: Boolean = false,
    labelStyle: TextStyle =
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
) {
    TriStateFilterChip(
        modifier = modifier,
        state = ToggleableState(selected),
        hideIcons = hideIcons,
        toggleState = { _ -> onClick() },
        name = name,
        labelTextStyle = labelStyle,
    )
}
