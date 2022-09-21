package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortState.Ascending
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortState.Descending
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortState.None
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.ChapterNumber
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.SourceOrder
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.UploadDate
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun FilterChapterSheet(
    themeColorState: ThemeColorState,
    sortFilter: MangaConstants.SortFilter,
    filter: MangaConstants.Filter,
    scanlatorFilter: MangaConstants.ScanlatorFilter,
    languageFilter: MangaConstants.LanguageFilter,
    hideTitlesFilter: Boolean,
    changeSort: (SortOption?) -> Unit,
    changeFilter: (MangaConstants.FilterOption?) -> Unit,
    changeScanlatorFilter: (MangaConstants.ScanlatorOption?) -> Unit,
    changeLanguageFilter: (MangaConstants.LanguageOption?) -> Unit,
    changeHideTitles: (Boolean) -> Unit,
    setAsGlobal: (MangaConstants.SetGlobal) -> Unit,
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .8

        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {
                item {
                    Sort(themeColorState = themeColorState, sortFilter, changeSort) { setAsGlobal(MangaConstants.SetGlobal.Sort) }
                }
                item {
                    Filter(themeColorState = themeColorState, filter, hideTitlesFilter, changeFilter, changeHideTitles) { setAsGlobal(MangaConstants.SetGlobal.Filter) }
                }

                item {
                    Scanlator(themeColorState = themeColorState, scanlatorFilter, changeScanlatorFilter)
                }
                item {
                    Language(themeColorState = themeColorState, languageFilter, changeLanguageFilter)
                }
            }
        }
    }
}

@Composable
private fun Sort(themeColorState: ThemeColorState, sortFilter: MangaConstants.SortFilter, changeSort: (SortOption?) -> Unit, setGlobal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(modifier = Modifier.padding(vertical = 16.dp), text = stringResource(id = R.string.sort), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            if (!sortFilter.matchesGlobalDefaults) {
                TextButton(onClick = setGlobal) {
                    Text(text = stringResource(id = R.string.set_as_default), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
                }
                TextButton(onClick = { changeSort(null) }) {
                    Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
                }
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Filter(
    themeColorState: ThemeColorState,
    filter: MangaConstants.Filter,
    hideTitlesFilter: Boolean,
    changeFilter: (MangaConstants.FilterOption?) -> Unit,
    changeHideTitles: (Boolean) -> Unit,
    setGlobal: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.filter_and_display),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!filter.matchesGlobalDefaults) {
                TextButton(onClick = setGlobal) {
                    Text(text = stringResource(id = R.string.set_as_default), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
                }
                TextButton(onClick = { changeFilter(null) }) {
                    Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
                }
            }
        }
        CheckboxLine(
            themeColorState = themeColorState,
            checked = filter.showAll,
            disabledOnChecked = true,
            text = stringResource(id = R.string.show_all),
            onChecked = { changeFilter(MangaConstants.FilterOption(filterType = MangaConstants.FilterType.All, filterState = ToggleableState(!filter.showAll))) },
        )
        FilterLine(
            themeColorState = themeColorState,
            state = MangaConstants.FilterOption(filterType = MangaConstants.FilterType.Unread, filterState = filter.unread),
            text = stringResource(id = R.string.show_unread_chapters),
            changeFilter = changeFilter,
        )
        FilterLine(
            themeColorState = themeColorState,
            state = MangaConstants.FilterOption(filterType = MangaConstants.FilterType.Downloaded, filterState = filter.downloaded),
            text = stringResource(id = R.string.show_downloaded_chapters),
            changeFilter = changeFilter,
        )
        FilterLine(
            themeColorState = themeColorState,
            state = MangaConstants.FilterOption(filterType = MangaConstants.FilterType.Bookmarked, filterState = filter.bookmarked),
            text = stringResource(id = R.string.show_bookmarked_chapters),
            changeFilter = changeFilter,
        )

        CheckboxLine(themeColorState = themeColorState, checked = hideTitlesFilter, text = stringResource(id = R.string.hide_chapter_titles)) {
            changeHideTitles(!hideTitlesFilter)
        }
    }
}

@Composable
private fun FilterLine(themeColorState: ThemeColorState, state: MangaConstants.FilterOption, text: String, changeFilter: (MangaConstants.FilterOption) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                changeFilter(
                    state.copy(
                        filterState = when (state.filterState) {
                            ToggleableState.On -> ToggleableState.Indeterminate
                            ToggleableState.Indeterminate -> ToggleableState.Off
                            ToggleableState.Off -> ToggleableState.On
                        },
                    ),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = state.filterState,
            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.surface, uncheckedColor = themeColorState.buttonColor, checkedColor = themeColorState.buttonColor),
            onClick = {
                changeFilter(
                    state.copy(
                        filterState = when (state.filterState) {
                            ToggleableState.On -> ToggleableState.Indeterminate
                            ToggleableState.Indeterminate -> ToggleableState.Off
                            ToggleableState.Off -> ToggleableState.On
                        },
                    ),
                )
            },
        )
        Gap(width = 8.dp)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CheckboxLine(themeColorState: ThemeColorState, checked: Boolean, disabledOnChecked: Boolean = false, text: String, onChecked: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!(disabledOnChecked && checked)) {
                    onChecked()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.surface, uncheckedColor = themeColorState.buttonColor, checkedColor = themeColorState.buttonColor),
            onCheckedChange = { onChecked() },
            enabled = if (disabledOnChecked) !checked else true,
        )
        Gap(width = 8.dp)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Scanlator(themeColorState: ThemeColorState, scanlatorFilter: MangaConstants.ScanlatorFilter, changeScanlatorFilter: (MangaConstants.ScanlatorOption?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.filter_groups),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (scanlatorFilter.scanlators.any { it.disabled }) {
                TextButton(onClick = { changeScanlatorFilter(null) }) {
                    Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
                }
            }
        }

        val enabled = scanlatorFilter.scanlators.size > 1

        scanlatorFilter.scanlators.forEach { scanlatorOption ->
            ScanlatorLine(
                themeColorState = themeColorState,
                enabledButton = enabled,
                scanlatorOption = scanlatorOption,
            ) { changeScanlatorFilter(scanlatorOption.copy(disabled = !scanlatorOption.disabled)) }
        }
    }
}

@Composable
private fun ScanlatorLine(themeColorState: ThemeColorState, scanlatorOption: MangaConstants.ScanlatorOption, enabledButton: Boolean, changeScanlatorFilter: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .conditional(enabledButton) {
                this.clickable {
                    changeScanlatorFilter()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = if (scanlatorOption.disabled) ToggleableState.Indeterminate else ToggleableState.Off,
            enabled = enabledButton,
            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.surface, uncheckedColor = themeColorState.buttonColor, checkedColor = themeColorState.buttonColor),
            onClick = changeScanlatorFilter,
        )
        Gap(width = 8.dp)
        Text(text = scanlatorOption.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Language(themeColorState: ThemeColorState, languageFilter: MangaConstants.LanguageFilter, changeLanguageFilter: (MangaConstants.LanguageOption?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.filter_languages),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (languageFilter.languages.any { it.disabled }) {
                TextButton(onClick = { changeLanguageFilter(null) }) {
                    Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
                }
            }
        }

        val enabled = languageFilter.languages.size > 1

        languageFilter.languages.forEach { languageOption ->
            LanguageLine(
                themeColorState = themeColorState,
                enabledButton = enabled,
                languageOption = languageOption,
            ) { changeLanguageFilter(languageOption.copy(disabled = !languageOption.disabled)) }
        }
    }
}

@Composable
private fun LanguageLine(themeColorState: ThemeColorState, languageOption: MangaConstants.LanguageOption, enabledButton: Boolean, changeLanguageFilter: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .conditional(enabledButton) {
                this.clickable {
                    changeLanguageFilter()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = if (languageOption.disabled) ToggleableState.Indeterminate else ToggleableState.Off,
            enabled = enabledButton,
            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.surface, uncheckedColor = themeColorState.buttonColor, checkedColor = themeColorState.buttonColor),
            onClick = changeLanguageFilter,
        )
        Gap(width = 8.dp)
        Text(text = MdLang.fromIsoCode(languageOption.name)?.prettyPrint ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
