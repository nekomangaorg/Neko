package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.jobs.follows.StatusSyncJob
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.setting.MangaDexSettingsViewModel
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.openInFirefox
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.components.dialog.LogoutDialog
import org.nekomanga.presentation.components.dialog.PullMangaDexFollowDialog
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.screens.settings.widgets.TriStateListDialog

internal class MangaDexSettingsScreen(
    onNavigationIconClick: () -> Unit,
    val mangaDexPreferences: MangaDexPreferences,
    val mangaDexSettingsState: MangaDexSettingsViewModel.MangaDexSettingsState,
    val deleteSavedFilters: () -> Unit,
    val logout: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.site_specific_settings

    @Composable
    override fun getPreferences(): PersistentList<Preference> {
        val context = LocalContext.current

        return persistentListOf(
                generalGroup(context, deleteSavedFilters, mangaDexPreferences),
                chapterGroup(
                    context,
                    mangaDexPreferences,
                    mangaDexSettingsState.blockedGroups,
                    mangaDexSettingsState.blockedUploaders,
                ),
                imageGroup(mangaDexPreferences),
                libraryGroup(context, mangaDexPreferences),
            )
            .toPersistentList()
    }

    @Composable
    fun generalGroup(
        context: Context,
        deleteSavedFilters: () -> Unit,
        mangaDexPreferences: MangaDexPreferences,
    ): Preference.PreferenceGroup {

        var showDeleteFilterDialog by rememberSaveable { mutableStateOf(false) }

        if (showDeleteFilterDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.delete_saved_filters),
                confirmButton = stringResource(R.string.delete),
                onConfirm = {
                    deleteSavedFilters()
                    context.toast(R.string.saved_filters_deleted)
                },
                onDismiss = { showDeleteFilterDialog = false },
            )
        }

        var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

        if (showLogoutDialog) {
            LogoutDialog(
                sourceName = stringResource(R.string.site_specific_settings),
                onDismiss = { showLogoutDialog = false },
                onConfirm = logout,
            )
        }

        val loginText =
            when (mangaDexSettingsState.isLoggedIn) {
                true -> stringResource(R.string.sign_out)
                false -> stringResource(R.string.sign_in)
            }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.general),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SitePreference(
                        title = loginText,
                        isLoggedIn = mangaDexSettingsState.isLoggedIn,
                        login = {
                            when (BuildConfig.DEBUG) {
                                true -> context.openInFirefox(mangaDexSettingsState.loginUrl)
                                false -> context.openInBrowser(mangaDexSettingsState.loginUrl)
                            }
                        },
                        logout = { showLogoutDialog = true },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(R.string.show_content_rating_filter_in_search),
                        pref = mangaDexPreferences.showContentRatingFilter(),
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        title = stringResource(R.string.content_rating_title),
                        subtitleProvider = { selected, all ->
                            stringResource(R.string.content_rating_summary) +
                                selected.joinToString()
                        },
                        entries =
                            persistentMapOf(
                                MdConstants.ContentRating.safe to
                                    stringResource(R.string.content_rating_safe),
                                MdConstants.ContentRating.suggestive to
                                    stringResource(R.string.content_rating_suggestive),
                                MdConstants.ContentRating.erotica to
                                    stringResource(R.string.content_rating_erotica),
                                MdConstants.ContentRating.pornographic to
                                    stringResource(R.string.content_rating_pornographic),
                            ),
                        pref = mangaDexPreferences.visibleContentRatings(),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.delete_saved_filters),
                        subtitle = stringResource(R.string.delete_saved_filters_description),
                        onClick = { showDeleteFilterDialog = true },
                    ),
                ),
        )
    }

    @Composable
    fun chapterGroup(
        context: Context,
        mangaDexPreferences: MangaDexPreferences,
        blockedGroups: ImmutableSet<String>,
        blockedUploaders: ImmutableSet<String>,
    ): Preference.PreferenceGroup {

        var showBlockedGroupDialog by rememberSaveable { mutableStateOf(false) }
        var showBlockedUploaderDialog by rememberSaveable { mutableStateOf(false) }

        if (showBlockedGroupDialog) {
            TriStateListDialog(
                title = stringResource(R.string.unblock_group),
                negativeOnly = true,
                items = blockedGroups.sorted().toList(),
                initialChecked = emptyList(),
                initialInversed = emptyList(),
                itemLabel = { it },
                onDismissRequest = { showBlockedGroupDialog = false },
                onValueChanged = { _, newExcluded ->
                    mangaDexPreferences
                        .blockedGroups()
                        .set(blockedGroups.minus(newExcluded.toSet()))
                    showBlockedGroupDialog = false
                },
            )
        }
        if (showBlockedUploaderDialog) {
            TriStateListDialog(
                title = stringResource(R.string.unblock_uploader),
                negativeOnly = true,
                items = blockedUploaders.sorted().toList(),
                initialChecked = emptyList(),
                initialInversed = emptyList(),
                itemLabel = { it },
                onDismissRequest = { showBlockedUploaderDialog = false },
                onValueChanged = { _, newExcluded ->
                    mangaDexPreferences
                        .blockedUploaders()
                        .set(blockedUploaders.minus(newExcluded.toSet()))
                    showBlockedUploaderDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.chapter_group),
            preferenceItems =
                persistentListOf(
                        Preference.PreferenceItem.SwitchPreference(
                            pref = mangaDexPreferences.includeUnavailableChapters(),
                            title = stringResource(R.string.include_unavailable),
                            subtitle = stringResource(R.string.include_unavailable_summary),
                        ),
                        Preference.PreferenceItem.MultiSelectListPreference(
                            title = stringResource(R.string.show_languages),
                            subtitleProvider = { enabledLanguages, allLanguages ->
                                enabledLanguages
                                    .mapNotNull { lang -> allLanguages[lang] }
                                    .sorted()
                                    .joinToString(", ")
                            },
                            entries =
                                MdLang.entries
                                    .associate { lang -> lang.lang to lang.prettyPrint }
                                    .toImmutableMap(),
                            pref = mangaDexPreferences.enabledChapterLanguages(),
                        ),
                        Preference.PreferenceItem.TextPreference(
                            title = stringResource(R.string.currently_blocked_groups),
                            subtitle = stringResource(R.string.currently_blocked_description),
                            onClick = {
                                if (blockedGroups.isEmpty()) {
                                    context.toast(R.string.no_blocked_groups)
                                } else {
                                    showBlockedGroupDialog = true
                                }
                            },
                        ),
                        Preference.PreferenceItem.TextPreference(
                            title = stringResource(R.string.currently_blocked_uploaders),
                            subtitle = stringResource(R.string.currently_blocked_description),
                            onClick = {
                                if (blockedUploaders.isEmpty()) {
                                    context.toast(R.string.no_blocked_uploaders)
                                } else {
                                    showBlockedUploaderDialog = true
                                }
                            },
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            pref = mangaDexPreferences.readingSync(),
                            title = stringResource(R.string.reading_sync),
                            subtitle = stringResource(R.string.reading_sync_summary),
                        ),
                    )
                    .toPersistentList(),
        )
    }

    @Composable
    fun imageGroup(mangaDexPreferences: MangaDexPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.image_group),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(R.string.data_saver),
                        subtitle = stringResource(R.string.data_saver_summary),
                        pref = mangaDexPreferences.dataSaver(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(R.string.use_port_443_title),
                        subtitle = stringResource(R.string.use_port_443_summary),
                        pref = mangaDexPreferences.usePort443ForImageServer(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        title = stringResource(R.string.cover_quality),
                        entries =
                            persistentMapOf(
                                0 to stringResource(R.string.original_cover_quality),
                                1 to stringResource(R.string.medium_cover_quality),
                                2 to stringResource(R.string.low_cover_quality),
                            ),
                        pref = mangaDexPreferences.coverQuality(),
                    ),
                ),
        )
    }

    @Composable
    fun libraryGroup(
        context: Context,
        mangaDexPreferences: MangaDexPreferences,
    ): Preference.PreferenceGroup {
        var pullFollowsFromMangaDexToLibrary by rememberSaveable { mutableStateOf(false) }
        // var pushFollowsFromLibraryToMangaDex by rememberSaveable { mutableStateOf(false) }

        if (pullFollowsFromMangaDexToLibrary) {
            PullMangaDexFollowDialog(
                onDismiss = { pullFollowsFromMangaDexToLibrary = false },
                onConfirm = { selectedIndicies ->
                    mangaDexPreferences.mangaDexPullToLibraryIndices().set(selectedIndicies)
                    StatusSyncJob.startNow(context, StatusSyncJob.entireFollowsFromDex)
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.library),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        title = stringResource(R.string.auto_add_to_mangadex_library),
                        entries =
                            persistentMapOf(
                                0 to stringResource(R.string.disabled),
                                1 to stringResource(R.string.follows_plan_to_read),
                                2 to stringResource(R.string.follows_on_hold),
                                3 to stringResource(R.string.follows_reading),
                            ),
                        pref = mangaDexPreferences.autoAddToMangaDexLibrary(),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.pull_follows_to_library),
                        subtitle = stringResource(R.string.pull_follows_to_library_summary),
                        onClick = { pullFollowsFromMangaDexToLibrary = true },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.push_favorites_to_mangadex),
                        subtitle = stringResource(R.string.push_favorites_to_mangadex_summary),
                        onClick = {
                            StatusSyncJob.startNow(context, StatusSyncJob.entireLibraryToDex)
                        },
                    ),
                ),
        )
    }

    // image group

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(
                    title = stringResource(R.string.show_content_rating_filter_in_search),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.content_rating_title),
                    subtitle = stringResource(R.string.content_rating_summary),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.delete_saved_filters),
                    subtitle = stringResource(R.string.delete_saved_filters_description),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.include_unavailable),
                    subtitle = stringResource(R.string.include_unavailable_summary),
                    group = stringResource(R.string.chapter_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.show_languages),
                    group = stringResource(R.string.chapter_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.currently_blocked_groups),
                    subtitle = stringResource(R.string.currently_blocked_description),
                    group = stringResource(R.string.chapter_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.currently_blocked_uploaders),
                    subtitle = stringResource(R.string.currently_blocked_description),
                    group = stringResource(R.string.chapter_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.reading_sync),
                    subtitle = stringResource(R.string.reading_sync_summary),
                    group = stringResource(R.string.chapter_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.data_saver),
                    subtitle = stringResource(R.string.data_saver_summary),
                    group = stringResource(R.string.image_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.use_port_443_title),
                    subtitle = stringResource(R.string.use_port_443_summary),
                    group = stringResource(R.string.image_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.cover_quality),
                    group = stringResource(R.string.image_group),
                ),
                SearchTerm(
                    title = stringResource(R.string.auto_add_to_mangadex_library),
                    group = stringResource(R.string.library),
                ),
                SearchTerm(
                    title = stringResource(R.string.pull_follows_to_library),
                    subtitle = stringResource(R.string.pull_follows_to_library_summary),
                    group = stringResource(R.string.library),
                ),
                SearchTerm(
                    title = stringResource(R.string.push_favorites_to_mangadex),
                    subtitle = stringResource(R.string.push_favorites_to_mangadex_summary),
                    group = stringResource(R.string.library),
                ),
            )
        }
    }
}
