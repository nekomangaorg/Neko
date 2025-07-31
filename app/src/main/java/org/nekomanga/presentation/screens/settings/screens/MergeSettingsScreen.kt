package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.setting.MergeLoginEvent
import eu.kanade.tachiyomi.ui.setting.MergeScreenState
import eu.kanade.tachiyomi.ui.setting.MergeScreenType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.presentation.components.dialog.LoginDialog
import org.nekomanga.presentation.components.dialog.LogoutDialog
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class MergeSettingsScreen(
    onNavigationIconClick: () -> Unit,
    val komgaState: MergeScreenState,
    val suwayomiState: MergeScreenState,
    val logout: (MergeScreenType) -> Unit,
    val login: (MergeScreenType, String, String, String) -> Unit,
    val loginEvent: SharedFlow<MergeLoginEvent>,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.merge_source_settings

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        val context = LocalContext.current

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
            LoginDialog(
                sourceName,
                requiresCredential = mergeState.requiresCredentials,
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
                persistentListOf(
                    Preference.PreferenceItem.SitePreference(
                        title = loginText,
                        subtitle = mergeState.currentUrl.ifBlank { null },
                        isLoggedIn = mergeState.isLoggedIn,
                        login = { showLoginDialog = true },
                        logout = { showLogoutDialog = true },
                    ),
                    Preference.PreferenceItem.InfoPreference(title = infoText),
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf()
        }
    }
}
