package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_CHARGING
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import org.nekomanga.domain.library.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_NOT_COMPLETED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_NOT_STARTED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_COMPLETED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_DROPPED
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_ON_HOLD
import org.nekomanga.domain.library.LibraryPreferences.Companion.MANGA_TRACKING_PLAN_TO_READ
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.screens.settings.widgets.TriStateListDialog

internal class LibrarySettingsScreen(
    incognitoMode: Boolean,
    val libraryPreferences: LibraryPreferences,
    onNavigationIconClick: () -> Unit,
    val categories: PersistentList<CategoryItem>,
    val viewModelScope: CoroutineScope,
    val onAddEditCategoryClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick, incognitoMode) {

    override fun getTitleRes(): Int = R.string.library

    @Composable
    override fun getPreferences(): PersistentList<Preference> {
        val context = LocalContext.current

        return persistentListOf(
            generalGroup(context),
            categoriesGroup(categories),
            globalUpdateGroup(context, categories),
        )
    }

    @Composable
    private fun generalGroup(context: Context): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.general),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = libraryPreferences.enableLocalChapters(),
                        title = stringResource(R.string.enable_local_manga),
                        subtitle = stringResource(R.string.enable_local_manga_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = libraryPreferences.removeArticles(),
                        title = stringResource(R.string.sort_by_ignoring_articles),
                        subtitle = stringResource(R.string.when_sorting_ignore_articles),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = libraryPreferences.dynamicCover(),
                        title = stringResource(R.string.dynamic_cover),
                        subtitle = stringResource(R.string.dynamic_cover_summary),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(R.string.display_options_can_be)
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = libraryPreferences.chapterScanlatorFilterOption(),
                        title = stringResource(R.string.chapter_scanlator_filter_option),
                        subtitleProvider = { value, options ->
                            when (value == 0) {
                                true -> stringResource(R.string.chapter_filter_all_summary)
                                false -> stringResource(R.string.chapter_filter_any_summary)
                            }
                        },
                        entries =
                            persistentMapOf(
                                0 to stringResource(R.string.chapter_filter_all),
                                1 to stringResource(R.string.chapter_filter_any),
                            ),
                    ),
                ),
        )
    }

    @Composable
    private fun categoriesGroup(
        categories: PersistentList<CategoryItem>
    ): Preference.PreferenceGroup {
        val alwaysAsk = Pair(-1, stringResource(R.string.always_ask))
        val nonSystemCategories =
            remember(categories) { categories.filterNot { it.isSystemCategory } }
        val categoryMap =
            remember(categories) {
                (listOf(alwaysAsk) + categories.map { it.id to it.name }).toMap().toImmutableMap()
            }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.categories),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title =
                            if (nonSystemCategories.isNotEmpty())
                                stringResource(R.string.edit_categories)
                            else stringResource(R.string.add_categories),
                        subtitle =
                            if (nonSystemCategories.isNotEmpty()) {
                                pluralStringResource(
                                    R.plurals.category_plural,
                                    nonSystemCategories.size,
                                    nonSystemCategories.size,
                                )
                            } else {
                                null
                            },
                        onClick = onAddEditCategoryClick,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = libraryPreferences.defaultCategory(),
                        title = stringResource(R.string.default_category),
                        entries = categoryMap,
                    ),
                ),
        )
    }

    @Composable
    private fun globalUpdateGroup(
        context: Context,
        allCategoryList: PersistentList<CategoryItem>,
    ): Preference.PreferenceGroup {

        val libraryUpdateInterval by libraryPreferences.updateInterval().collectAsState()

        val showLibraryUpdateRestrictions by
            remember(libraryUpdateInterval) { mutableStateOf(libraryUpdateInterval > 0) }

        val includedCategories by libraryPreferences.whichCategoriesToUpdate().collectAsState()
        val excludedCategories by libraryPreferences.whichCategoriesToExclude().collectAsState()

        var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(R.string.categories),
                items = allCategoryList,
                initialChecked =
                    includedCategories.mapNotNull { id ->
                        allCategoryList.find { it.id.toString() == id }
                    },
                initialInversed =
                    excludedCategories.mapNotNull { id ->
                        allCategoryList.find { it.id.toString() == id }
                    },
                itemLabel = { it.name },
                onDismissRequest = { showCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    libraryPreferences
                        .whichCategoriesToUpdate()
                        .set(newIncluded.map { it.id.toString() }.toSet())
                    libraryPreferences
                        .whichCategoriesToExclude()
                        .set(newExcluded.map { it.id.toString() }.toSet())
                    showCategoriesDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.global_updates),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = libraryPreferences.updateInterval(),
                        title = stringResource(R.string.library_update_frequency),
                        entries =
                            persistentMapOf(
                                0 to stringResource(R.string.manual),
                                12 to stringResource(R.string.every_12_hours),
                                24 to stringResource(R.string.daily),
                                48 to stringResource(R.string.every_2_days),
                                72 to stringResource(R.string.every_3_days),
                                168 to stringResource(R.string.weekly),
                            ),
                        onValueChanged = {
                            viewModelScope.launchNonCancellable {
                                val interval = libraryPreferences.updateInterval().get()
                                LibraryUpdateJob.setupTask(context, interval)
                            }
                            true
                        },
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        enabled = showLibraryUpdateRestrictions,
                        pref = libraryPreferences.autoUpdateDeviceRestrictions(),
                        title = stringResource(R.string.library_update_device_restriction),
                        subtitle = stringResource(R.string.restrictions_),
                        entries =
                            persistentMapOf(
                                DEVICE_ONLY_ON_WIFI to stringResource(R.string.connected_to_wifi),
                                DEVICE_NETWORK_NOT_METERED to
                                    stringResource(R.string.network_not_metered),
                                DEVICE_CHARGING to stringResource(R.string.charging),
                            ),
                        onValueChanged = {
                            viewModelScope.launchNonCancellable {
                                LibraryUpdateJob.setupTask(context)
                            }
                            true
                        },
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        pref = libraryPreferences.autoUpdateMangaRestrictions(),
                        title = stringResource(R.string.smart_library_update_restrictions),
                        entries =
                            persistentMapOf(
                                MANGA_HAS_UNREAD to
                                    stringResource(R.string.smart_library_has_unread),
                                MANGA_NOT_STARTED to
                                    stringResource(R.string.smart_library_has_not_started),
                                MANGA_NOT_COMPLETED to
                                    stringResource(R.string.smart_library_status_is_completed),
                                MANGA_TRACKING_PLAN_TO_READ to
                                    stringResource(R.string.smart_library_tracking_is_plan_to_read),
                                MANGA_TRACKING_DROPPED to
                                    stringResource(R.string.smart_library_tracking_is_dropped),
                                MANGA_TRACKING_ON_HOLD to
                                    stringResource(R.string.smart_library_tracking_is_on_hold),
                                MANGA_TRACKING_COMPLETED to
                                    stringResource(R.string.smart_library_tracking_is_completed),
                            ),
                        subtitle = stringResource(R.string.restrictions_),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = libraryPreferences.skipMangaMetadataDuringUpdate(),
                        title = stringResource(R.string.skip_manga_metadata_title),
                        subtitle = stringResource(R.string.skip_manga_metadata_description),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = libraryPreferences.prioritizeLibraryUpdates(),
                        title = stringResource(R.string.prioritize_library_title),
                        subtitle = stringResource(R.string.prioritize_library_description),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.categories),
                        subtitle =
                            getCategoriesLabel(
                                allCategoryList,
                                includedCategories,
                                excludedCategories,
                            ),
                        onClick = { showCategoriesDialog = true },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = libraryPreferences.updateCovers(),
                        title = stringResource(R.string.auto_refresh_covers),
                        subtitle = stringResource(R.string.auto_refresh_covers_summary),
                    ),
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(
                    title = stringResource(R.string.enable_local_manga),
                    subtitle = stringResource(R.string.enable_local_manga_summary),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.sort_by_ignoring_articles),
                    subtitle = stringResource(R.string.when_sorting_ignore_articles),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.dynamic_cover),
                    subtitle = stringResource(R.string.dynamic_cover_summary),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.display_options_can_be),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.chapter_scanlator_filter_option),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.add_edit_categories),
                    group = stringResource(R.string.categories),
                ),
                SearchTerm(
                    title = stringResource(R.string.default_category),
                    group = stringResource(R.string.categories),
                ),
                SearchTerm(
                    title = stringResource(R.string.global_updates),
                    group = stringResource(R.string.global_updates),
                ),
                SearchTerm(
                    title = stringResource(R.string.library_update_device_restriction),
                    group = stringResource(R.string.global_updates),
                ),
                SearchTerm(
                    title = stringResource(R.string.smart_library_update_restrictions),
                    group = stringResource(R.string.global_updates),
                ),
                SearchTerm(
                    title = stringResource(R.string.skip_manga_metadata_title),
                    subtitle = stringResource(R.string.skip_manga_metadata_description),
                    group = stringResource(R.string.global_updates),
                ),
                SearchTerm(
                    title = stringResource(R.string.prioritize_library_title),
                    subtitle = stringResource(R.string.prioritize_library_description),
                    group = stringResource(R.string.global_updates),
                ),
                SearchTerm(
                    title = stringResource(R.string.categories),
                    group = stringResource(R.string.global_updates),
                ),
                SearchTerm(
                    title = stringResource(R.string.auto_refresh_covers),
                    stringResource(R.string.auto_refresh_covers_summary),
                    group = stringResource(R.string.global_updates),
                ),
            )
        }
    }
}
