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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
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
    onBackPressed: () -> Unit,
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
        onNavigationIconClicked = onBackPressed,
        searchPlaceHolder = stringResource(R.string.search_settings),
        onSearch = { searchText = it },
    ) { contentPadding ->
        contentPadding.toString()
        SearchResult(
            searchKey = searchText ?: "",
            listState = listState,
            contentPadding = contentPadding,
        ) { result ->
            SearchableSetting.highlightKey = result.highlightKey
            navigate(result.route)
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

    val index = getIndex()
    val result by
        produceState<List<SearchResultItem>?>(initialValue = null, searchKey) {
            value =
                index
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
                                    route = settingsData.route,
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
    persistentMapOf<Any, ImmutableList<SearchTerm>>(
        Screens.Settings.General to GeneralSettingsScreen.getSearchTerms()
    )

@Composable
@NonRestartableComposable
private fun getIndex() =
    searchTerms().map { entry -> SettingsData(route = entry.key, contents = entry.value) }

private data class SettingsData(val route: Any, val contents: ImmutableList<SearchTerm>)

private data class SearchResultItem(
    val route: Any,
    val searchTerm: SearchTerm,
    val highlightKey: String,
)
