package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.screens.settings.widgets.TriStateListDialog

internal class DownloadSettingsScreen(
    val preferences: PreferencesHelper,
    val readerPreferences: ReaderPreferences,
    val allCategories: PersistentList<CategoryItem>,
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {
    override fun getTitleRes(): Int = R.string.downloads

    @Composable
    override fun getPreferences(): PersistentList<Preference> {

        return persistentListOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = preferences.downloadOnlyOverWifi(),
                title = stringResource(R.string.only_download_over_wifi),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = preferences.saveChaptersAsCBZ(),
                title = stringResource(R.string.save_chapters_as_cbz),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = readerPreferences.splitTallImages(),
                title = stringResource(R.string.split_tall_images),
                subtitle = stringResource(R.string.split_tall_images_summary),
            ),
            removeAfterReadGroup(),
            downloadNewChaptersGroup(),
            downloadAheadGroup(),
            automaticRemovalGroup(),
        )
    }

    @Composable
    fun removeAfterReadGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.remove_after_read),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = preferences.removeAfterMarkedAsRead(),
                        title = stringResource(R.string.remove_when_marked_as_read),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = preferences.removeAfterReadSlots(),
                        title = stringResource(R.string.remove_after_read),
                        entries =
                            mapOf(
                                    -1 to stringResource(R.string.never),
                                    0 to stringResource(R.string.last_read_chapter),
                                    1 to stringResource(R.string.second_to_last),
                                    2 to stringResource(R.string.third_to_last),
                                    3 to stringResource(R.string.fourth_to_last),
                                    4 to stringResource(R.string.fifth_to_last),
                                )
                                .toMap()
                                .toPersistentMap(),
                    ),
                ),
        )
    }

    @Composable
    fun downloadNewChaptersGroup(): Preference.PreferenceGroup {

        var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }

        val includedCategories by preferences.downloadNewChaptersInCategories().collectAsState()
        val excludedCategories by preferences.excludeCategoriesInDownloadNew().collectAsState()

        if (showCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(R.string.categories),
                items = allCategories,
                initialChecked =
                    includedCategories.mapNotNull { id ->
                        allCategories.find { it.id.toString() == id }
                    },
                initialInversed =
                    excludedCategories.mapNotNull { id ->
                        allCategories.find { it.id.toString() == id }
                    },
                itemLabel = { it.name },
                onDismissRequest = { showCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    preferences
                        .downloadNewChaptersInCategories()
                        .set(newIncluded.map { it.id.toString() }.toSet())
                    preferences
                        .excludeCategoriesInDownloadNew()
                        .set(newExcluded.map { it.id.toString() }.toSet())
                    showCategoriesDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.download_new_chapters),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = preferences.downloadNewChapters(),
                        title = stringResource(R.string.download_new_chapters),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.categories),
                        subtitle =
                            getCategoriesLabel(
                                allCategories,
                                includedCategories,
                                excludedCategories,
                            ),
                        onClick = { showCategoriesDialog = true },
                        enabled = preferences.downloadNewChapters().collectAsState().value,
                    ),
                ),
        )
    }

    @Composable
    fun downloadAheadGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.download_ahead),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = preferences.autoDownloadWhileReading(),
                        title = stringResource(R.string.auto_download_while_reading),
                        entries =
                            mapOf(
                                    0 to stringResource(R.string.never),
                                    2 to pluralStringResource(R.plurals.next_unread_chapters, 2, 2),
                                    3 to pluralStringResource(R.plurals.next_unread_chapters, 3, 3),
                                    5 to pluralStringResource(R.plurals.next_unread_chapters, 5, 5),
                                    10 to
                                        pluralStringResource(R.plurals.next_unread_chapters, 10, 10),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(R.string.download_ahead_info)
                    ),
                ),
        )
    }

    @Composable
    fun automaticRemovalGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.automatic_removal),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = preferences.deleteRemovedChapters(),
                        title = stringResource(R.string.delete_removed_chapters),
                        subtitle = stringResource(R.string.delete_downloaded_if_removed_online),
                        entries =
                            mapOf(
                                    0 to stringResource(R.string.ask_on_chapters_page),
                                    1 to stringResource(R.string.always_keep),
                                    2 to stringResource(R.string.always_delete),
                                )
                                .toPersistentMap(),
                    )
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(title = stringResource(R.string.only_download_over_wifi)),
                SearchTerm(title = stringResource(R.string.save_chapters_as_cbz)),
                SearchTerm(title = stringResource(R.string.split_tall_images_summary)),
                SearchTerm(
                    title = stringResource(R.string.remove_when_marked_as_read),
                    group = stringResource(R.string.remove_after_read),
                ),
                SearchTerm(
                    title = stringResource(R.string.remove_after_read),
                    group = stringResource(R.string.remove_after_read),
                ),
                SearchTerm(
                    title = stringResource(R.string.download_new_chapters),
                    group = stringResource(R.string.download_new_chapters),
                ),
                SearchTerm(
                    title = stringResource(R.string.categories),
                    group = stringResource(R.string.download_new_chapters),
                ),
                SearchTerm(
                    title = stringResource(R.string.auto_download_while_reading),
                    group = stringResource(R.string.download_ahead),
                ),
                SearchTerm(
                    title = stringResource(R.string.delete_removed_chapters),
                    subtitle = stringResource(R.string.delete_downloaded_if_removed_online),
                    group = stringResource(R.string.automatic_removal),
                ),
            )
        }
    }
}
