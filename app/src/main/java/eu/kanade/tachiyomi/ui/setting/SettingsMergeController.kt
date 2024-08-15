package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.merged.komga.Komga
import eu.kanade.tachiyomi.widget.preference.KomgaLoginDialog
import eu.kanade.tachiyomi.widget.preference.KomgaLoginPreference
import eu.kanade.tachiyomi.widget.preference.KomgaLogoutDialog
import org.nekomanga.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsMergeController :
    SettingsController(), KomgaLoginDialog.Listener, KomgaLogoutDialog.Listener {
    private val komga by lazy { Injekt.get<SourceManager>().komga }

    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.merge_source_settings

            preferenceCategory {
                title = Komga.name
                this.summaryRes = R.string.minimum_komga_version

                val sourcePreference =
                    KomgaLoginPreference(context, komga).apply {
                        title = "${komga.name} Login"

                        this.komgaUrl = preferences.sourceUrl(komga).get()

                        key = getSourceKey(source.id)
                        setOnLoginClickListener {
                            if (this.komgaUrl.isNotBlank()) {
                                val dialog = KomgaLogoutDialog(komga)
                                dialog.targetController = this@SettingsMergeController
                                dialog.showDialog(router)
                            } else {
                                val dialog = KomgaLoginDialog(komga)
                                dialog.targetController = this@SettingsMergeController
                                dialog.showDialog(router)
                            }
                        }
                        this.isIconSpaceReserved = false
                    }

                addPreference(sourcePreference)
            }
        }

    override fun siteLoginDialogClosed(source: Source, username: String) {
        val pref = findPreference(getSourceKey(source.id)) as? KomgaLoginPreference
        pref?.komgaUrl = username
        pref?.notifyChanged()
    }

    override fun siteLogoutDialogClosed(source: Source) {
        val pref = findPreference(getSourceKey(source.id)) as? KomgaLoginPreference
        pref?.komgaUrl = ""
        pref?.notifyChanged()
    }

    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }
}
