package eu.kanade.tachiyomi.ui.setting

import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.widget.preference.IntListMatPreference
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.android.synthetic.main.main_activity.*
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsGeneralController : SettingsController() {

    private val isUpdaterEnabled = BuildConfig.INCLUDE_UPDATER

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_general

        listPreference(activity) {
            key = Keys.lang
            titleRes = R.string.pref_language
            entryValues = listOf("", "ar", "bg", "bn", "ca", "cs", "de", "el", "en-US", "en-GB",
            "es", "fr", "hi", "hu", "in", "it", "ja", "ko", "lv", "ms", "nb-rNO", "nl", "pl", "pt",
            "pt-BR", "ro", "ru", "sc", "sr", "sv", "th", "tl", "tr", "uk", "vi", "zh-rCN")
            entries = entryValues.map { value ->
                val locale = LocaleHelper.getLocaleFromString(value.toString())
                locale?.getDisplayName(locale)?.capitalize() ?:
                        context.getString(R.string.system_default)
            }
            defaultValue = ""
            summary = "%s"

            onChange { newValue ->
                val activity = activity ?: return@onChange false
                val app = activity.application
                LocaleHelper.changeLocale(newValue.toString())
                LocaleHelper.updateConfiguration(app, app.resources.configuration)
                activity.recreate()
                true
            }
        }

        intListPreference(activity) {
            key = Keys.theme
            titleRes = R.string.pref_theme
            entriesRes = arrayOf(R.string.light_theme, R.string.white_theme, R.string.dark_theme,
                R.string.amoled_theme, R.string.darkblue_theme,
                R.string.system_theme, R.string.sysyem_white_theme, R.string.system_amoled_theme, R.string
                    .system_darkblue_theme)
            entryValues = listOf(1, 8, 2, 3, 4, 5, 9, 6, 7)
            defaultValue = 5

            onChange {
                activity?.recreate()
                true
            }
        }

        listPreference(activity) {
            key= Keys.dateFormat
            titleRes = R.string.pref_date_format
            entryValues = listOf("", "MM/dd/yy", "dd/MM/yy", "yyyy-MM-dd")
            entries = entryValues.map { value ->
                if (value == "") {
                    context.getString(R.string.system_default)
                } else {
                    value
                }
            }
            defaultValue = ""
            summary = "%s"
        }

        intListPreference(activity) {
            key = Keys.startScreen
            titleRes = R.string.pref_start_screen
            entriesRes = arrayOf(R.string.label_library, R.string.label_recent_manga,
                R.string.label_recent_updates)
            entryRange = 1..3
            defaultValue = 1
        }
        switchPreference {
            key = Keys.automaticUpdates
            titleRes = R.string.pref_enable_automatic_updates
            summaryRes = R.string.pref_enable_automatic_updates_summary
            defaultValue = true

            if (isUpdaterEnabled) {
                onChange { newValue ->
                    val checked = newValue as Boolean
                    if (checked) {
                        UpdaterJob.setupTask()
                    } else {
                        UpdaterJob.cancelTask()
                    }
                    true
                }
            } else {
                isVisible = false
            }
        }

        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            var preference:IntListMatPreference? = null
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
                        else -> resources?.getQuantityString(R.plurals.lock_after_mins, it.toInt(),
                            it)
                    }
                }
                entryValues = values
                defaultValue = 0
            }
        }
    }
}
