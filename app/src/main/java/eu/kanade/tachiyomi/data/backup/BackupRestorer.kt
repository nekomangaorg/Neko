package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.notificationManager
import kotlinx.coroutines.Job
import okio.buffer
import okio.gzip
import okio.source
import uy.kohesive.injekt.injectLazy

class BackupRestorer(val context: Context, val job: Job?) {

    lateinit var backupManager: BackupManager
    val restoreHelper = RestoreHelper(context)

    /**
     * The progress of a backup restore
     */
    private var restoreProgress = 0

    /**
     * Amount of manga in file (needed for restore)
     */
    private var restoreAmount = 0

    private var totalAmount = 0

    private var skippedAmount = 0

    private var skippedTitles = emptyList<String>()

    private var categoriesAmount = 0
    private val errors = mutableListOf<String>()
    private var cancelled = 0
    private val trackingErrors = mutableListOf<String>()

    private val db: DatabaseHelper by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    val coverCache: CoverCache by injectLazy()

    suspend fun restoreBackup(uri: Uri) {
        backupManager = BackupManager(context)

        val backupString = context.contentResolver.openInputStream(uri)!!.source().gzip().buffer()
            .use { it.readByteArray() }
        val backup = backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)

        val partitionedList =
            backup.backupManga.partition { backupManager.sourceManager.isMangadex(it.source) }

        val dexManga = partitionedList.first
        skippedTitles = partitionedList.second.map { it.title }
        totalAmount = backup.backupManga.size
        skippedAmount = totalAmount - dexManga.size
        restoreAmount = dexManga.size
        trackingErrors.clear()
        errors.clear()
        cancelled = 0

        if (backup.backupCategories.isNotEmpty()) {
            restoreCategories(backup.backupCategories)
        }

        dexManga.groupBy { MdUtil.getMangaUUID(it.url) }.forEach { (_, mangaList) ->
            restoreManga(mangaList.first().title, mangaList, backup.backupCategories)
        }

        context.notificationManager.cancel(Notifications.ID_RESTORE_PROGRESS)

        cancelled = errors.count { it.contains("cancelled", true) }
        val tmpErrors = errors.filter { !it.contains("cancelled", true) }
        errors.clear()
        errors.addAll(tmpErrors)

        val logFile = restoreHelper.writeErrorLog(errors, skippedAmount, skippedTitles)
        restoreHelper.showResultNotification(
            logFile?.parent,
            logFile?.name,
            categoriesAmount,
            restoreProgress,
            restoreAmount,
            skippedAmount,
            totalAmount,
            cancelled,
            errors,
            trackingErrors,
        )
    }

    private fun restoreCategories(backupCategories: List<BackupCategory>) {
        backupManager.restoreCategories(backupCategories)
        categoriesAmount = backupCategories.size
        restoreAmount += 1
        restoreProgress += 1
        totalAmount += 1
        restoreHelper.showProgressNotification(restoreProgress, totalAmount, "Categories added")
    }

    private fun restoreManga(title: String, backupMangaList: List<BackupManga>, backupCategories: List<BackupCategory>) {
        try {
            if (job?.isCancelled == false) {
                restoreHelper.showProgressNotification(
                    restoreProgress,
                    totalAmount,
                    title,
                )
                restoreProgress += 1
            } else {
                throw java.lang.Exception("Job was cancelled")
            }

            val backupManga = if (backupMangaList.size == 1) {
                backupMangaList.first()
            } else {
                val tempCategories = backupMangaList.map { it.categories }.distinct().flatten()
                val tempChapters = backupMangaList.map { it.chapters }.distinct().flatten()
                val tempHistory = backupMangaList.map { it.history }.distinct().flatten()
                val tempTracks = backupMangaList.map { it.tracking }.distinct().flatten()

                backupMangaList.first().copy(
                    categories = tempCategories,
                    chapters = tempChapters,
                    history = tempHistory,
                    tracking = tempTracks,
                )
            }
            // always make it EN source
            backupManga.source = SourceManager.getId(MdLang.ENGLISH.lang)

            val manga = backupManga.getMangaImpl()
            val chapters = backupManga.getChaptersImpl()
            val categories = backupManga.categories
            val history = backupManga.history
            val tracks = backupManga.getTrackingImpl()

            val dbManga = backupManager.getMangaFromDatabase(manga)
            val dbMangaExists = dbManga != null

            if (dbMangaExists) {
                backupManager.restoreMangaNoFetch(manga, dbManga!!)
            } else {
                manga.initialized = false
                manga.id = backupManager.insertManga(manga)
            }
            manga.user_cover?.let {
                runCatching {
                    coverCache.setCustomCoverToCache(manga, manga.user_cover!!)
                    MangaCoverMetadata.remove(manga.id!!)
                }
            }
            backupManager.restoreChaptersForMangaOffline(manga, chapters)
            backupManager.restoreCategoriesForManga(manga, categories, backupCategories)
            backupManager.restoreHistoryForManga(history)
            backupManager.restoreTrackForManga(manga, tracks)
        } catch (e: Exception) {
            XLog.e(e)
            errors.add("$title - ${e.message}")
        }
    }
}
