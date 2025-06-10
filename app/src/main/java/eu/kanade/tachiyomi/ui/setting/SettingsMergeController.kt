package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.merged.komga.Komga
import eu.kanade.tachiyomi.source.online.merged.suwayomi.Suwayomi
import eu.kanade.tachiyomi.widget.preference.MergedLoginDialog
import eu.kanade.tachiyomi.widget.preference.MergedLoginPreference
import eu.kanade.tachiyomi.widget.preference.MergedLogoutDialog
import org.nekomanga.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsMergeController :
    AbstractSettingsController(), MergedLoginDialog.Listener, MergedLogoutDialog.Listener {
    private val komga by lazy { Injekt.get<SourceManager>().komga }
    private val suwayomi by lazy { Injekt.get<SourceManager>().suwayomi }

    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.merge_source_settings

            preferenceCategory {
                title = Komga.name
                this.summaryRes = R.string.minimum_komga_version

                val sourcePreference =
                    MergedLoginPreference(context, komga).apply {
                        title = "${komga.name} Login"

                        this.mergeUrl = preferences.sourceUrl(komga).get()

                        key = getSourceKey(source.id)
                        setOnLoginClickListener {
                            if (this.mergeUrl.isNotBlank()) {
                                val dialog = MergedLogoutDialog(komga)
                                dialog.targetController = this@SettingsMergeController
                                dialog.showDialog(router)
                            } else {
                                val dialog = MergedLoginDialog(komga)
                                dialog.targetController = this@SettingsMergeController
                                dialog.showDialog(router)
                            }
                        }
                        this.isIconSpaceReserved = false
                    }

                addPreference(sourcePreference)
            }

            preferenceCategory {
                title = Suwayomi.name
                this.summaryRes = R.string.minimum_suwayomi_version

                val sourcePreference =
                    MergedLoginPreference(context, suwayomi).apply {
                        title = "${suwayomi.name} Login"

                        this.mergeUrl = preferences.sourceUrl(suwayomi).get()

                        key = getSourceKey(source.id)
                        setOnLoginClickListener {
                            if (this.mergeUrl.isNotBlank()) {
                                val dialog = MergedLogoutDialog(suwayomi)
                                dialog.targetController = this@SettingsMergeController
                                dialog.showDialog(router)
                            } else {
                                val dialog = MergedLoginDialog(suwayomi)
                                dialog.targetController = this@SettingsMergeController
                                dialog.showDialog(router)
                            }
                        }
                        this.isIconSpaceReserved = false
                    }

                addPreference(sourcePreference)
            }
        }

    override fun siteLoginDialogClosed(source: Source, url: String) {
        val pref = findPreference(getSourceKey(source.id)) as? MergedLoginPreference
        pref?.mergeUrl = url
        pref?.notifyChanged()
    }

    override fun siteLogoutDialogClosed(source: Source) {
        val pref = findPreference(getSourceKey(source.id)) as? MergedLoginPreference
        pref?.mergeUrl = ""
        pref?.notifyChanged()
    }

    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }
}
