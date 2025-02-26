package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.theme.Size

@Composable
fun MultiSelectListPreferenceWidget(
    preference: Preference.PreferenceItem.MultiSelectListPreference,
    values: Set<String>,
    onValuesChange: (Set<String>) -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = preference.title,
        subtitle = preference.subtitleProvider(values, preference.entries),
        icon = preference.icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val selected = remember {
            preference.entries.keys.filter { values.contains(it) }.toMutableStateList()
        }
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
            onDismissRequest = { isDialogShown = false },
            title = { Text(text = preference.title) },
            text = {
                LazyColumn {
                    preference.entries.forEach { current ->
                        item {
                            val isSelected = selected.contains(current.key)
                            CheckboxRow(
                                modifier = Modifier.fillMaxWidth(),
                                rowText = current.value,
                                checkedState = isSelected,
                                checkedChange = {
                                    if (it) {
                                        selected.add(current.key)
                                    } else {
                                        selected.remove(current.key)
                                    }
                                },
                            )
                        }
                    }
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = true),
            confirmButton = {
                TextButton(
                    onClick = {
                        onValuesChange(selected.toMutableSet())
                        isDialogShown = false
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogShown = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
