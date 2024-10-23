package eu.kanade.tachiyomi.ui.setting

import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.isAuthenticationSupported
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import uy.kohesive.injekt.injectLazy

class SettingsSecurityController : SettingsController() {

    val securityPreferences: SecurityPreferences by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.security

            if (context.isAuthenticationSupported()) {
                switchPreference {
                    key = securityPreferences.useBiometrics().key()
                    titleRes = R.string.lock_with_biometrics
                    defaultValue = false

                    requireAuthentication(
                        activity as? FragmentActivity,
                        context.getString(R.string.lock_with_biometrics),
                        confirmationRequired = false,
                    )
                }
                intListPreference(activity) {
                    securityPreferences
                        .useBiometrics()
                        .changes()
                        .onEach { isVisible = it }
                        .launchIn(viewScope)

                    key = securityPreferences.lockAfter().key()
                    titleRes = R.string.lock_when_idle
                    val values = listOf(0, 2, 5, 10, 20, 30, 60, 90, 120, -1)
                    entries =
                        values.mapNotNull {
                            when (it) {
                                0 -> context.getString(R.string.always)
                                -1 -> context.getString(R.string.never)
                                else ->
                                    resources?.getQuantityString(R.plurals.after_minutes, it, it)
                            }
                        }
                    entryValues = values
                    defaultValue = 0
                }
            }

            switchPreference {
                key = securityPreferences.hideNotificationContent().key()
                titleRes = R.string.hide_notification_content
                defaultValue = false
            }

            listPreference(activity) {
                bindTo(securityPreferences.secureScreen())
                titleRes = R.string.secure_screen
                entriesRes =
                    SecurityPreferences.SecureScreenMode.values()
                        .map { it.titleResId }
                        .toTypedArray()
                entryValues = SecurityPreferences.SecureScreenMode.values().map { it.name }

                onChange {
                    it as String
                    SecureActivityDelegate.setSecure(activity)
                    true
                }
            }

            infoPreference(R.string.secure_screen_summary)
        }
}
