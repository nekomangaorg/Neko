package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.merged.suwayomi.LoginMode
import eu.kanade.tachiyomi.ui.setting.MergeLoginEvent
import eu.kanade.tachiyomi.ui.setting.MergeScreenState
import eu.kanade.tachiyomi.ui.setting.MergeScreenType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.presentation.components.dialog.LoginDialog
import org.nekomanga.presentation.components.dialog.LogoutDialog
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class MergeSettingsScreen(
    incognitoMode: Boolean,
    onNavigationIconClick: () -> Unit,
    val komgaState: MergeScreenState,
    val suwayomiState: MergeScreenState,
    val logout: (MergeScreenType) -> Unit,
    val login: (MergeScreenType, String, String, String) -> Unit,
    val loginEvent: SharedFlow<MergeLoginEvent>,
    val preferences: PreferencesHelper,
) : SearchableSettings(onNavigationIconClick, incognitoMode) {

    override fun getTitleRes(): Int = R.string.merge_source_settings

    @Composable
    override fun getPreferences(): PersistentList<Preference> {
        return persistentListOf(
            mergeGroup(
                mergeState = komgaState,
                sourceName = stringResource(R.string.komga),
                infoText = stringResource(R.string.minimum_komga_version),
                loginEvent = loginEvent,
                login = { username, password, url ->
                    login(komgaState.mergeScreenType, username, password, url)
                },
                logout = { logout(komgaState.mergeScreenType) },
            ),
            mergeGroup(
                mergeState = suwayomiState,
                sourceName = stringResource(R.string.suwayomi),
                infoText = stringResource(R.string.minimum_suwayomi_version),
                loginEvent = loginEvent,
                login = { username, password, url ->
                    login(suwayomiState.mergeScreenType, username, password, url)
                },
                logout = { logout(suwayomiState.mergeScreenType) },
            ),
        )
    }

    @Composable
    private fun mergeGroup(
        mergeState: MergeScreenState,
        sourceName: String,
        infoText: String,
        logout: () -> Unit,
        login: (String, String, String) -> Unit,
        loginEvent: SharedFlow<MergeLoginEvent>,
    ): Preference.PreferenceGroup {
        var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
        var showLoginDialog by rememberSaveable { mutableStateOf(false) }

        if (showLoginDialog) {
            val suwayomi = stringResource(R.string.suwayomi)
            LoginDialog(
                sourceName,
                showUrlField = true,
                showCredentialsField = {
                    if (sourceName == suwayomi) {
                        preferences.suwayomiLoginMode().get() != LoginMode.None
                    } else true
                },
                onDismiss = { showLoginDialog = false },
                onConfirm = login,
                loginEvent = loginEvent,
            )
        }

        if (showLogoutDialog) {
            LogoutDialog(sourceName, onDismiss = { showLogoutDialog = false }, onConfirm = logout)
        }

        val loginText =
            when (mergeState.isLoggedIn) {
                true -> stringResource(R.string.sign_out)
                false -> stringResource(R.string.sign_in)
            }

        return Preference.PreferenceGroup(
            title = sourceName,
            preferenceItems =
                listOfNotNull(
                        Preference.PreferenceItem.SitePreference(
                            title = loginText,
                            subtitle = mergeState.currentUrl.ifBlank { null },
                            isLoggedIn = mergeState.isLoggedIn,
                            login = { showLoginDialog = true },
                            logout = { showLogoutDialog = true },
                        ),
                        if (sourceName == stringResource(R.string.suwayomi)) {
                            Preference.PreferenceItem.ListPreference(
                                pref = preferences.suwayomiLoginMode(),
                                title = stringResource(R.string.suwayomi_login_mode),
                                entries =
                                    LoginMode.entries
                                        .associateWith { stringResource(it.titleResId) }
                                        .toImmutableMap(),
                            )
                        } else null,
                        Preference.PreferenceItem.InfoPreference(title = infoText),
                    )
                    .toPersistentList(),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(title = stringResource(R.string.komga)),
                SearchTerm(title = stringResource(R.string.suwayomi)),
            )
        }
    }
}
