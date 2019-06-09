package eu.kanade.tachiyomi.ui.setting

import android.support.v7.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.widget.preference.LoginCheckBoxPreference
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsSiteController : SettingsController(), SourceLoginDialog.Listener {

    private val sources by lazy { Injekt.get<SourceManager>().getSources() as List<HttpSource> }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_site_specific_settings

        val sourcePreference = LoginCheckBoxPreference(context, sources[0]).apply {
            title = "MangaDex Login"
            key = getSourceKey(source.id)
            setOnLoginClickListener {
                val dialog = SourceLoginDialog(source)
                dialog.targetController = this@SettingsSiteController
                dialog.showDialog(router)
            }
        }

        preferenceScreen.addPreference(sourcePreference)


        listPreference {
            key = PreferenceKeys.showR18
            titleRes = R.string.pref_show_r18_title
            entriesRes = arrayOf(R.string.pref_show_r18_no, R.string.pref_show_r18_all, R.string.pref_show_r18_show)
            entryValues = arrayOf("0", "1", "2")
            summary = "%s"

        }

        listPreference {
            key = PreferenceKeys.imageServer
            titleRes = R.string.pref_image_server
            entries = arrayOf("Auto", "North America", "North America 2", "Europe", "Europe 2", "Rest of the World")
            entryValues = arrayOf("0", "na", "na2", "eu", "eu2", "row")
            summary = "%s"

        }
    }

    override fun loginDialogClosed(source: Source) {
        val pref = findPreference(getSourceKey(source.id)) as? LoginCheckBoxPreference
        pref?.notifyChanged()
    }
    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }

    inline fun PreferenceScreen.mangaDexLogin(
            source: Source,
            block: (@DSL LoginPreference).() -> Unit
    ): LoginPreference {
        return initThenAdd(LoginPreference(context).apply {
            key = "source_${source.id}"
            title = "Login"
        }, block)
    }
}