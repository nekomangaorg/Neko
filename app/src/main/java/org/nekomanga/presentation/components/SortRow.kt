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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.ui.theme.ThemeConfig
import org.nekomanga.ui.theme.ThemeConfigProvider
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
            false -> themeColorState.primaryColor to MaterialTheme.colorScheme.onSurface
        }

    Row(
        modifier =
            modifier
                .clickable {
                    if (!disabled) {
                        changeSortState(sortState, sortChanged)
                    }
                }
                .padding(horizontal = Size.smedium, vertical = Size.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (sortState) {
            MangaConstants.SortState.Ascending -> {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(id = org.nekomanga.R.string.ascending),
                    tint = tintColor,
                    modifier = Modifier.size(Size.large),
                )
            }
            MangaConstants.SortState.Descending -> {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(id = org.nekomanga.R.string.descending),
                    tint = tintColor,
                    modifier = Modifier.size(Size.large),
                )
            }
            MangaConstants.SortState.None -> {
                Gap(Size.large)
            }
        }
        Gap(Size.medium)
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

@Preview
@Composable
private fun SortRowPreview(@PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig) {
    ThemedPreviews(themeConfig) { theme ->
        SortRow(
            sortState = MangaConstants.SortState.Ascending,
            sortChanged = {},
            rowText = theme.name,
        )
    }
}
