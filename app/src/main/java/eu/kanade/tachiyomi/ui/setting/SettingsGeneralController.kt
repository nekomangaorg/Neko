package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.widget.preference.IntListMatPreference

class SettingsGeneralController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_general

        intListPreference(activity) {
            key = Keys.theme
            titleRes = R.string.pref_theme
            entriesRes = arrayOf(
                R.string.light_theme, R.string.dark_theme,
                R.string.amoled_theme
            )
            entryValues = listOf(1, 2, 3)
            defaultValue = 1

            onChange {
                activity?.recreate()
                true
            }
        }
        intListPreference(activity) {
            key = Keys.startScreen
            titleRes = R.string.pref_start_screen
            entriesRes = arrayOf(
                R.string.label_library, R.string.label_recent_manga,
                R.string.label_recent_updates
            )
            entryValues = listOf(1, 2, 3)
            defaultValue = 1
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            preference {
                titleRes = R.string.pref_manage_notifications
                onClick {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    startActivity(intent)
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_security

            if (BiometricManager.from(context)
                    .canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            ) {
                switchPreference {
                    key = Keys.automaticUpdates
                    titleRes = R.string.pref_enable_automatic_updates
                    summaryRes = R.string.pref_enable_automatic_updates_summary
                    defaultValue = true

                    onChange { newValue ->
                        val checked = newValue as Boolean
                        if (checked) {
                            UpdaterJob.setupTask()
                        } else {
                            UpdaterJob.cancelTask()
                        }
                        true
                    }
                }

                var preference: IntListMatPreference? = null
                switchPreference {
                    key = Keys.useBiometrics
                    titleRes = R.string.lock_with_biometrics
                    defaultValue = false

                    onChange {
                        preference?.isVisible = it as Boolean
                        true
                    }
                }
                preference = intListPreference(activity) {
                    key = Keys.lockAfter
                    titleRes = R.string.lock_when_idle
                    isVisible = preferences.useBiometrics().getOrDefault()
                    val values = listOf(0, 2, 5, 10, 20, 30, 60, 90, 120, -1)
                    entries = values.mapNotNull {
                        when (it) {
                            0 -> context.getString(R.string.lock_always)
                            -1 -> context.getString(R.string.lock_never)
                            else -> resources?.getQuantityString(
                                R.plurals.lock_after_mins, it.toInt(),
                                it
                            )
                        }
                    }
                    entryValues = values
                    defaultValue = 0
                }
            }
        }
    }
}
