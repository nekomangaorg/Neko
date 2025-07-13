package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.jobs.follows.StatusSyncJob
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.openInFirefox
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.preference.MangadexLogoutDialog
import eu.kanade.tachiyomi.widget.preference.SiteLoginPreference
import kotlinx.coroutines.launch
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsSiteController : SettingsController(), MangadexLogoutDialog.Listener {

    private val mangaDexLoginHelper by lazy { Injekt.get<MangaDexLoginHelper>() }
    private val db by lazy { Injekt.get<DatabaseHelper>() }

    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.site_specific_settings

            val sourcePreference =
                SiteLoginPreference(context, mangaDexLoginHelper).apply {
                    title = "MangaDex Login"
                    key = PreferenceKeys.refreshToken

                    setOnLoginClickListener {
                        when (mangaDexLoginHelper.isLoggedIn()) {
                            true -> {
                                val dialog = MangadexLogoutDialog()
                                dialog.targetController = this@SettingsSiteController
                                dialog.showDialog(router)
                            }
                            false -> {
                                val url =
                                    MdConstants.Login.authUrl(preferences.codeVerifier().get())
                                when (BuildConfig.DEBUG) {
                                    true -> activity?.openInFirefox(url)
                                    false -> activity?.openInBrowser(url)
                                }
                            }
                        }
                    }
                    this.isIconSpaceReserved = false
                }

            addPreference(sourcePreference)

            preference {
                titleRes = R.string.show_languages
                onClick {
                    val ctrl = ChooseLanguagesDialog(preferences)
                    ctrl.targetController = this@SettingsSiteController
                    ctrl.showDialog(router)
                }
            }

            multiSelectListPreferenceMat(activity) {
                key = PreferenceKeys.contentRating
                titleRes = R.string.content_rating_title
                summaryRes = R.string.content_rating_summary
                entriesRes =
                    arrayOf(
                        R.string.content_rating_safe,
                        R.string.content_rating_suggestive,
                        R.string.content_rating_erotica,
                        R.string.content_rating_pornographic,
                    )
                entryValues =
                    listOf(
                        MdConstants.ContentRating.safe,
                        MdConstants.ContentRating.suggestive,
                        MdConstants.ContentRating.erotica,
                        MdConstants.ContentRating.pornographic,
                    )

                defValue =
                    setOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive)

                defaultValue =
                    listOf(MdConstants.ContentRating.safe, MdConstants.ContentRating.suggestive)
            }

            switchPreference {
                key = PreferenceKeys.showContentRatingFilter
                titleRes = R.string.show_content_rating_filter_in_search
                defaultValue = true
            }

            switchPreference {
                key = PreferenceKeys.enablePort443Only
                titleRes = R.string.use_port_443_title
                summaryRes = R.string.use_port_443_summary
                defaultValue = true
            }

            switchPreference {
                key = PreferenceKeys.dataSaver
                titleRes = R.string.data_saver
                summaryRes = R.string.data_saver_summary
                defaultValue = false
            }

            intListPreference(activity) {
                key = PreferenceKeys.thumbnailQuality
                titleRes = R.string.thumbnail_quality
                entriesRes =
                    arrayOf(R.string.original_thumb, R.string.medium_thumb, R.string.low_thumb)
                entryRange = 0..2
                defaultValue = 0
            }

            preference {
                titleRes = R.string.delete_saved_filters
                summaryRes = R.string.delete_saved_filters_description
                onClick {
                    activity!!
                        .materialAlertDialog()
                        .setTitle(R.string.delete_saved_filters)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.delete) { dialog, t ->
                            viewScope.launch { db.deleteAllBrowseFilters().executeAsBlocking() }
                        }
                        .show()
                }
            }

            preference {
                titleRes = R.string.currently_blocked_scanlators
                summaryRes = R.string.currently_blocked_scanlators_description

                onClick {
                    when (preferences.blockedScanlators().get().isEmpty()) {
                        true -> context.toast(R.string.no_blocked_scanlator)
                        false -> {
                            activity!!
                                .materialAlertDialog()
                                .setTitle(R.string.unblock_scanlator)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setMultiChoiceItems(
                                    preferences
                                        .blockedScanlators()
                                        .get()
                                        .toTypedArray()
                                        .sortedArrayDescending(),
                                    preferences
                                        .blockedScanlators()
                                        .get()
                                        .map { false }
                                        .toBooleanArray(),
                                ) { dialog, position, bool ->
                                    val listView = (dialog as AlertDialog).listView
                                    listView.setItemChecked(position, bool)
                                }
                                .setPositiveButton(R.string.remove) { dialog, t ->
                                    val listView = (dialog as AlertDialog).listView
                                    val blockedScanlators =
                                        preferences
                                            .blockedScanlators()
                                            .get()
                                            .toList()
                                            .sortedDescending()
                                    val selectedToRemove = HashSet<String>()
                                    for (i in 0 until listView.count) {
                                        if (listView.isItemChecked(i)) {
                                            selectedToRemove.add(blockedScanlators[i])
                                        }
                                    }
                                    if (selectedToRemove.size > 0) {
                                        val newBlocks =
                                            blockedScanlators
                                                .filter { it !in selectedToRemove }
                                                .toSet()
                                        preferences.blockedScanlators().set(newBlocks)
                                        selectedToRemove.map {
                                            viewScope.launch {
                                                db.deleteScanlator(it).executeOnIO()
                                            }
                                        }
                                    }
                                }
                                .show()
                        }
                    }
                }
            }

            switchPreference {
                key = PreferenceKeys.readingSync
                titleRes = R.string.reading_sync
                summaryRes = R.string.reading_sync_summary
                defaultValue = false
            }

            preference {
                titleRes = R.string.sync_follows_to_library
                summaryRes = R.string.sync_follows_to_library_summary

                onClick {
                    activity!!
                        .materialAlertDialog()
                        .setNegativeButton(android.R.string.cancel, null)
                        .setMultiChoiceItems(
                            context.resources
                                .getStringArray(R.array.follows_options)
                                .drop(1)
                                .toTypedArray(),
                            booleanArrayOf(true, false, false, false, false, true),
                        ) { dialog, position, bool ->
                            val listView = (dialog as AlertDialog).listView
                            listView.setItemChecked(position, bool)
                        }
                        .setPositiveButton(android.R.string.ok) { dialog, t ->
                            val listView = (dialog as AlertDialog).listView
                            val indiciesSelected = mutableListOf<String>()
                            for (i in 0 until listView.count) {
                                if (listView.isItemChecked(i)) {
                                    indiciesSelected.add((i + 1).toString())
                                }
                            }
                            if (indiciesSelected.size > 0) {
                                preferences
                                    .mangadexSyncToLibraryIndexes()
                                    .set(indiciesSelected.toSet())
                                TimberKt.d { "Starting sync job" }
                                StatusSyncJob.startNow(context, StatusSyncJob.entireFollowsFromDex)
                            }
                        }
                        .show()
                }
            }

            preference {
                titleRes = R.string.push_favorites_to_mangadex
                summaryRes = R.string.push_favorites_to_mangadex_summary

                onClick { StatusSyncJob.startNow(context, StatusSyncJob.entireLibraryToDex) }
            }

            intListPreference(activity) {
                key = PreferenceKeys.autoAddToMangadexLibrary
                titleRes = R.string.auto_add_to_mangadex_library
                summaryRes =
                    when (preferences.autoAddToMangadexLibrary().get()) {
                        1 -> R.string.follows_plan_to_read
                        2 -> R.string.follows_on_hold
                        3 -> R.string.follows_reading
                        else -> R.string.disabled
                    }
                entriesRes =
                    arrayOf(
                        R.string.disabled,
                        R.string.follows_plan_to_read,
                        R.string.follows_on_hold,
                        R.string.follows_reading,
                    )
                entryValues = (0..3).toList()
                defaultValue = 0
                customSelectedValue =
                    when (val value = preferences.autoAddToMangadexLibrary().get()) {
                        in 0..3 -> value
                        else -> 0
                    }
                onChange { newValue ->
                    summaryRes =
                        when (preferences.autoAddToMangadexLibrary().get()) {
                            1 -> R.string.follows_plan_to_read
                            2 -> R.string.follows_on_hold
                            3 -> R.string.follows_reading
                            else -> R.string.disabled
                        }
                    customSelectedValue =
                        when (newValue) {
                            in 0..3 -> newValue as Int
                            else -> 0
                        }
                    true
                }
            }
        }

    class ChooseLanguagesDialog() : DialogController() {

        constructor(preferences: PreferencesHelper) : this() {
            this.preferences = preferences
        }

        var preferences: PreferencesHelper? = null

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!

            val options = MdLang.values().map { Pair(it.lang, it.prettyPrint) }

            val initialLangs =
                preferences!!
                    .langsToShow()
                    .get()
                    .split(",")
                    .map { lang -> options.indexOfFirst { it.first == lang } }
                    .toIntArray()

            val allLangs = (options.map { it.second }).toTypedArray()
            val enabledLangs =
                (List(options.size) { index -> initialLangs.contains(index) }).toBooleanArray()

            return activity
                .materialAlertDialog()
                .setTitle(R.string.show_languages)
                .setMultiChoiceItems(allLangs, enabledLangs) { dialog, position, _ -> }
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { dialog, t ->
                    val listView = (dialog as AlertDialog).listView
                    val selected = mutableListOf<String>()
                    for (i in 0 until listView.count) {
                        if (listView.isItemChecked(i)) {
                            selected.add(options[i].first)
                        }
                    }
                    preferences!!.langsToShow().set(selected.joinToString(","))
                }
                .create()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        val pref = findPreference(PreferenceKeys.refreshToken) as? SiteLoginPreference
        pref?.notifyChanged()
    }

    override fun siteLogoutDialogClosed() {
        val pref = findPreference(PreferenceKeys.refreshToken) as? SiteLoginPreference
        pref?.notifyChanged()
    }
}
