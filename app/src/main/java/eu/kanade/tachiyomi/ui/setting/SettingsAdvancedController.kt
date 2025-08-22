package eu.kanade.tachiyomi.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceScreen
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.image.coil.CoilDiskCache
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.disableItems
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.setCustomTitleAndMessage
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.openInBrowser
import java.io.File
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.constants.Constants.TMP_FILE_SUFFIX
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.logging.TimberKt
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import tachiyomi.core.network.PREF_DOH_360
import tachiyomi.core.network.PREF_DOH_ADGUARD
import tachiyomi.core.network.PREF_DOH_ALIDNS
import tachiyomi.core.network.PREF_DOH_CLOUDFLARE
import tachiyomi.core.network.PREF_DOH_CONTROLD
import tachiyomi.core.network.PREF_DOH_DNSPOD
import tachiyomi.core.network.PREF_DOH_GOOGLE
import tachiyomi.core.network.PREF_DOH_MULLVAD
import tachiyomi.core.network.PREF_DOH_NJALLA
import tachiyomi.core.network.PREF_DOH_QUAD101
import tachiyomi.core.network.PREF_DOH_QUAD9
import tachiyomi.core.network.PREF_DOH_SHECAN
import tachiyomi.core.util.storage.DiskUtil
import tachiyomi.core.util.system.setDefaultSettings
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsAdvancedController : AbstractSettingsController() {

    private val network: NetworkHelper by injectLazy()
    private val networkPreferences: NetworkPreferences by injectLazy()

    private val chapterCache: ChapterCache by injectLazy()

    private val db: DatabaseHelper by injectLazy()

    private val coverCache: CoverCache by injectLazy()

    private val downloadManager: DownloadManager by injectLazy()

    @SuppressLint("BatteryLife")
    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.advanced

            switchPreference {
                key = "acra.enable"
                titleRes = R.string.send_crash_report
                summaryRes = R.string.helps_fix_bugs
                defaultValue = true
                onClick {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isEnabled)
                }
            }

            preference {
                key = "dump_crash_logs"
                titleRes = R.string.dump_crash_logs
                summaryRes = R.string.saves_error_logs

                onClick {
                    (activity as? AppCompatActivity)?.lifecycleScope?.launchIO {
                        CrashLogUtil(context).dumpLogs()
                    }
                }
            }

            switchPreference {
                key = networkPreferences.verboseLogging().key()
                titleRes = R.string.verbose_logging
                summaryRes = R.string.verbose_logging_summary
                defaultValue = BuildConfig.DEBUG
                onChange {
                    activity?.toast(R.string.requires_app_restart)
                    true
                }
            }

            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager?
            if (pm != null) {
                preference {
                    key = "disable_batt_opt"
                    titleRes = R.string.disable_battery_optimization
                    summaryRes = R.string.disable_if_issues_with_updating

                    onClick {
                        val packageName: String = context.packageName
                        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                            val intent =
                                Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = "package:$packageName".toUri()
                                }
                            startActivity(intent)
                        } else {
                            context.toast(R.string.battery_optimization_disabled)
                        }
                    }
                }
            }

            preference {
                key = "pref_dont_kill_my_app"
                title = "Don't kill my app!"
                summaryRes = R.string.about_dont_kill_my_app

                onClick { openInBrowser("https://dontkillmyapp.com/") }
            }

            preferenceCategory {
                titleRes = R.string.data_management

                preference {
                    title = "Total cache usage"

                    val tmpFiles =
                        File(context.cacheDir, "")
                            .listFiles()!!
                            .mapNotNull {
                                if (it.isFile && (it.name.endsWith(TMP_FILE_SUFFIX))) {
                                    DiskUtil.getDirectorySize(it)
                                } else {
                                    null
                                }
                            }
                            .sum()

                    summary =
                        """
                      Parent cache folder: ${DiskUtil.readableDiskSize(context, File(context.cacheDir, ""))}
                      Chapter disk cache: ${DiskUtil.readableDiskSize(context, chapterCache.cacheDir)}
                      Cover cache: ${coverCache.getCoverCacheSize()}
                      Online cover cache: ${coverCache.getOnlineCoverCacheSize()}
                      Image cache: ${DiskUtil.readableDiskSize(context, CoilDiskCache.get(context).size)}
                      Network cache: ${DiskUtil.readableDiskSize(context, network.cacheDir)}
                      Temp file cache: ${DiskUtil.readableDiskSize(context, tmpFiles)}
                    """
                            .trimIndent()
                    onClick {}
                }

                preference {
                    key = CLEAR_CACHE_KEY
                    titleRes = R.string.clear_chapter_cache
                    summary = context.getString(R.string.used_, chapterCache.readableSize)

                    onClick { clearChapterCache() }
                }

                preference {
                    titleRes = R.string.reindex_downloads
                    summaryRes = R.string.reindex_downloads_summary
                    onClick {
                        (activity as? AppCompatActivity)?.lifecycleScope?.launchIO {
                            downloadManager.refreshCache()
                        }
                    }
                }

                preference {
                    key = "clean_cached_covers"
                    titleRes = R.string.clean_up_cached_covers
                    summary =
                        context.getString(
                            R.string.delete_old_covers_in_library_used_,
                            coverCache.getCoverCacheSize(),
                        )

                    onClick {
                        context.toast(R.string.starting_cleanup)
                        (activity as? AppCompatActivity)?.lifecycleScope?.launchIO {
                            coverCache.deleteOldCovers()
                        }
                    }
                }
                preference {
                    key = "clear_cached_not_library"
                    titleRes = R.string.clear_cached_covers_non_library
                    summary =
                        context.getString(
                            R.string.delete_all_covers__not_in_library_used_,
                            coverCache.getOnlineCoverCacheSize(),
                        )

                    onClick {
                        context.toast(R.string.starting_cleanup)
                        (activity as? AppCompatActivity)?.lifecycleScope?.launchIO {
                            coverCache.deleteAllCachedCovers()
                        }
                    }
                }

                preference {
                    key = "clear_temp_cache_files"
                    title = "Clear Temp Cache files"
                    onClick {
                        context.toast(R.string.starting_cleanup)
                        (activity as? AppCompatActivity)?.lifecycleScope?.launchIO {
                            File(context.cacheDir, "").listFiles()!!.forEach {
                                launchIO { it.delete() }
                            }
                        }
                    }
                }

                preference {
                    key = "clean_downloaded_chapters"
                    titleRes = R.string.clean_up_downloaded_chapters

                    summaryRes = R.string.delete_unused_chapters

                    onClick {
                        val ctrl = CleanupDownloadsDialogController()
                        ctrl.targetController = this@SettingsAdvancedController
                        ctrl.showDialog(router)
                    }
                }

                preference {
                    key = "clear_download_queue"
                    titleRes = R.string.clear_download_queue

                    onClick {
                        launchIO {
                            downloadManager.clearQueue()
                            launchUI { activity?.toast(R.string.clear_download_queue_completed) }
                        }
                    }
                }

                preference {
                    key = "pref_clear_webview_data"
                    titleRes = R.string.pref_clear_webview_data

                    onClick { clearWebViewData() }
                }
                preference {
                    key = "clear_database"
                    titleRes = R.string.clear_database
                    summaryRes = R.string.clear_database_summary

                    onClick {
                        val ctrl = ClearDatabaseDialogController()
                        ctrl.targetController = this@SettingsAdvancedController
                        ctrl.showDialog(router)
                    }
                }
            }

            preferenceCategory {
                titleRes = R.string.network
                preference {
                    key = "clear_cookies"
                    titleRes = R.string.clear_cookies

                    onClick {
                        network.cookieManager.removeAll()
                        activity?.toast(R.string.cookies_cleared)
                    }
                }
                intListPreference(activity) {
                    key = networkPreferences.dohProvider().key()
                    titleRes = R.string.doh
                    entriesRes =
                        arrayOf(
                            R.string.disabled,
                            R.string.cloudflare,
                            R.string.google,
                            R.string.ad_guard,
                            R.string.quad9,
                            R.string.aliDNS,
                            R.string.dnsPod,
                            R.string.dns_360,
                            R.string.quad_101,
                            R.string.mullvad,
                            R.string.control_d,
                            R.string.njalla,
                            R.string.shecan,
                        )
                    entryValues =
                        listOf(
                            -1,
                            PREF_DOH_CLOUDFLARE,
                            PREF_DOH_GOOGLE,
                            PREF_DOH_ADGUARD,
                            PREF_DOH_QUAD9,
                            PREF_DOH_ALIDNS,
                            PREF_DOH_DNSPOD,
                            PREF_DOH_360,
                            PREF_DOH_QUAD101,
                            PREF_DOH_MULLVAD,
                            PREF_DOH_CONTROLD,
                            PREF_DOH_NJALLA,
                            PREF_DOH_SHECAN,
                        )

                    defaultValue = -1
                    onChange {
                        activity?.toast(R.string.requires_app_restart)
                        true
                    }
                }
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
                key = "send_firebase_event"
                title = "send a test firebase event"

                onClick {
                    FirebaseAnalytics.getInstance(context)
                        .logEvent("test_event", Bundle().apply { this.putString("test", "test") })
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
                                    it.uuid(),
                                    FollowStatus.UNFOLLOWED,
                                )
                                db.getMDList(it).executeOnIO()?.let { _ ->
                                    db.deleteTrackForManga(it, trackManager.mdList)
                                        .executeAsBlocking()
                                }
                            }
                        }
                    }
                }

                preference {
                    title = "Remove all manga with status on MangaDex"
                    onClick {
                        launchIO {
                            val statusHandler = Injekt.get<StatusHandler>()
                            val followsHandler = Injekt.get<FollowsHandler>()

                            val results = statusHandler.fetchReadingStatusForAllManga()

                            results.entries.forEach { entry ->
                                if (entry.value != null) {
                                    followsHandler.updateFollowStatus(
                                        entry.key,
                                        FollowStatus.UNFOLLOWED,
                                    )
                                }
                            }
                        }
                    }
                }

                preference {
                    title = "Clear all Manga"
                    onClick {
                        launchIO {
                            val db = Injekt.get<DatabaseHelper>()
                            db.deleteAllManga().executeOnIO()
                        }
                    }
                }
                preference {
                    title = "Clear all categories"
                    onClick {
                        launchIO {
                            val db = Injekt.get<DatabaseHelper>()
                            val categories = db.getCategories().executeAsBlocking()
                            db.deleteCategories(categories).executeOnIO()
                        }
                    }
                }
                preference {
                    title = "Clear all trackers"
                    onClick {
                        launchIO {
                            val db = Injekt.get<DatabaseHelper>()
                            db.deleteTracks().executeOnIO()
                        }
                    }
                }
            }
        }

    class CleanupDownloadsDialogController : DialogController() {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return activity!!
                .materialAlertDialog()
                .setTitle(R.string.clean_up_downloaded_chapters)
                .setMultiChoiceItems(
                    R.array.clean_up_downloads,
                    booleanArrayOf(true, true, true),
                ) { dialog, position, _ ->
                    if (position == 0) {
                        val listView = (dialog as AlertDialog).listView
                        listView.setItemChecked(position, true)
                    }
                }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    val listView = (dialog as AlertDialog).listView
                    val deleteRead = listView.isItemChecked(1)
                    val deleteNonFavorite = listView.isItemChecked(2)
                    (targetController as? SettingsAdvancedController)?.cleanupDownloads(
                        deleteRead,
                        deleteNonFavorite,
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply {
                    this.disableItems(
                        arrayOf(activity!!.getString(R.string.clean_orphaned_downloads))
                    )
                }
        }
    }

    private fun cleanupDownloads(removeRead: Boolean, removeNonFavorite: Boolean) {
        if (job?.isActive == true) return

        activity?.toast(R.string.starting_cleanup)
        job =
            GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
                val sourceManager: SourceManager = Injekt.get()
                val downloadProvider = DownloadProvider(activity!!)
                var foldersCleared = 0
                val mangaList = db.getMangaList().executeAsBlocking()
                val source = sourceManager.mangaDex
                val mangaFolders = downloadManager.getMangaFolders()

                for (mangaFolder in mangaFolders) {
                    val manga =
                        mangaList.find { downloadProvider.getMangaDirName(it) == mangaFolder.name }
                    if (manga == null) {
                        // download is orphaned delete it if remove non favorited is enabled
                        if (removeNonFavorite) {
                            foldersCleared += 1 + (mangaFolder.listFiles()?.size ?: 0)
                            mangaFolder.delete()
                        }
                        continue
                    }
                    val chapterList = db.getChapters(manga).executeAsBlocking()
                    foldersCleared +=
                        downloadManager.cleanupChapters(
                            chapterList,
                            manga,
                            removeRead,
                            removeNonFavorite,
                        )
                }
                launchUI {
                    val activity = activity ?: return@launchUI
                    val cleanupString =
                        if (foldersCleared == 0) {
                            activity.getString(R.string.no_folders_to_cleanup)
                        } else {
                            resources!!.getQuantityString(
                                R.plurals.cleanup_done,
                                foldersCleared,
                                foldersCleared,
                            )
                        }
                    activity.toast(cleanupString, Toast.LENGTH_LONG)
                }
            }
    }

    private fun clearChapterCache() {
        if (activity == null) return
        val files = chapterCache.cacheDir.listFiles() ?: return

        var deletedFiles = 0

        Observable.defer { Observable.from(files) }
            .doOnNext { file ->
                if (chapterCache.removeFileFromCache(file.name)) {
                    deletedFiles++
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {},
                { activity?.toast(R.string.cache_delete_error) },
                {
                    activity?.toast(
                        resources?.getQuantityString(
                            R.plurals.cache_cleared,
                            deletedFiles,
                            deletedFiles,
                        )
                    )
                    findPreference(CLEAR_CACHE_KEY)?.summary =
                        resources?.getString(R.string.used_, chapterCache.readableSize)
                },
            )
    }

    class ClearDatabaseDialogController : DialogController() {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val item = arrayOf(activity!!.getString(R.string.clear_db_exclude_read))
            val selected = booleanArrayOf(false)
            return activity!!
                .materialAlertDialog()
                .setCustomTitleAndMessage(
                    R.string.clear_database_confirmation_title,
                    activity!!.getString(R.string.clear_database_confirmation),
                )
                .setMultiChoiceItems(item, selected) { _, which, checked ->
                    selected[which] = checked
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    (targetController as? SettingsAdvancedController)?.clearDatabase(
                        selected.last()
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    private fun clearDatabase(keepRead: Boolean) {
        if (keepRead) {
            db.deleteAllMangaNotInLibraryAndNotRead().executeAsBlocking()
        } else {
            db.deleteAllMangaNotInLibrary().executeAsBlocking()
        }
        db.deleteHistoryNoLastRead().executeAsBlocking()
        activity?.toast(R.string.clear_database_completed)
    }

    private fun clearWebViewData() {
        if (activity == null) return
        try {
            val webview = WebView(activity!!)
            webview.setDefaultSettings()
            webview.clearCache(true)
            webview.clearFormData()
            webview.clearHistory()
            webview.clearSslPreferences()
            WebStorage.getInstance().deleteAllData()
            activity?.applicationInfo?.dataDir?.let { File("$it/app_webview/").deleteRecursively() }
            activity?.toast(R.string.webview_data_deleted)
        } catch (e: Throwable) {
            TimberKt.e(e) { "Error clearing webview data" }
            activity?.toast(R.string.cache_delete_error)
        }
    }

    private companion object {
        const val CLEAR_CACHE_KEY = "pref_clear_cache_key"

        private var job: Job? = null
    }
}
