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
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

@Composable
fun SortRow(
    sortState: MangaConstants.SortState,
    sortChanged: (MangaConstants.SortState) -> Unit,
    rowText: String,
    modifier: Modifier = Modifier,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    Row(
        modifier = modifier
            .clickable {
                changeSortState(sortState, sortChanged)
            }
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (sortState) {
            MangaConstants.SortState.Ascending -> {
                Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = null, tint = themeColorState.buttonColor, modifier = Modifier.size(24.dp))
            }
            MangaConstants.SortState.Descending -> {
                Icon(imageVector = Icons.Filled.ArrowDownward, contentDescription = null, tint = themeColorState.buttonColor, modifier = Modifier.size(24.dp))
            }
            MangaConstants.SortState.None -> {
                Gap(24.dp)
            }
        }
        Gap(16.dp)
        Text(text = rowText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun changeSortState(sortState: MangaConstants.SortState, sortChanged: (MangaConstants.SortState) -> Unit) {
    val newState = when (sortState) {
        MangaConstants.SortState.Ascending -> MangaConstants.SortState.Descending
        MangaConstants.SortState.Descending -> MangaConstants.SortState.Ascending
        MangaConstants.SortState.None -> MangaConstants.SortState.Descending
    }
    sortChanged(newState)
}
