package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.surfaceColorAtElevation
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
fun TriStateCheckboxRow(
    state: ToggleableState,
    toggleState: (ToggleableState) -> Unit,
    rowText: String,
    modifier: Modifier = Modifier,
    rowTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    disabled: Boolean = false,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    Row(
        modifier = modifier.clickable { toggleStateIfAble(disabled, state, toggleState) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = state,
            onClick = { toggleStateIfAble(disabled, state, toggleState) },
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
            color =
                if (!disabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast),
            style = rowTextStyle,
        )
    }
}

@Composable
fun TriStateFilterChip(
    state: ToggleableState,
    toggleState: (ToggleableState) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    hideIcons: Boolean = false,
    labelTextStyle: TextStyle =
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
) {
    FilterChip(
        modifier = modifier,
        selected = state == ToggleableState.On || state == ToggleableState.Indeterminate,
        onClick = { toggleStateIfAble(false, state, toggleState) },
        leadingIcon = {
            if (!hideIcons) {
                if (state == ToggleableState.On) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                } else if (state == ToggleableState.Indeterminate) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                }
            }
        },
        shape = RoundedCornerShape(100),
        label = { Text(text = name, style = labelTextStyle) },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                selectedContainerColor =
                    MaterialTheme.colorScheme.surfaceColorAtElevation(Size.small),
                selectedLabelColor = MaterialTheme.colorScheme.primary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.onSurface.copy(NekoColors.veryLowContrast),
                selectedBorderColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.small),
            ),
    )
}

private fun toggleStateIfAble(
    disabled: Boolean,
    state: ToggleableState,
    toggleState: (ToggleableState) -> Unit,
) {
    if (!disabled) {
        val newState =
            when (state) {
                ToggleableState.On -> ToggleableState.Indeterminate
                ToggleableState.Indeterminate -> ToggleableState.Off
                ToggleableState.Off -> ToggleableState.On
            }
        toggleState(newState)
    }
}
