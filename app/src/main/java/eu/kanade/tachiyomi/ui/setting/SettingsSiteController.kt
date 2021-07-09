package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.follows.StatusSyncJob
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.v5.job.V5MigrationJob
import eu.kanade.tachiyomi.widget.preference.MangadexLoginDialog
import eu.kanade.tachiyomi.widget.preference.MangadexLogoutDialog
import eu.kanade.tachiyomi.widget.preference.SiteLoginPreference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsSiteController :
    SettingsController(),
    MangadexLoginDialog.Listener,
    MangadexLogoutDialog.Listener {

    private val mdex by lazy { Injekt.get<SourceManager>().getMangadex() }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
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
            entriesRes = arrayOf(
                R.string.content_rating_safe,
                R.string.content_rating_suggestive,
                R.string.content_rating_erotica,
                R.string.content_rating_pornographic,
            )
            entryValues = listOf(
                "safe",
                "suggestive",
                "erotica",
                "pornographic"
            )

            defValue = setOf("safe", "suggestive")

            defaultValue = listOf("safe", "suggestive")
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
                MaterialDialog(activity!!).show {
                    listItemsMultiChoice(
                        items = context.resources.getStringArray(R.array.follows_options).toList()
                            .let { it.subList(1, it.size) },
                        initialSelection = intArrayOf(0, 5)
                    ) { _, indices, _ ->
                        preferences.mangadexSyncToLibraryIndexes()
                            .set(indices.map { (it + 1).toString() }.toSet())
                        StatusSyncJob.doWorkNow(context, StatusSyncJob.entireFollowsFromDex)
                    }
                    positiveButton(android.R.string.ok)
                    negativeButton(android.R.string.cancel)
                }
            }
        }

        preference {
            titleRes = R.string.push_favorites_to_mangadex
            summaryRes = R.string.push_favorites_to_mangadex_summary

            onClick {
                StatusSyncJob.doWorkNow(context, StatusSyncJob.entireLibraryToDex)
            }
        }

        if (BuildConfig.DEBUG) {
            preference {
                title = "Unfollow all library manga"
                onClick {
                    launchIO {
                        val db = Injekt.get<DatabaseHelper>()
                        val followsHandler = Injekt.get<FollowsHandler>()
                        val trackManager: TrackManager = Injekt.get()
                        db.getLibraryMangaList().executeAsBlocking().forEach {
                            followsHandler.updateFollowStatus(
                                MdUtil.getMangaId(it.url),
                                FollowStatus.UNFOLLOWED
                            )
                            db.getMDList(it).executeOnIO()?.let { _ ->
                                db.deleteTrackForManga(it, trackManager.mdList)
                                    .executeAsBlocking()
                            }
                        }
                    }
                }
            }
        }

        switchPreference {
            key = PreferenceKeys.addToLibraryAsPlannedToRead
            titleRes = R.string.add_favorites_as_planned_to_read
            summaryRes = R.string.add_favorites_as_planned_to_read_summary
            defaultValue = false
        }

        preference {
            titleRes = R.string.v5_migration_service
            summary = context.resources.getString(R.string.v5_migration_desc)
            onClick {
                MaterialDialog(activity!!).show {
                    title(text = "This will start legacy id migration (Note: This uses data)")
                    positiveButton(android.R.string.ok) {
                        V5MigrationJob.doWorkNow()
                    }
                }
            }
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

            val options = MdLang.values().map { Pair(it.lang, it.prettyPrint) }
            val initialLangs = preferences!!.langsToShow().get().split(",")
                .map { lang -> options.indexOfFirst { it.first == lang } }.toIntArray()

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
