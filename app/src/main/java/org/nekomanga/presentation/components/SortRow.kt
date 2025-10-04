package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.ui.theme.ThemePreviews
import org.nekomanga.ui.theme.ThemedPreviews

@Composable
fun SortRow(
    sortState: MangaConstants.SortState,
    sortChanged: (MangaConstants.SortState) -> Unit,
    rowText: String,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    rowTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    val (tintColor, textColor) =
        when (disabled) {
            true ->
                MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast) to
                    MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast)
            false -> themeColorState.buttonColor to MaterialTheme.colorScheme.onSurface
        }

    Row(
        modifier =
            modifier
                .clickable {
                    if (!disabled) {
                        changeSortState(sortState, sortChanged)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (sortState) {
            MangaConstants.SortState.Ascending -> {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            MangaConstants.SortState.Descending -> {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            MangaConstants.SortState.None -> {
                Gap(24.dp)
            }
        }
        Gap(16.dp)
        Text(text = rowText, style = rowTextStyle, color = textColor)
    }
}

private fun changeSortState(
    sortState: MangaConstants.SortState,
    sortChanged: (MangaConstants.SortState) -> Unit,
) {
    val newState =
        when (sortState) {
            MangaConstants.SortState.Ascending -> MangaConstants.SortState.Descending
            MangaConstants.SortState.Descending -> MangaConstants.SortState.Ascending
            MangaConstants.SortState.None -> MangaConstants.SortState.Descending
        }
    sortChanged(newState)
}

@ThemePreviews
@Composable
private fun SortRowPreview() {
    ThemedPreviews { theme ->
        SortRow(
            sortState = MangaConstants.SortState.Ascending,
            sortChanged = {},
            rowText = theme.name,
        )
    }
}