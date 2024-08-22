package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackList
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun TrackingListDialog(
    themeColorState: ThemeColorState,
    currentLists: ImmutableList<TrackList>,
    service: TrackServiceItem,
    onDismiss: () -> Unit,
    trackListChange: (List<String>, List<String>) -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleTheme provides themeColorState.rippleTheme,
        LocalTextSelectionColors provides themeColorState.textSelectionColors) {
            val scope = rememberCoroutineScope()

            val saver =
                listSaver<MutableList<Int>, Int>(
                    save = {
                        when (it.isEmpty()) {
                            true -> emptyList()
                            false -> it.toList()
                        }
                    },
                    restore = { it.toMutableStateList() },
                )

            val positionOfItemsToAdd = rememberSaveable(saver = saver) { mutableStateListOf() }
            val positionOfItemsToRemove = rememberSaveable(saver = saver) { mutableStateListOf() }

            AlertDialog(
                title = {
                    Text(
                        text = stringResource(id = R.string.list),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        service.lists?.forEachIndexed { index, list ->
                            var isSelected by remember {
                                mutableStateOf(
                                    currentLists.contains(list) ||
                                        positionOfItemsToAdd.contains(index))
                            }

                            val clicked = { enabled: Boolean ->
                                scope.launch {
                                    isSelected = enabled
                                    when (enabled) {
                                        true -> {
                                            positionOfItemsToRemove.remove(index)
                                            if (currentLists.firstOrNull {
                                                it.id == service.lists[index].id
                                            } == null)
                                                positionOfItemsToAdd.add(index)
                                        }

                                        false -> {
                                            positionOfItemsToAdd.remove(index)
                                            positionOfItemsToRemove.add(index)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .selectable(
                                            selected = isSelected,
                                            onClick = { clicked(!isSelected) },
                                        ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = (isSelected),
                                    onCheckedChange = { enabled -> clicked(enabled) },
                                    colors =
                                        CheckboxDefaults.colors(
                                            checkedColor = themeColorState.buttonColor),
                                )
                                Gap(width = 8.dp)
                                Text(text = list.name, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                },
                onDismissRequest = onDismiss,
                dismissButton = {
                    TextButton(
                        onClick = onDismiss,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = themeColorState.buttonColor)) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (service.lists != null) {
                                trackListChange
                                val itemsToAddUUIDs =
                                    positionOfItemsToAdd.map { service.lists[it].id }
                                val itemsToRemoveUUIDs =
                                    positionOfItemsToRemove.map { service.lists[it].id }
                                trackListChange(itemsToAddUUIDs, itemsToRemoveUUIDs)
                            }
                            onDismiss()
                        },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = themeColorState.buttonColor),
                    ) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
            )
        }
}
