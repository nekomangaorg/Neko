package org.nekomanga.presentation.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.ScreenBars
import eu.kanade.tachiyomi.ui.setting.SettingsScreenType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.icons.MergeIcon
import org.nekomanga.presentation.screens.EmptyScreen
import org.nekomanga.presentation.screens.Screens
import org.nekomanga.presentation.screens.settings.screens.AdvancedSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.AppearanceSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.DataStorageSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.DownloadSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.GeneralSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.LibrarySettingsScreen
import org.nekomanga.presentation.screens.settings.screens.MangaDexSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.MergeSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.ReaderSettingsScreen
import org.nekomanga.presentation.screens.settings.screens.SearchableSettings
import org.nekomanga.presentation.screens.settings.screens.SecuritySettingsScreen
import org.nekomanga.presentation.screens.settings.screens.TrackingSettingsScreen
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.screens.settings.widgets.TextPreferenceWidget
import org.nekomanga.presentation.theme.Size

@Composable
fun SettingsMainScreen(
    onNavigateClick: (NavKey) -> Unit,
    onNavigationIconClick: () -> Unit,
    incognitoMode: Boolean,
) {

    val updateTopBar = LocalBarUpdater.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var searchText: String by remember { mutableStateOf("") }

    val screenBars = remember {
        ScreenBars(
            topBar = {
                SettingsSearchTopBar(
                    onSearch = { searchText = it ?: "" },
                    incognitoMode = incognitoMode,
                    scrollBehavior = scrollBehavior,
                    onNavigationIconClick = onNavigationIconClick,
                )
            },
            scrollBehavior = scrollBehavior,
        )
    }

    DisposableEffect(Unit) {
        updateTopBar(screenBars)
        onDispose { updateTopBar(ScreenBars(id = screenBars.id, topBar = null)) }
    }

    if (searchText.isNotEmpty()) {
        SearchResult(searchKey = searchText) { result ->
            SearchableSettings.highlightKey = result.highlightKey
            val route =
                when (result.settingScreenType) {
                    SettingsScreenType.Advanced -> Screens.Settings.Advanced
                    SettingsScreenType.Appearance -> Screens.Settings.Appearance
                    SettingsScreenType.DataAndStorage -> Screens.Settings.DataStorage
                    SettingsScreenType.Downloads -> Screens.Settings.Downloads
                    SettingsScreenType.General -> Screens.Settings.General
                    SettingsScreenType.Library -> Screens.Settings.Library
                    SettingsScreenType.MangaDex -> Screens.Settings.MangaDex
                    SettingsScreenType.MergeSource -> Screens.Settings.MergeSource
                    SettingsScreenType.Reader -> Screens.Settings.Reader
                    SettingsScreenType.Security -> Screens.Settings.Security
                    SettingsScreenType.Tracking -> Screens.Settings.Tracking
                }
            onNavigateClick(route)
        }
    } else {
        mainContent(onNavigateClick = onNavigateClick)
    }
}

@Composable
private fun mainContent(onNavigateClick: (NavKey) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.general),
                icon = Icons.Outlined.Tune,
                onClick = { onNavigateClick(Screens.Settings.General) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.appearance),
                icon = Icons.Outlined.Palette,
                onClick = { onNavigateClick(Screens.Settings.Appearance) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.library),
                icon = Icons.Outlined.CollectionsBookmark,
                onClick = { onNavigateClick(Screens.Settings.Library) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.data_storage),
                icon = Icons.Outlined.Folder,
                onClick = { onNavigateClick(Screens.Settings.DataStorage) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.site_specific_settings),
                icon = Icons.Outlined.Public,
                onClick = { onNavigateClick(Screens.Settings.MangaDex) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.merge_source_settings),
                icon = MergeIcon,
                onClick = { onNavigateClick(Screens.Settings.MergeSource) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.reader),
                icon = Icons.AutoMirrored.Default.ChromeReaderMode,
                onClick = { onNavigateClick(Screens.Settings.Reader) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.downloads),
                icon = Icons.Outlined.Download,
                onClick = { onNavigateClick(Screens.Settings.Downloads) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.tracking),
                icon = Icons.Outlined.Autorenew,
                onClick = { onNavigateClick(Screens.Settings.Tracking) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.security),
                icon = Icons.Outlined.Security,
                onClick = { onNavigateClick(Screens.Settings.Security) },
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.advanced),
                icon = Icons.Outlined.Code,
                onClick = { onNavigateClick(Screens.Settings.Advanced) },
            )
        }
        if (BuildConfig.DEBUG) {
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.debug),
                    icon = Icons.Outlined.BugReport,
                    onClick = { onNavigateClick(Screens.Settings.Debug) },
                )
            }
        }
    }
}

@Composable
private fun SearchResult(
    searchKey: String,
    modifier: Modifier = Modifier,
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
                                    settingsStringTitle = settingsData.settingsStringTitle,
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
                EmptyScreen(message = UiText.StringResource(resourceId = R.string.no_results_found))
            }
            else -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize().padding(top = Size.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    itemsIndexed(
                        items = it,
                        key = { index, item ->
                            "$index-${item.searchTerm.title}-${item.searchTerm.subtitle}"
                        },
                    ) { index, item ->
                        TextPreferenceWidget(
                            title = item.searchTerm.title,
                            subtitle = item.searchTerm.subtitle,
                            footer = item.footer(),
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
    persistentListOf(
        SettingsData(
            settingScreenType = SettingsScreenType.General,
            settingsStringTitle = stringResource(R.string.general),
            contents = GeneralSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Appearance,
            settingsStringTitle = stringResource(R.string.appearance),
            contents = AppearanceSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Library,
            settingsStringTitle = stringResource(R.string.library),
            contents = LibrarySettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.DataAndStorage,
            settingsStringTitle = stringResource(R.string.data_storage),
            contents = DataStorageSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.MangaDex,
            settingsStringTitle = stringResource(R.string.site_specific_settings),
            contents = MangaDexSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.MergeSource,
            settingsStringTitle = stringResource(R.string.merge_source_settings),
            contents = MergeSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Reader,
            settingsStringTitle = stringResource(R.string.reader_settings),
            contents = ReaderSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Downloads,
            settingsStringTitle = stringResource(R.string.downloads),
            contents = DownloadSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Tracking,
            settingsStringTitle = stringResource(R.string.tracking),
            contents = TrackingSettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Security,
            settingsStringTitle = stringResource(R.string.security),
            contents = SecuritySettingsScreen.getSearchTerms(),
        ),
        SettingsData(
            settingScreenType = SettingsScreenType.Advanced,
            settingsStringTitle = stringResource(R.string.advanced),
            contents = AdvancedSettingsScreen.getSearchTerms(),
        ),
    )

private data class SettingsData(
    val settingScreenType: SettingsScreenType,
    val settingsStringTitle: String,
    val contents: PersistentList<SearchTerm>,
)

private data class SearchResultItem(
    val settingsStringTitle: String,
    val settingScreenType: SettingsScreenType,
    val searchTerm: SearchTerm,
    val highlightKey: String,
) {
    fun footer(): String {
        return listOf(settingsStringTitle, searchTerm.group).mapNotNull { it }.joinToString(" → ")
    }
}
