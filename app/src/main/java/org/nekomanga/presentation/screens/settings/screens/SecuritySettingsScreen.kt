package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.isAuthenticationSupported
import eu.kanade.tachiyomi.util.system.getActivity
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class SecuritySettingsScreen(
    val securityPreferences: SecurityPreferences,
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.security

    @Composable
    override fun getPreferences(): PersistentList<Preference> {
        val context = LocalContext.current
        return persistentListOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = securityPreferences.useBiometrics(),
                title = stringResource(R.string.lock_with_biometrics),
                enabled = context.isAuthenticationSupported(),
                onValueChanged = {
                    (context as FragmentActivity).authenticate(
                        title = context.getString(R.string.lock_with_biometrics)
                    )
                    true
                },
            ),
            Preference.PreferenceItem.ListPreference(
                pref = securityPreferences.lockAfter(),
                enabled = securityPreferences.useBiometrics().get(),
                title = stringResource(R.string.lock_when_idle),
                entries =
                    listOf(0, 2, 5, 10, 20, 30, 60, 90, 120, -1)
                        .associate {
                            when (it) {
                                0 -> it to stringResource(R.string.always)
                                -1 -> it to stringResource(R.string.never)
                                else -> it to pluralStringResource(R.plurals.after_minutes, it, it)
                            }
                        }
                        .toImmutableMap(),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = securityPreferences.hideNotificationContent(),
                title = stringResource(R.string.hide_notification_content),
            ),
            Preference.PreferenceItem.ListPreference(
                pref = securityPreferences.secureScreen(),
                title = stringResource(R.string.secure_screen),
                entries =
                    SecurityPreferences.SecureScreenMode.entries
                        .associate { it to stringResource(it.titleResId) }
                        .toImmutableMap(),
                onValueChanged = {
                    val activity = context.getActivity()
                    if (activity != null) {
                        SecureActivityDelegate.setSecure(context.getActivity())
                    }
                    true
                },
            ),
            Preference.PreferenceItem.InfoPreference(
                title = stringResource(R.string.secure_screen_summary)
            ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(stringResource(R.string.lock_with_biometrics)),
                SearchTerm(stringResource(R.string.lock_when_idle)),
                SearchTerm(stringResource(R.string.hide_notification_content)),
                SearchTerm(stringResource(R.string.secure_screen)),
            )
        }
    }
}
