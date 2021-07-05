package eu.kanade.tachiyomi.ui.setting

import androidx.biometric.BiometricManager
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.widget.preference.IntListMatPreference

class SettingsSecurityController : SettingsController() {
    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.security

        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            var preference: IntListMatPreference? = null
            switchPreference {
                key = PreferenceKeys.useBiometrics
                titleRes = R.string.lock_with_biometrics
                defaultValue = false

                onChange {
                    preference?.isVisible = it as Boolean
                    true
                }
            }
            preference = intListPreference(activity) {
                key = PreferenceKeys.lockAfter
                titleRes = R.string.lock_when_idle
                isVisible = preferences.useBiometrics().getOrDefault()
                val values = listOf(0, 2, 5, 10, 20, 30, 60, 90, 120, -1)
                entries = values.mapNotNull {
                    when (it) {
                        0 -> context.getString(R.string.always)
                        -1 -> context.getString(R.string.never)
                        else -> resources?.getQuantityString(
                            R.plurals.after_minutes,
                            it,
                            it
                        )
                    }
                }
                entryValues = values
                defaultValue = 0
            }
        }

        switchPreference {
            key = PreferenceKeys.secureScreen
            titleRes = R.string.secure_screen
            summaryRes = R.string.hide_app_block_screenshots
            defaultValue = false

            onChange {
                it as Boolean
                SecureActivityDelegate.setSecure(activity, it)
                true
            }
        }
        switchPreference {
            key = PreferenceKeys.hideNotificationContent
            titleRes = R.string.hide_notification_content
            defaultValue = false
        }
    }
}
