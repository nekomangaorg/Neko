package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.util.getResourceColor

class SettingsMainController : SettingsController() {
    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.label_settings

        val tintColor = context.getResourceColor(R.attr.colorAccent)
        val size = 18

        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon2.cmd_tune_vertical)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.pref_category_general
            onClick { navigateTo(SettingsGeneralController()) }
        }
        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon.cmd_google_chrome)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.pref_category_site_specific
            onClick { navigateTo(SettingsSiteController()) }
        }
        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon.cmd_book_open_page_variant)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.pref_category_reader
            onClick { navigateTo(SettingsReaderController()) }
        }
        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon.cmd_download)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.pref_category_downloads
            onClick { navigateTo(SettingsDownloadController()) }
        }
        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon2.cmd_sync)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.pref_category_tracking
            onClick { navigateTo(SettingsTrackingController()) }
        }
        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon.cmd_cloud_upload)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.backup
            onClick { navigateTo(SettingsBackupController()) }
        }
        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon.cmd_code_tags)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.pref_category_advanced
            onClick { navigateTo(SettingsAdvancedController()) }
        }
        preference {
            iconDrawable =
                    IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon2.cmd_help_circle)
                            .colorInt(tintColor)
                            .sizeDp(size)

            titleRes = R.string.pref_category_about
            onClick { navigateTo(SettingsAboutController()) }
        }
    }

    private fun navigateTo(controller: SettingsController) {
        router.pushController(controller.withFadeTransaction())
    }
}