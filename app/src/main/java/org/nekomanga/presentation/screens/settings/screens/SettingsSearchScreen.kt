package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.setting.SettingsScreenType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.screens.EmptyScreen
import org.nekomanga.presentation.screens.Screens
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.screens.settings.widgets.TextPreferenceWidget
import org.nekomanga.presentation.theme.Size

@Composable
fun SettingsSearchScreen(
    modifier: Modifier = Modifier,
    onNavigationIconClicked: () -> Unit,
    navigate: (Any) -> Unit,
) {
    // Hide keyboard on change screen
    val softKeyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    DisposableEffect(Unit) { onDispose { softKeyboardController?.hide() } }

    // Hide keyboard on outside text field is touched
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    // Request text field focus on launch
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

    var searchText: String? by remember { mutableStateOf("") }

    NekoScaffold(
        type = NekoScaffoldType.SearchOutline,
        focusRequester = focusRequester,
        onNavigationIconClicked = onNavigationIconClicked,
        searchPlaceHolder = stringResource(R.string.search_settings),
        onSearch = { searchText = it },
    ) { contentPadding ->
        contentPadding.toString()
        SearchResult(
            searchKey = searchText ?: "",
            listState = listState,
            contentPadding = contentPadding,
        ) { result ->
            SearchableSettings.highlightKey = result.highlightKey
            val route =
                when (result.settingScreenType) {
                    SettingsScreenType.Advanced -> Screens.Settings.Advanced
                    SettingsScreenType.Appearance -> Screens.Settings.Appearance
                    SettingsScreenType.DataAndStorage -> Screens.Settings.DataStorage
                    SettingsScreenType.General -> Screens.Settings.General
                    SettingsScreenType.Library -> Screens.Settings.Library
                    SettingsScreenType.MangaDex -> Screens.Settings.MangaDex
                    SettingsScreenType.MergeSource -> Screens.Settings.MergeSource
                    SettingsScreenType.Reader -> Screens.Settings.Reader
                    SettingsScreenType.Security -> Screens.Settings.Security
                    SettingsScreenType.Tracking -> Screens.Settings.Tracking
                }
            navigate(route)
        }
    }
}

@Composable
private fun SearchResult(
    searchKey: String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    onItemClick: (SearchResultItem) -> Unit,
) {
    if (searchKey.isEmpty()) return

    val searchTerms = searchTerms()

    val result by
        produceState<List<SearchResultItem>?>(initialValue = null, searchKey) {
            value =
                searchTerms
                    .asSequence()
                    .flatMap { settingsData ->
                        settingsData.contents
                            .asSequence()
                            .filter { searchTerm ->
                                val inTitle = searchTerm.title.contains(searchKey, true)
                                val inSummary =
                                    searchTerm.subtitle?.contains(searchKey, true) == true
                                inTitle || inSummary
                            }
                            // Map result data
                            .map { searchTerm ->
                                SearchResultItem(
                                    settingScreenType = settingsData.settingScreenType,
                                    searchTerm = searchTerm,
                                    highlightKey = searchKey,
                                )
                            }
                    }
                    .take(10) // Just take top 10 result for quicker result
                    .toList()
        }

    Crossfade(targetState = result, label = "results") {
        when {
            it == null -> {}
            it.isEmpty() -> {
                EmptyScreen(message = stringResource(R.string.no_results_found))
            }
            else -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize().padding(top = Size.medium),
                    state = listState,
                    contentPadding = contentPadding,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(items = it) { item ->
                        TextPreferenceWidget(
                            title = item.searchTerm.title,
                            subtitle = item.searchTerm.subtitle,
                            onPreferenceClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
private fun searchTerms() =
    persistentListOf<SettingsData>(
        SettingsData(
            settingScreenType = SettingsScreenType.General,
            contents = GeneralSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Appearance,
            contents = AppearanceSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Library,
            contents = LibrarySettingsScreen.getSearchTerms(),
        ),
    )

private data class SettingsData(
    val settingScreenType: SettingsScreenType,
    val contents: ImmutableList<SearchTerm>,
)

private data class SearchResultItem(
    val settingScreenType: SettingsScreenType,
    val searchTerm: SearchTerm,
    val highlightKey: String,
)
