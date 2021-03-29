package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.similar.SimilarUpdateJob
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.preference.MangadexLoginDialog
import eu.kanade.tachiyomi.widget.preference.MangadexLogoutDialog
import eu.kanade.tachiyomi.widget.preference.SiteLoginPreference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsSiteController :
    SettingsController(),
    MangadexLoginDialog.Listener,
    MangadexLogoutDialog.Listener {

    private val mdex by lazy { Injekt.get<SourceManager>().getMangadex() as HttpSource }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.site_specific_settings

        val sourcePreference = SiteLoginPreference(context, mdex).apply {
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

        preference {
            titleRes = R.string.show_languages
            onClick {
                val ctrl = ChooseLanguagesDialog(preferences)
                ctrl.targetController = this@SettingsSiteController
                ctrl.showDialog(router)
            }
        }

        switchPreference {
            key = PreferenceKeys.useCacheSource
            titleRes = R.string.use_cache_source
            defaultValue = false
            onClick {
                if (isChecked) {
                    MaterialDialog(activity!!).show {
                        message(R.string.use_cache_source_dialog)
                        positiveButton(android.R.string.ok) {
                            SimilarUpdateJob.doWorkNow(true)
                        }
                    }
                }
            }
        }

        listPreference(activity) {
            key = PreferenceKeys.showR18
            titleRes = R.string.show_r18_title
            entriesRes = arrayOf(
                R.string.show_r18_no,
                R.string.show_r18_all,
                R.string.show_r18_show
            )
            entryValues = listOf("0", "1", "2")
            summary = "%s"
        }

        switchPreference {
            key = PreferenceKeys.showR18Filter
            titleRes = R.string.show_r18_filter_in_search
            defaultValue = true
        }

        listPreference(activity) {
            key = PreferenceKeys.imageServer
            titleRes = R.string.image_server
            entries = MangaDex.SERVER_PREF_ENTRIES
            entryValues = MangaDex.SERVER_PREF_ENTRY_VALUES
            summary = "%s"
        }

        switchPreference {
            key = PreferenceKeys.dataSaver
            titleRes = R.string.data_saver
            defaultValue = false
        }

        switchPreference {
            key = PreferenceKeys.lowQualityCovers
            titleRes = R.string.low_quality_covers
            defaultValue = false
        }

        switchPreference {
            key = PreferenceKeys.forceLatestCovers
            titleRes = R.string.use_latest_cover
            summaryRes = R.string.use_latest_cover_summary
            defaultValue = false
        }

        preference {
            titleRes = R.string.sync_follows_to_library
            summaryRes = R.string.sync_follows_to_library_summary

            onClick {
                MaterialDialog(activity!!).show {
                    checkBoxPrompt(text = "Sync planned to read also?", onToggle = null)
                    positiveButton(android.R.string.ok) { dialog ->
                        val type = when {
                            dialog.isCheckPromptChecked() -> LibraryUpdateService.Target.SYNC_FOLLOWS_PLUS
                            else -> LibraryUpdateService.Target.SYNC_FOLLOWS
                        }
                        LibraryUpdateService.start(
                            context,
                            target = type
                        )
                    }
                }
            }
        }


        preference {
            titleRes = R.string.push_favorites_to_mangadex
            summaryRes = R.string.push_favorites_to_mangadex_summary

            onClick {
                LibraryUpdateService.start(
                    context,
                    target = LibraryUpdateService.Target.PUSH_FAVORITES
                )
            }
        }

        switchPreference {
            key = PreferenceKeys.markChaptersFromMDList
            titleRes = R.string.mark_mdlist_chapters_read
            summaryRes = R.string.mark_mdlist_chapters_read_summary
            defaultValue = false
        }

        switchPreference {
            key = PreferenceKeys.addToLibraryAsPlannedToRead
            titleRes = R.string.add_favorites_as_planned_to_read
            summaryRes = R.string.add_favorites_as_planned_to_read_summary
            defaultValue = false
        }
    }

    override fun siteLoginDialogClosed(source: Source) {
        val pref = findPreference(getSourceKey(source.id)) as? SiteLoginPreference
        pref?.notifyChanged()
    }

    override fun siteLogoutDialogClosed(source: Source) {
        val pref = findPreference(getSourceKey(source.id)) as? SiteLoginPreference
        pref?.notifyChanged()
    }

    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }

    class ChooseLanguagesDialog() : DialogController() {

        constructor(preferences: PreferencesHelper) : this() {
            this.preferences = preferences
        }

        var preferences: PreferencesHelper? = null

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!

            val options = MdLang.values().map { Pair(it.dexLang, it.name) }
            val initialLangs = preferences!!.langsToShow().get().split(",")
                .map { lang -> options.indexOfFirst { it.first.equals(lang) } }.toIntArray()

            return MaterialDialog(activity)
                .title(R.string.show_languages)
                .listItemsMultiChoice(
                    items = options.map { it.second },
                    initialSelection = initialLangs
                ) { _, selections, _ ->
                    val selected = selections.map { options[it].first }
                    preferences!!.langsToShow().set(selected.joinToString(","))
                }
                .positiveButton(android.R.string.ok) {
                }
                .negativeButton(android.R.string.cancel)
        }
    }
}
