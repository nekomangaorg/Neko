package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.Mangadex
import eu.kanade.tachiyomi.widget.preference.LoginCheckBoxPreference
import eu.kanade.tachiyomi.widget.preference.MangadexLoginDialog
import eu.kanade.tachiyomi.widget.preference.MangadexLogoutDialog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsSiteController : SettingsController(), MangadexLoginDialog.Listener, MangadexLogoutDialog.Listener {

    private val mdex by lazy { Injekt.get<SourceManager>().getMangadex() as HttpSource }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_site_specific_settings

        val sourcePreference = LoginCheckBoxPreference(context, mdex).apply {
            title = mdex.name + " Login"
            key = getSourceKey(source.id)
            setOnLoginClickListener {
                if (mdex.isLogged()) {
                    val dialog = MangadexLogoutDialog(source)
                    dialog.targetController = this@SettingsSiteController
                    dialog.showDialog(router)
                } else {
                    val dialog = MangadexLoginDialog(source)
                    dialog.targetController = this@SettingsSiteController
                    dialog.showDialog(router)
                }
            }
        }

        preferenceScreen.addPreference(sourcePreference)

        listPreference(activity) {
            key = PreferenceKeys.showR18
            titleRes = R.string.pref_show_r18_title
            entriesRes = arrayOf(R.string.pref_show_r18_no, R.string.pref_show_r18_all, R.string.pref_show_r18_show)
            entryValues = listOf("0", "1", "2")
            summary = "%s"
        }

        listPreference(activity) {
            key = PreferenceKeys.imageServer
            titleRes = R.string.pref_image_server
            entries = Mangadex.SERVER_PREF_ENTRIES
            entryValues = Mangadex.SERVER_PREF_ENTRY_VALUES
            summary = "%s"
        }

        switchPreference {
            key = PreferenceKeys.lowQualityCovers
            titleRes = R.string.pref_low_quality_covers
            defaultValue = false
        }
        switchPreference {
            key = PreferenceKeys.useNonLoggedNetwork
            titleRes = R.string.pref_use_non_logged_in_network
            defaultValue = false
        }

        preference {
            titleRes = R.string.pref_sync_library_follows
            summaryRes = R.string.pref_refresh_library_follows_summary

            onClick { LibraryUpdateService.start(context, target = LibraryUpdateService.Target.SYNC_FOLLOWS) }
        }
    }

    override fun siteLoginDialogClosed(source: Source) {
        val pref = findPreference(getSourceKey(source.id)) as? LoginCheckBoxPreference
        pref?.notifyChanged()
    }

    override fun siteLogoutDialogClosed(source: Source) {
        val pref = findPreference(getSourceKey(source.id)) as? LoginCheckBoxPreference
        pref?.notifyChanged()
    }

    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }
}
