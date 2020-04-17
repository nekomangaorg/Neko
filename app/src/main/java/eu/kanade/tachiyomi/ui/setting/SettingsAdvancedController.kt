package eu.kanade.tachiyomi.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.library.LibraryUpdateService.Target
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsAdvancedController : SettingsController() {

    private val network: NetworkHelper by injectLazy()

    private val chapterCache: ChapterCache by injectLazy()

    private val coverCache: CoverCache by injectLazy()

    private val db: DatabaseHelper by injectLazy()

    @SuppressLint("BatteryLife")
    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.advanced

        preference {
            key = CLEAR_CACHE_KEY
            titleRes = R.string.clear_chapter_cache
            summary = context.getString(R.string.used_, chapterCache.readableSize)

            onClick { clearChapterCache() }
        }
        preference {
            key = CLEAR_CACHE_IMAGES_KEY
            titleRes = R.string.clear_image_cache
            summary = context.getString(R.string.used_, getChaperCacheSize())

            onClick { clearImageCache() }
        }
        preference {
            titleRes = R.string.clear_cookies

            onClick {
                network.cookieManager.removeAll()
                activity?.toast(R.string.cookies_cleared)
            }
        }
        preference {
            titleRes = R.string.clear_database
            summaryRes = R.string.clear_database_summary

            onClick {
                val ctrl = ClearDatabaseDialogController()
                ctrl.targetController = this@SettingsAdvancedController
                ctrl.showDialog(router)
            }
        }

        preference {
            titleRes = R.string.refresh_tracking_metadata
            summaryRes = R.string.updates_tracking_details

            onClick { LibraryUpdateService.start(context, target = Target.TRACKING) }
        }
        preference {
            titleRes = R.string.clean_up_downloaded_chapters

            summaryRes = R.string.delete_unused_chapters

            onClick { cleanupDownloads() }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager?
            if (pm != null) preference {
                titleRes = R.string.disable_battery_optimization
                summaryRes = R.string.disable_if_issues_with_updating

                onClick {
                    val packageName: String = context.packageName
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } else {
                        context.toast(R.string.battery_optimization_disabled)
                    }
                }
            }
        }
    }

    private fun cleanupDownloads() {
        if (job_downloads?.isActive == true) return
        activity?.toast(R.string.starting_cleanup)
        job_downloads = GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val mangaList = db.getMangas().executeAsBlocking()
            val sourceManager: SourceManager = Injekt.get()
            val downloadManager: DownloadManager = Injekt.get()
            var foldersCleared = 0
            for (manga in mangaList) {
                val chapterList = db.getChapters(manga).executeAsBlocking()
                val source = sourceManager.getMangadex()
                foldersCleared += downloadManager.cleanupChapters(chapterList, manga, source)
            }
            launchUI {
                val activity = activity ?: return@launchUI
                val cleanupString =
                    if (foldersCleared == 0) activity.getString(R.string.no_folders_to_cleanup)
                    else resources!!.getQuantityString(
                        R.plurals.cleanup_done,
                        foldersCleared,
                        foldersCleared
                    )
                activity.toast(cleanupString, Toast.LENGTH_LONG)
            }
        }
    }

    private fun getChaperCacheSize(): String {
        val dirCache = GlideApp.getPhotoCacheDir(activity!!)
        val realSize1 = DiskUtil.getDirectorySize(dirCache!!)
        val realSize2 = DiskUtil.getDirectorySize(coverCache.cacheDir)
        return Formatter.formatFileSize(activity!!, realSize1 + realSize2)
    }

    private fun clearImageCache() {
        if (job_covercache?.isActive == true) return
        job_covercache = GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            // Delete all files from the image cache folder
            val files = coverCache.cacheDir.listFiles()
            var deletedFiles = 0
            if (files != null) {
                for (file in files) {
                    if (file.delete()) {
                        deletedFiles++
                    }
                }
            }
            // Clear the glide disk cache for our chapters
            GlideApp.get(activity!!).clearDiskCache()
            // Sync back to the ui thread to display the toast
            launchUI {
                val activity = activity ?: return@launchUI
                GlideApp.get(activity).clearMemory()
                activity?.toast(
                    resources?.getQuantityString(
                        R.plurals.cache_cleared,
                        deletedFiles, deletedFiles
                    ), Toast.LENGTH_LONG
                )
                findPreference(CLEAR_CACHE_IMAGES_KEY)?.summary =
                    resources?.getString(R.string.used_, getChaperCacheSize())
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
            .subscribe({
            }, {
                activity?.toast(R.string.cache_delete_error)
            }, {
                activity?.toast(
                    resources?.getQuantityString(
                        R.plurals.cache_cleared,
                        deletedFiles, deletedFiles
                    )
                )
                findPreference(CLEAR_CACHE_KEY)?.summary =
                    resources?.getString(R.string.used_, chapterCache.readableSize)
            })
    }

    class ClearDatabaseDialogController : DialogController() {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return MaterialDialog(activity!!)
                .message(R.string.clear_database_confirmation)
                    .positiveButton(android.R.string.ok) {
                    (targetController as? SettingsAdvancedController)?.clearDatabase()
                }
                    .negativeButton(android.R.string.cancel)
        }
    }

    private fun clearDatabase() {
        db.deleteMangasNotInLibrary().executeAsBlocking()
        db.deleteHistoryNoLastRead().executeAsBlocking()
        activity?.toast(R.string.clear_database_completed)
    }

    private companion object {
        const val CLEAR_CACHE_KEY = "pref_clear_cache_key"
        const val CLEAR_CACHE_IMAGES_KEY = "pref_clear_cache_images_key"

        private var job_downloads: Job? = null
        private var job_covercache: Job? = null
    }
}
