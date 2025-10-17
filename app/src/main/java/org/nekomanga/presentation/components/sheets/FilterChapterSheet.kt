package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.ChapterNumber
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.SourceOrder
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortType.UploadDate
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.SortRow
import org.nekomanga.presentation.components.TriStateCheckboxRow
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun FilterChapterSheet(
    themeColorState: ThemeColorState,
    sortFilter: MangaConstants.SortFilter,
    filter: MangaConstants.ChapterDisplay,
    scanlatorFilter: MangaConstants.ScanlatorFilter,
    sourceFilter: MangaConstants.ScanlatorFilter,
    languageFilter: MangaConstants.LanguageFilter,
    changeSort: (SortOption?) -> Unit,
    changeFilter: (MangaConstants.ChapterDisplayOptions?) -> Unit,
    changeScanlatorFilter: (MangaConstants.ScanlatorOption?) -> Unit,
    changeLanguageFilter: (MangaConstants.LanguageOption?) -> Unit,
    setAsGlobal: (MangaConstants.SetGlobal) -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .8

        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp)
            ) {
                item {
                    Sort(themeColorState = themeColorState, sortFilter, changeSort) {
                        setAsGlobal(MangaConstants.SetGlobal.Sort)
                    }
                }
                item {
                    Filter(themeColorState = themeColorState, filter, changeFilter) {
                        setAsGlobal(MangaConstants.SetGlobal.Filter)
                    }
                }
                item {
                    Scanlator(
                        themeColorState = themeColorState,
                        sourceFilter,
                        true,
                        changeScanlatorFilter,
                    )
                }
                item {
                    Scanlator(
                        themeColorState = themeColorState,
                        scanlatorFilter,
                        false,
                        changeScanlatorFilter,
                    )
                }
                item {
                    Language(
                        themeColorState = themeColorState,
                        languageFilter,
                        changeLanguageFilter,
                    )
                }
            }
        }
    }
}

@Composable
private fun Sort(
    themeColorState: ThemeColorState,
    sortFilter: MangaConstants.SortFilter,
    changeSort: (SortOption?) -> Unit,
    setGlobal: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.sort),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!sortFilter.matchesGlobalDefaults) {
                TextButton(onClick = setGlobal) {
                    Text(
                        text = stringResource(id = R.string.set_as_default),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColorState.primaryColor,
                    )
                }
                TextButton(onClick = { changeSort(null) }) {
                    Text(
                        text = stringResource(id = R.string.reset),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColorState.primaryColor,
                    )
                }
            }
        }
        Gap(Size.small)
        SortLine(
            themeColorState,
            SortOption(sortFilter.smartOrderSort, ChapterNumber),
            stringResource(id = R.string.by_smart_order),
            changeSort,
        )
        SortLine(
            themeColorState,
            SortOption(sortFilter.sourceOrderSort, SourceOrder),
            stringResource(id = R.string.by_source_order),
            changeSort,
        )
        SortLine(
            themeColorState,
            SortOption(sortFilter.uploadDateSort, UploadDate),
            stringResource(id = R.string.by_update_date),
            changeSort,
        )
    }
}

@Composable
private fun SortLine(
    themeColorState: ThemeColorState,
    state: SortOption,
    text: String,
    changeSort: (SortOption) -> Unit,
) {
    SortRow(
        modifier = Modifier.fillMaxWidth(),
        sortState = state.sortState,
        sortChanged = { newSortState -> changeSort(state.copy(sortState = newSortState)) },
        rowText = text,
        themeColorState = themeColorState,
    )
}

@Composable
private fun Filter(
    themeColorState: ThemeColorState,
    filter: MangaConstants.ChapterDisplay,
    changeFilter: (MangaConstants.ChapterDisplayOptions?) -> Unit,
    setGlobal: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.filter_and_display),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!filter.matchesGlobalDefaults) {
                TextButton(onClick = setGlobal) {
                    Text(
                        text = stringResource(id = R.string.set_as_default),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColorState.primaryColor,
                    )
                }
                TextButton(onClick = { changeFilter(null) }) {
                    Text(
                        text = stringResource(id = R.string.reset),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColorState.primaryColor,
                    )
                }
            }
        }
        CheckboxLine(
            themeColorState = themeColorState,
            checked = filter.showAll,
            disabledOnChecked = true,
            text = stringResource(id = R.string.show_all),
            onChecked = {
                changeFilter(
                    MangaConstants.ChapterDisplayOptions(
                        displayType = MangaConstants.ChapterDisplayType.All,
                        displayState = ToggleableState(!filter.showAll),
                    )
                )
            },
        )
        FilterLine(
            themeColorState = themeColorState,
            state =
                MangaConstants.ChapterDisplayOptions(
                    displayType = MangaConstants.ChapterDisplayType.Unread,
                    displayState = filter.unread,
                ),
            text = stringResource(id = R.string.show_unread_chapters),
            changeFilter = changeFilter,
        )
        FilterLine(
            themeColorState = themeColorState,
            state =
                MangaConstants.ChapterDisplayOptions(
                    displayType = MangaConstants.ChapterDisplayType.Downloaded,
                    displayState = filter.downloaded,
                ),
            text = stringResource(id = R.string.show_downloaded_chapters),
            changeFilter = changeFilter,
        )
        FilterLine(
            themeColorState = themeColorState,
            state =
                MangaConstants.ChapterDisplayOptions(
                    displayType = MangaConstants.ChapterDisplayType.Bookmarked,
                    displayState = filter.bookmarked,
                ),
            text = stringResource(id = R.string.show_bookmarked_chapters),
            changeFilter = changeFilter,
        )
        FilterLine(
            themeColorState = themeColorState,
            state =
                MangaConstants.ChapterDisplayOptions(
                    displayType = MangaConstants.ChapterDisplayType.Available,
                    displayState = filter.available,
                ),
            text = stringResource(id = R.string.show_available_chapters),
            changeFilter = changeFilter,
        )
        CheckboxLine(
            themeColorState = themeColorState,
            checked = filter.hideChapterTitles == ToggleableState.On,
            text = stringResource(id = R.string.hide_chapter_titles),
            onChecked = {
                changeFilter(
                    MangaConstants.ChapterDisplayOptions(
                        displayType = MangaConstants.ChapterDisplayType.HideTitles,
                        displayState =
                            when (filter.hideChapterTitles == ToggleableState.On) {
                                true -> ToggleableState.Off
                                false -> ToggleableState.On
                            },
                    )
                )
            },
        )
    }
}

@Composable
private fun FilterLine(
    themeColorState: ThemeColorState,
    state: MangaConstants.ChapterDisplayOptions,
    text: String,
    changeFilter: (MangaConstants.ChapterDisplayOptions) -> Unit,
) {
    TriStateCheckboxRow(
        modifier = Modifier.fillMaxWidth(),
        state = state.displayState,
        toggleState = { newState -> changeFilter(state.copy(displayState = newState)) },
        rowText = text,
        themeColorState = themeColorState,
    )
}

@Composable
private fun CheckboxLine(
    themeColorState: ThemeColorState,
    checked: Boolean,
    disabledOnChecked: Boolean = false,
    text: String,
    onChecked: () -> Unit = {},
) {
    CheckboxRow(
        modifier = Modifier.fillMaxWidth(),
        checkedState = checked,
        checkedChange = { onChecked() },
        rowText = text,
        themeColorState = themeColorState,
        disabled = disabledOnChecked && checked,
    )
}

@Composable
private fun Scanlator(
    themeColorState: ThemeColorState,
    scanlatorFilter: MangaConstants.ScanlatorFilter,
    isSourceFilter: Boolean,
    changeScanlatorFilter: (MangaConstants.ScanlatorOption?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text =
                    stringResource(
                        id =
                            if (isSourceFilter) R.string.filter_source
                            else R.string.filter_scanlators
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (scanlatorFilter.scanlators.any { it.disabled }) {
                TextButton(onClick = { changeScanlatorFilter(null) }) {
                    Text(
                        text = stringResource(id = R.string.reset),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColorState.primaryColor,
                    )
                }
            }
        }

        val enabled = scanlatorFilter.scanlators.size > 1

        scanlatorFilter.scanlators.forEach { scanlatorOption ->
            ScanlatorLine(
                themeColorState = themeColorState,
                enabledButton = enabled,
                scanlatorOption = scanlatorOption,
            ) {
                changeScanlatorFilter(scanlatorOption.copy(disabled = !scanlatorOption.disabled))
            }
        }
    }
}

@Composable
private fun ScanlatorLine(
    themeColorState: ThemeColorState,
    scanlatorOption: MangaConstants.ScanlatorOption,
    enabledButton: Boolean,
    changeScanlatorFilter: () -> Unit,
) {
    TriStateCheckboxRow(
        modifier = Modifier.fillMaxWidth(),
        disabled = !enabledButton,
        state =
            if (scanlatorOption.disabled) ToggleableState.Indeterminate else ToggleableState.Off,
        toggleState = { changeScanlatorFilter() },
        rowText = scanlatorOption.name,
        themeColorState = themeColorState,
    )
}

@Composable
private fun Language(
    themeColorState: ThemeColorState,
    languageFilter: MangaConstants.LanguageFilter,
    changeLanguageFilter: (MangaConstants.LanguageOption?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.filter_languages),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (languageFilter.languages.any { it.disabled }) {
                TextButton(onClick = { changeLanguageFilter(null) }) {
                    Text(
                        text = stringResource(id = R.string.reset),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColorState.primaryColor,
                    )
                }
            }
        }

        val enabled = languageFilter.languages.size > 1

        languageFilter.languages.forEach { languageOption ->
            LanguageLine(
                themeColorState = themeColorState,
                enabledButton = enabled,
                languageOption = languageOption,
            ) {
                changeLanguageFilter(languageOption.copy(disabled = !languageOption.disabled))
            }
        }
    }
}

@Composable
private fun LanguageLine(
    themeColorState: ThemeColorState,
    languageOption: MangaConstants.LanguageOption,
    enabledButton: Boolean,
    changeLanguageFilter: () -> Unit,
) {
    TriStateCheckboxRow(
        modifier = Modifier.fillMaxWidth(),
        disabled = !enabledButton,
        state = if (languageOption.disabled) ToggleableState.Indeterminate else ToggleableState.Off,
        toggleState = { changeLanguageFilter() },
        rowText = MdLang.fromIsoCode(languageOption.name)?.prettyPrint ?: "",
        themeColorState = themeColorState,
    )
}
