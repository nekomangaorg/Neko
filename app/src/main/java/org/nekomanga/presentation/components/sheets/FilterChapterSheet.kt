package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortState.Ascending
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortState.Descending
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortState.None
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.ChapterNumber
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.SourceOrder
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.UploadDate
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun FilterChapterSheet(themeColorState: ThemeColorState, sortFilter: MangaConstants.SortFilter, changeSort: (SortOption) -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {

        BaseSheet(themeColor = themeColorState) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Sort(themeColorState = themeColorState, sortFilter, changeSort)
                }
                item {
                    Filter(themeColorState = themeColorState)
                }

            }
        }
    }
}

@Composable
private fun Sort(themeColorState: ThemeColorState, sortFilter: MangaConstants.SortFilter, changeSort: (SortOption) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Text(text = stringResource(id = R.string.sort), style = MaterialTheme.typography.labelMedium)
        Gap(8.dp)
        SortLine(themeColorState, SortOption(sortFilter.sourceOrderSort, SourceOrder), stringResource(id = R.string.by_source_order), changeSort)
        SortLine(themeColorState, SortOption(sortFilter.chapterNumberSort, ChapterNumber), stringResource(id = R.string.by_chapter_number), changeSort)
        SortLine(themeColorState, SortOption(sortFilter.uploadDateSort, UploadDate), stringResource(id = R.string.by_update_date), changeSort)

    }
}

@Composable
private fun SortLine(themeColorState: ThemeColorState, state: SortOption, text: String, changeSort: (SortOption) -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val newState = when (state.sortState) {
                    Ascending -> Descending
                    Descending -> Ascending
                    None -> Descending
                }
                val sortOption = state.copy(sortState = newState)
                changeSort(sortOption)

            }
            .padding(horizontal = 0.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state.sortState) {
            Ascending -> {
                Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = null, tint = themeColorState.buttonColor, modifier = Modifier.size(24.dp))
            }
            Descending -> {
                Icon(imageVector = Icons.Filled.ArrowDownward, contentDescription = null, tint = themeColorState.buttonColor, modifier = Modifier.size(24.dp))
            }
            None -> {
                Gap(24.dp)
            }
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge)

    }
}

@Composable
private fun Filter(themeColorState: ThemeColorState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Text(text = stringResource(id = R.string.filter), style = MaterialTheme.typography.labelMedium)
        Gap(8.dp)
        FilterLine(themeColorState, ToggleableState.On, stringResource(id = R.string.show_all))
        FilterLine(themeColorState, ToggleableState.Off, stringResource(id = R.string.show_unread_chapters))
        FilterLine(themeColorState, ToggleableState.Indeterminate, stringResource(id = R.string.show_downloaded_chapters))
        FilterLine(themeColorState, ToggleableState.Indeterminate, stringResource(id = R.string.show_bookmarked_chapters))

    }
}

@Composable
private fun FilterLine(themeColorState: ThemeColorState, state: ToggleableState, text: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = state,
            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.surface, uncheckedColor = themeColorState.buttonColor, checkedColor = themeColorState.buttonColor),
            onClick = { /*TODO*/ },
        )
        Gap(width = 8.dp)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)

    }
}
