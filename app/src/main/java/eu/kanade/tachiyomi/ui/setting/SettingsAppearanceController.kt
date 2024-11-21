package eu.kanade.tachiyomi.ui.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getPrefTheme
import kotlin.math.max
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.R
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsAppearanceController : SettingsController() {

    private var lastThemeXLight: Int? = null
    private var lastThemeXDark: Int? = null
    private var themePreference: ThemePreference? = null

    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()

    @SuppressLint("NotifyDataSetChanged")
    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.appearance

            preferenceCategory {
                titleRes = R.string.app_theme

                themePreference = themePreference {
                    key = "theme_preference"
                    titleRes = R.string.app_theme
                    lastScrollPostionLight = lastThemeXLight
                    lastScrollPostionDark = lastThemeXDark
                    summary = context.getString(context.getPrefTheme(preferences).nameRes())
                    activity = this@SettingsAppearanceController.activity
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
                            themePreference?.fastAdapterLight?.notifyDataSetChanged()
                            themePreference?.fastAdapterDark?.notifyDataSetChanged()
                        }
                        true
                    }
                    preferences
                        .nightMode()
                        .changes()
                        .onEach { mode ->
                            isChecked = mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                        .launchIn(viewScope)
                }
            }

            preferenceCategory {
                titleRes = R.string.details_page

                switchPreference {
                    key = mangaDetailsPreferences.forcePortrait().key()
                    titleRes = R.string.force_portrait_details
                    summaryRes = R.string.force_portrait_details_description
                    defaultValue = false
                }

                switchPreference {
                    key = mangaDetailsPreferences.autoThemeByCover().key()
                    titleRes = R.string.theme_buttons_based_on_cover
                    defaultValue = true
                }

                switchPreference {
                    key = mangaDetailsPreferences.hideButtonText().key()
                    titleRes = R.string.hide_button_text
                    defaultValue = false
                }

                switchPreference {
                    key = mangaDetailsPreferences.extraLargeBackdrop().key()
                    titleRes = R.string.extra_large_backdrop
                    defaultValue = false
                }

                switchPreference {
                    key = mangaDetailsPreferences.wrapAltTitles().key()
                    titleRes = R.string.wrap_alt_titles
                    defaultValue = false
                }
            }

            preferenceCategory {
                titleRes = R.string.navigation

                /*switchPreference {
                    key = Keys.hideBottomNavOnScroll
                    titleRes = R.string.hide_bottom_nav
                    summaryRes = R.string.hides_on_scroll
                    defaultValue = true
                }*/

                intListPreference(activity) {
                    key = Keys.sideNavIconAlignment
                    titleRes = R.string.side_nav_icon_alignment
                    entriesRes = arrayOf(R.string.top, R.string.center, R.string.bottom)
                    entryRange = 0..2
                    defaultValue = 1
                    isVisible =
                        max(
                            context.resources.displayMetrics.widthPixels,
                            context.resources.displayMetrics.heightPixels,
                        ) >= 720.dpToPx
                }

                intListPreference(activity) {
                    key = Keys.sideNavMode
                    titleRes = R.string.use_side_navigation
                    val values = SideNavMode.values()
                    entriesRes = values.map { it.stringRes }.toTypedArray()
                    entryValues = values.map { it.prefValue }
                    defaultValue = SideNavMode.DEFAULT.prefValue

                    onChange {
                        activity?.recreate()
                        true
                    }
                }

                infoPreference(R.string.by_default_side_nav_info)
            }
        }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        themePreference = null
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        outState.putInt(::lastThemeXLight.name, themePreference?.lastScrollPostionLight ?: 0)
        outState.putInt(::lastThemeXDark.name, themePreference?.lastScrollPostionDark ?: 0)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        lastThemeXLight = savedViewState.getInt(::lastThemeXLight.name)
        lastThemeXDark = savedViewState.getInt(::lastThemeXDark.name)
        themePreference?.lastScrollPostionLight = lastThemeXLight
        themePreference?.lastScrollPostionDark = lastThemeXDark
    }
}
