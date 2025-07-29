package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.setting.SettingsMangaDexViewModel
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.openInFirefox
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.PreferenceItem
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.screens.settings.widgets.TriStateListDialog

internal class MangaDexSettingsScreen(
    onNavigationIconClick: () -> Unit,
    val mangaDexPreferences: MangaDexPreferences,
    val mangaDexSettingsState: SettingsMangaDexViewModel.MangaDexSettingsState,
    val logout: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.site_specific_settings

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        val context = LocalContext.current
        // TODO do a dialog for logout
        val loginText =
            when (mangaDexSettingsState.isLoggedIn) {
                true ->
                    stringResource(
                        R.string.log_out_from_,
                        stringResource(R.string.site_specific_settings),
                    )
                false ->
                    stringResource(
                        R.string.log_in_to_,
                        stringResource(R.string.site_specific_settings),
                    )
            }

        return persistentListOf(
                Preference.PreferenceItem.SitePreference(
                    title = loginText,
                    isLoggedIn = mangaDexSettingsState.isLoggedIn,
                    login = {
                        when (BuildConfig.DEBUG) {
                            true -> context.openInFirefox(mangaDexSettingsState.loginUrl)
                            false -> context.openInBrowser(mangaDexSettingsState.loginUrl)
                        }
                    },
                    logout = logout,
                ),
                chapterGroup(context, mangaDexPreferences, mangaDexSettingsState.blockedGroups),
            )
            .toImmutableList()
    }

    @Composable
    fun chapterGroup(
        context: Context,
        mangaDexPreferences: MangaDexPreferences,
        blockedGroups: ImmutableSet<String>,
    ): Preference.PreferenceGroup {

        var showBlockedDialog by rememberSaveable { mutableStateOf(false) }
        if (showBlockedDialog) {
            TriStateListDialog(
                title = stringResource(R.string.unblock_group),
                negativeOnly = true,
                items = blockedGroups.sorted().toList(),
                initialChecked = emptyList(),
                initialInversed = emptyList(),
                itemLabel = { it },
                onDismissRequest = { showBlockedDialog = false },
                onValueChanged = { _, newExcluded ->
                    mangaDexPreferences
                        .blockedGroups()
                        .set(blockedGroups.minus(newExcluded.toSet()))
                    showBlockedDialog = false
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.chapter_group),
            preferenceItems =
                persistentListOf(
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
                            subtitle =
                                stringResource(R.string.currently_blocked_groups_description),
                            onClick = {
                                if (blockedGroups.isEmpty()) {
                                    context.toast(R.string.no_blocked_groups)
                                } else {
                                    showBlockedDialog = true
                                }
                            },
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            pref = mangaDexPreferences.readingSync(),
                            title = stringResource(R.string.reading_sync),
                            subtitle = stringResource(R.string.reading_sync_summary),
                        ),
                    )
                    .toImmutableList(),
        )
    }

    // image group

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf()
        }
    }
}
