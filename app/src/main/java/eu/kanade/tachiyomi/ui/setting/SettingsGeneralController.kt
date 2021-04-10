package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import eu.kanade.tachiyomi.util.system.getPrefTheme
import eu.kanade.tachiyomi.widget.preference.IntListMatPreference
import kotlinx.coroutines.flow.launchIn
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsGeneralController : SettingsController() {

    private val isUpdaterEnabled = BuildConfig.INCLUDE_UPDATER

    var lastThemeX: Int? = null
    var themePreference: ThemePreference? = null
    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.general

        intListPreference(activity) {
            key = Keys.startingTab
            titleRes = R.string.starting_screen
            summaryRes = when (preferences.startingTab().get()) {
                -1 -> R.string.library
                -2 -> R.string.recents
                -3 -> R.string.browse
                else -> R.string.last_used_library_recents
            }
            entriesRes = arrayOf(
                R.string.last_used_library_recents,
                R.string.library,
                R.string.recents,
                R.string.browse
            )
            entryValues = (0 downTo -3).toList()
            defaultValue = 0
            customSelectedValue = when (val value = preferences.startingTab().get()) {
                in -3..-1 -> value
                else -> 0
            }

            onChange { newValue ->
                summaryRes = when (newValue) {
                    0, 1 -> R.string.last_used_library_recents
                    -1 -> R.string.library
                    -2 -> R.string.recents
                    -3 -> R.string.browse
                    else -> R.string.last_used_library_recents
                }
                customSelectedValue = when (newValue) {
                    in -3..-1 -> newValue as Int
                    else -> 0
                }
                true
            }
        }

        switchPreference {
            key = Keys.backToStart
            titleRes = R.string.back_to_start
            summaryRes = R.string.pressing_back_to_start
            defaultValue = true
        }

        switchPreference {
            key = Keys.hideBottomNavOnScroll
            titleRes = R.string.hide_bottom_nav
            summaryRes = R.string.hides_on_scroll
            defaultValue = true
        }

        switchPreference {
            key = Keys.automaticUpdates
            titleRes = R.string.check_for_updates
            summaryRes = R.string.auto_check_for_app_versions
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

        preferenceCategory {
            titleRes = R.string.display

            themePreference = themePreference {
                key = "theme_preference"
                titleRes = R.string.app_theme
                lastScrollPostion = lastThemeX
                summaryRes = context.getPrefTheme(preferences).nameRes
                activity = this@SettingsGeneralController.activity
            }

            switchPreference {
                key = "night_mode_switch"
                isPersistent = false
                titleRes = R.string.follow_system_theme
                isChecked =
                    preferences.nightMode().get() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

                onChange {
                    if (it == true) {
                        preferences.nightMode().set(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        activity?.recreate()
                    } else {
                        preferences.nightMode().set(context.appDelegateNightMode())
                        themePreference?.fastAdapter?.notifyDataSetChanged()
                    }
                    true
                }
                preferences.nightMode().asImmediateFlow { mode ->
                    isChecked = mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }.launchIn(viewScope)
            }
        }

        preferenceCategory {
            titleRes = R.string.security

            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
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
                            0 -> context.getString(R.string.always)
                            -1 -> context.getString(R.string.never)
                            else -> resources?.getQuantityString(
                                R.plurals.after_minutes,
                                it.toInt(),
                                it
                            )
                        }
                    }
                    entryValues = values
                    defaultValue = 0
                }
            }

            switchPreference {
                key = Keys.secureScreen
                titleRes = R.string.secure_screen
                summaryRes = R.string.hide_tachi_from_recents
                defaultValue = false

                onChange {
                    it as Boolean
                    SecureActivityDelegate.setSecure(activity, it)
                    true
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.locale

            listPreference(activity) {
                key = Keys.lang
                titleRes = R.string.language
                entryValues = listOf(
                    "", "ar", "bg", "bn", "ca", "cs", "de", "el", "en-US", "en-GB",
                    "es", "fr", "hi", "hu", "in", "it", "ja", "ko", "lv", "ms", "nb-rNO", "nl", "pl", "pt",
                    "pt-BR", "ro", "ru", "sc", "sr", "sv", "th", "tl", "tr", "uk", "vi", "zh-rCN"
                )
                entries = entryValues.map { value ->
                    val locale = LocaleHelper.getLocaleFromString(value.toString())
                    locale?.getDisplayName(locale)?.capitalize()
                        ?: context.getString(R.string.system_default)
                }
                defaultValue = ""

                onChange { newValue ->
                    val activity = activity ?: return@onChange false
                    val app = activity.application
                    LocaleHelper.changeLocale(newValue.toString())
                    LocaleHelper.updateConfiguration(app, app.resources.configuration)
                    activity.recreate()
                    true
                }
            }

            listPreference(activity) {
                key = Keys.dateFormat
                titleRes = R.string.date_format
                entryValues = listOf("", "MM/dd/yy", "dd/MM/yy", "yyyy-MM-dd")
                entries = entryValues.map { value ->
                    if (value == "") {
                        context.getString(R.string.system_default)
                    } else {
                        value
                    }
                }
                defaultValue = ""
            }
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        themePreference = null
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        outState.putInt(::lastThemeX.name, themePreference?.lastScrollPostion ?: 0)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        lastThemeX = savedViewState.getInt(::lastThemeX.name)
        themePreference?.lastScrollPostion = lastThemeX
    }
}
