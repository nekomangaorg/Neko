package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

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
        modifier = modifier.clickable {
            toggleStateIfAble(disabled, state, toggleState)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = state,
            onClick = { toggleStateIfAble(disabled, state, toggleState) },
            enabled = !disabled,
            colors = CheckboxDefaults.colors(
                checkedColor = themeColorState.buttonColor,
                checkmarkColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Gap(4.dp)
        Text(text = rowText, color = if (!disabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast), style = rowTextStyle)
    }
}

private fun toggleStateIfAble(disabled: Boolean, state: ToggleableState, toggleState: (ToggleableState) -> Unit) {
    if (!disabled) {
        val newState = when (state) {
            ToggleableState.On -> ToggleableState.Indeterminate
            ToggleableState.Indeterminate -> ToggleableState.Off
            ToggleableState.Off -> ToggleableState.On
        }
        toggleState(newState)
    }
}
