package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceScreen
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.CreateCustomListDialogBinding
import eu.kanade.tachiyomi.jobs.customlist.CustomListSyncJob
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.jobs.follows.StatusSyncJob
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.openInFirefox
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.preference.MangadexLogoutDialog
import eu.kanade.tachiyomi.widget.preference.SiteLoginPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

            preferences.mangaDexUserName().changes().onEach { userName ->
                this.username = userName
            }.launchIn(viewScope)

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
                    arrayOf(
                        R.string.original_thumb,
                        R.string.medium_thumb,
                        R.string.low_thumb,
                    )
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

        preferenceCategory {
            titleRes = R.string.mdlists_

            switchPreference {
                key = PreferenceKeys.enableDefaultCustomLists
                titleRes = R.string.enable_default_mdlists
                summaryRes = R.string.enable_default_mdlists_summary
                defaultValue = false
            }

            preference {
                preferences.enableDefaultCustomLists().changes().onEach {
                    isVisible = it
                }.launchIn(viewScope)

                titleRes = R.string.default_mdlists_
                onClick {
                    val ctrl = ChooseCustomListDialog(preferences)
                    ctrl.targetController = this@SettingsSiteController
                    ctrl.showDialog(router)
                }
            }

            preference {
                titleRes = R.string.add_mdlist
                onClick {
                    val ctrl = AddCustomListDialog(viewScope)
                    ctrl.targetController = this@SettingsSiteController
                    ctrl.showDialog(router)
                }
            }

            preference {
                titleRes = R.string.delete_mdlist
                onClick {
                    val ctrl = DeleteCustomListDialog(viewScope)
                    ctrl.targetController = this@SettingsSiteController
                    ctrl.showDialog(router)
                }
            }

        }


        preference {
            titleRes = R.string.sync_mdlist_to_library
            summaryRes = R.string.sync_mdlist_to_library_summary

            onClick {

                val trackManager: TrackManager = Injekt.get()
                val options = trackManager.mdList.viewLists()

                activity!!.materialAlertDialog()
                        .setNegativeButton(android.R.string.cancel, null)
                        .setMultiChoiceItems(
                        options.map { it.name }.toTypedArray(),
                        options.map { false }.toBooleanArray(),
                        ) { dialog, position, bool ->
                            val listView = (dialog as AlertDialog).listView
                            listView.setItemChecked(position, bool)
                        }
                        .setPositiveButton(android.R.string.ok) { dialog, t ->
                            val listView = (dialog as AlertDialog).listView
                        val uuidsSelected = mutableListOf<String>()
                            for (i in 0 until listView.count) {
                                if (listView.isItemChecked(i)) {
                                uuidsSelected.add(options[i].id)
                                }
                            }
                        if (uuidsSelected.size > 0) {
                            CustomListSyncJob.fromMangaDex(context, uuidsSelected)
                            }
                        }
                        .show()
                }
            }

            preference {
                titleRes = R.string.push_favorites_to_mangadex
                summaryRes = R.string.push_favorites_to_mangadex_summary

            onClick {

                val trackManager: TrackManager = Injekt.get()
                val options = trackManager.mdList.viewLists()

                activity!!.materialAlertDialog()
                    .setNegativeButton(android.R.string.cancel, null)
                    .setMultiChoiceItems(
                        options.map { it.name }.toTypedArray(),
                        options.map { false }.toBooleanArray(),
                    ) { dialog, position, bool ->
                        val listView = (dialog as AlertDialog).listView
                        listView.setItemChecked(position, bool)
                    }
                    .setPositiveButton(android.R.string.ok) { dialog, t ->
                        val listView = (dialog as AlertDialog).listView
                        val uuidsSelected = mutableListOf<String>()
                        for (i in 0 until listView.count) {
                            if (listView.isItemChecked(i)) {
                                uuidsSelected.add(options[i].id)
            }
                        }
                        if (uuidsSelected.size > 0) {
                            CustomListSyncJob.toMangaDex(context, uuidsSelected, emptyList())
                        }
                    }
                    .show()
            }
            }
        }

    class AddCustomListDialog(bundle: Bundle? = null) : DialogController() {

        var scope: CoroutineScope? = null

        constructor(scope: CoroutineScope) : this() {
            this.scope = scope
        }

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {

            lateinit var binding: CreateCustomListDialogBinding
            val trackManager: TrackManager = Injekt.get()
            val sourceManager: SourceManager = Injekt.get()

            return activity!!.materialAlertDialog().apply {
                setTitle(R.string.add_mdlist)
                binding = CreateCustomListDialogBinding.inflate(activity!!.layoutInflater)
                setView(binding.root)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(R.string.save) { dialog, _ ->
                    val customListName = binding.title.text.toString()
                    val isPublic = binding.makePublicCheckbox.checkedState == STATE_CHECKED
                    if (customListName.isNotEmpty()) {
                        scope?.launchIO {
                            if (!sourceManager.mangaDex.createCustomList(customListName, isPublic)) {
                                launchUI {
                                    activity?.toast(R.string.failed_to_create_list)
                                }
                            } else {
                                trackManager.mdList.populateLists()
                            }
                        }
                        dismissDialog()
                    }

                }
            }.create()
        }
    }

    class DeleteCustomListDialog(bundle: Bundle? = null) : DialogController() {

        var scope: CoroutineScope? = null
        val trackManager: TrackManager = Injekt.get()
        val sourceManager: SourceManager = Injekt.get()

        constructor(scope: CoroutineScope) : this() {
            this.scope = scope
            scope.launchIO {
                trackManager.mdList.populateLists()
            }
        }

        val options = trackManager.mdList.viewLists()

        val allLists = ((options.map { it.name })).toTypedArray()
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {

            return activity!!.materialAlertDialog().apply {
                setTitle(R.string.delete)
                setSingleChoiceItems(allLists, -1) { dialog, position ->
                    val listUUID = options[position].id
                    scope?.launchIO {
                        if (!sourceManager.mangaDex.deleteCustomList(listUUID)) {
                            launchUI {
                                activity?.toast(R.string.failed_to_delete_list)
                            }
                        } else {
                            trackManager.mdList.populateLists()
                        }
                    }
                    dismissDialog()
                }
            }
                .create()
        }
    }

    class ChooseCustomListDialog() : DialogController() {
        constructor(preferences: PreferencesHelper) : this() {
            this.preferences = preferences
        }

        var preferences: PreferencesHelper? = null

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!

            val trackManager: TrackManager = Injekt.get()

            val options = trackManager.mdList.viewLists()

            val initialIds = preferences!!.getAddToLibraryToSpecificCustomList().get()
                .map { id -> options.indexOfFirst { it.id == id } }.toIntArray()

            val allLists = ((options.map { it.name })).toTypedArray()
            val enabledIds =
                (List(options.size) { index -> initialIds.contains(index) }).toBooleanArray()

            return activity.materialAlertDialog()
                .setTitle(R.string.default_mdlists_)
                .setMultiChoiceItems(
                    allLists,
                    enabledIds,
                ) { dialog, position, _ ->
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { dialog, t ->
                    val listView = (dialog as AlertDialog).listView
                    val selectedUUID = mutableSetOf<String>()

                    for (i in 0 until listView.count) {
                        if (listView.isItemChecked(i)) {
                            selectedUUID.add(options[i].id)
                        }
                    }
                    preferences!!.changeAddToLibraryToSpecificCustomList(selectedUUID)
                }
                .create()
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
                .setMultiChoiceItems(
                    allLangs,
                    enabledLangs,
                ) { dialog, position, _ ->
                }
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
