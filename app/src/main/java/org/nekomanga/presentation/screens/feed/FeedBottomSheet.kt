package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.recents.FeedHistoryGroup
import eu.kanade.tachiyomi.ui.recents.FeedScreenState
import eu.kanade.tachiyomi.ui.recents.FeedSettingActions
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedBottomSheet(
    feedScreenState: State<FeedScreenState>,
    contentPadding: PaddingValues,
    feedActions: FeedSettingActions,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    clearHistoryClick: () -> Unit,
    closeSheet: () -> Unit,
) {

    BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .6f, bottomPaddingAroundContent = contentPadding.calculateBottomPadding()) {

        Gap(16.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Size.medium),
            verticalArrangement = Arrangement.spacedBy(Size.small),
        ) {

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(id = R.string.group_chapters_together), style = MaterialTheme.typography.bodyMedium)
                    var expanded by remember { mutableStateOf(false) }
                    var selectedText = when (feedScreenState.value.historyGrouping) {
                        FeedHistoryGroup.Never -> R.string.group_never
                        FeedHistoryGroup.Series -> R.string.group_by_series
                        FeedHistoryGroup.Day -> R.string.group_by_day
                        FeedHistoryGroup.Week -> R.string.group_by_week
                    }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        modifier = Modifier.padding(start = Size.small, top = Size.small, bottom = Size.small, end = Size.none),
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        TextField(
                            value = stringResource(id = selectedText),
                            onValueChange = {},
                            textStyle = MaterialTheme.typography.bodyMedium,
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            FeedHistoryGroup.values().forEach { entry ->
                                val textRes = when (entry) {
                                    FeedHistoryGroup.Never -> R.string.group_never
                                    FeedHistoryGroup.Series -> R.string.group_by_series
                                    FeedHistoryGroup.Day -> R.string.group_by_day
                                    FeedHistoryGroup.Week -> R.string.group_by_week
                                }
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = textRes), style = MaterialTheme.typography.bodyMedium) },

                                    onClick = {
                                        selectedText = textRes
                                        expanded = false
                                        feedActions.groupHistoryClick(entry)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(
                        onClick = {
                            closeSheet()
                            clearHistoryClick()
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null)
                            Text(text = stringResource(id = R.string.clear_history))
                        }
                    }
                }
            }
        }

    }
}
