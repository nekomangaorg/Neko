package org.nekomanga.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.Size.large as horizontalPadding
import org.nekomanga.presentation.theme.Size.medium as verticalPadding
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.toggle

@Composable
fun HeadingItem(text: UiText) {
    Text(
        text = text.asString(),
        style = MaterialTheme.typography.headlineLarge,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    )
}

@Composable
fun IconItem(labelText: UiText, icon: ImageVector, onClick: () -> Unit) {
    BaseSettingsItem(
        labelText = labelText,
        widget = {
            Icon(
                modifier = Modifier.size(Size.large),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun SortItem(labelText: UiText, sortDescending: Boolean?, onClick: () -> Unit) {
    val arrowIcon =
        when (sortDescending) {
            true -> Icons.Default.ArrowDownward
            false -> Icons.Default.ArrowUpward
            null -> null
        }

    BaseSortItem(labelText = labelText, icon = arrowIcon, onClick = onClick)
}

@Composable
fun BaseSortItem(labelText: UiText, icon: ImageVector?, onClick: () -> Unit) {
    BaseSettingsItem(
        labelText = labelText,
        widget = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Gap(Size.large)
            }
        },
        onClick = onClick,
    )
}

@Composable
fun CheckboxItem(labelText: UiText, preference: Preference<Boolean>) {
    val checked by preference.collectAsState<Boolean>()
    CheckboxItem(labelText = labelText, checked = checked, onClick = { preference.toggle() })
}

@Composable
fun CheckboxItem(labelText: UiText, checked: Boolean, onClick: () -> Unit) {
    BaseSettingsItem(
        labelText = labelText,
        widget = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = onClick,
    )
}

@Composable
fun RadioItem(labelText: UiText, selected: Boolean, onClick: () -> Unit) {
    BaseSettingsItem(
        labelText = labelText,
        widget = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
    )
}

@Composable
fun SelectItem(
    labelText: UiText,
    options: Array<out Any?>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            modifier =
                Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            label = { Text(text = labelText.asString()) },
            value = options[selectedIndex].toString(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text.toString()) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun TriStateItem(
    labelText: UiText,
    state: ToggleableState,
    enabled: Boolean = true,
    onClick: ((ToggleableState) -> Unit)?,
) {
    Row(
        modifier =
            Modifier.clickable(
                    enabled = enabled && onClick != null,
                    onClick = {
                        when (state) {
                            ToggleableState.Off -> onClick?.invoke(ToggleableState.On)
                            ToggleableState.On -> onClick?.invoke(ToggleableState.Indeterminate)
                            ToggleableState.Indeterminate -> onClick?.invoke(ToggleableState.Off)
                        }
                    },
                )
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Size.large),
    ) {
        val stateAlpha = if (enabled && onClick != null) 1f else NekoColors.disabledAlphaLowContrast

        Icon(
            imageVector =
                when (state) {
                    ToggleableState.Off -> Icons.Rounded.CheckBoxOutlineBlank
                    ToggleableState.On -> Icons.Rounded.CheckBox
                    ToggleableState.Indeterminate -> Icons.Rounded.DisabledByDefault
                },
            contentDescription = null,
            tint =
                if (!enabled || state == ToggleableState.Indeterminate) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = stateAlpha)
                } else {
                    when (onClick) {
                        null ->
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = NekoColors.disabledAlphaLowContrast
                            )
                        else -> MaterialTheme.colorScheme.primary
                    }
                },
        )
        Text(
            text = labelText.asString(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = stateAlpha),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun TextItem(labelText: UiText, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = Size.tiny),
        label = { Text(text = labelText.asString()) },
        value = value,
        onValueChange = onChange,
        singleLine = true,
    )
}

@Composable
fun SettingsChipRow(labelText: UiText, content: @Composable FlowRowScope.() -> Unit) {
    Column {
        HeadingItem(labelText)
        FlowRow(
            modifier =
                Modifier.padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = verticalPadding,
                    top = 0.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(Size.small),
            content = content,
        )
    }
}

@Composable
fun SettingsIconGrid(labelText: UiText, content: LazyGridScope.() -> Unit) {
    Column {
        HeadingItem(labelText)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            modifier =
                Modifier.padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = verticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(Size.tiny),
            horizontalArrangement = Arrangement.spacedBy(Size.small),
            content = content,
        )
    }
}

@Composable
private fun BaseSettingsItem(
    labelText: UiText,
    widget: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.clickable(onClick = onClick)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Size.large),
    ) {
        widget(this)
        Text(text = labelText.asString(), style = MaterialTheme.typography.bodyLarge)
    }
}
