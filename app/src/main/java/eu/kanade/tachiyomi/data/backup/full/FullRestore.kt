package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.net.Uri
import androidx.core.text.isDigitsOnly
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.backup.RestoreHelper
import eu.kanade.tachiyomi.data.backup.full.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.full.models.BackupManga
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import eu.kanade.tachiyomi.v5.db.V5DbQueries
import kotlinx.coroutines.Job
import okio.buffer
import okio.gzip
import okio.source
import uy.kohesive.injekt.injectLazy

class FullRestore(val context: Context, val job: Job?) {

    lateinit var backupManager: FullBackupManager
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
    internal val dbV5: V5DbHelper by injectLazy()
    internal val trackManager: TrackManager by injectLazy()

    suspend fun restoreBackup(uri: Uri) {
        backupManager = FullBackupManager(context)

        val backupString = context.contentResolver.openInputStream(uri)!!.source().gzip().buffer().use { it.readByteArray() }
        val backup = backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)

        val partitionedList = backup.backupManga.partition { backupManager.sourceManager.isMangadex(it.source) }

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

        dexManga.forEach {
            restoreManga(it, backup.backupCategories)
        }

        context.notificationManager.cancel(Notifications.ID_RESTORE_PROGRESS)

        cancelled = errors.count { it.contains("cancelled", true) }
        val tmpErrors = errors.filter { !it.contains("cancelled", true) }
        errors.clear()
        errors.addAll(tmpErrors)

        val logFile = restoreHelper.writeErrorLog(errors, skippedAmount, skippedTitles)
        restoreHelper.showResultNotification(logFile.parent, logFile.name, categoriesAmount, restoreProgress, restoreAmount, skippedAmount, totalAmount, cancelled, errors, trackingErrors)
    }

    private fun restoreCategories(backupCategories: List<BackupCategory>) {
        backupManager.restoreCategories(backupCategories)
        categoriesAmount = backupCategories.size
        restoreAmount += 1
        restoreProgress += 1
        totalAmount += 1
        restoreHelper.showProgressNotification(restoreProgress, totalAmount, "Categories added")
    }

    private fun restoreManga(backupManga: BackupManga, backupCategories: List<BackupCategory>) {
        try {
            if (job?.isCancelled == false) {
                restoreHelper.showProgressNotification(restoreProgress, totalAmount, backupManga.title)
                restoreProgress += 1
            } else {
                throw java.lang.Exception("Job was cancelled")
            }
            var manga = backupManga.getMangaImpl()
            val chapters = backupManga.getChaptersImpl()
            val categories = backupManga.categories
            val history = backupManga.history
            val tracks = backupManga.getTrackingImpl()

            var dbManga = backupManager.getMangaFromDatabase(manga)
            val dbMangaExists = dbManga != null

            // If it is an old pre-V5 manga try to find the new id
            val oldMangaId = MdUtil.getMangaId(manga.url)
            val isNumericId = oldMangaId.isDigitsOnly()
            if (isNumericId) {
                val newMangaId = V5DbQueries.getNewMangaId(dbV5.idDb, oldMangaId)
                if (newMangaId.isNotBlank()) {
                    manga.url = "/title/$newMangaId"
                }
            }

            if (dbMangaExists) {
                backupManager.restoreMangaNoFetch(manga, dbManga!!)
            } else {
                manga.initialized = false
                manga.favorite = true
                manga.id = backupManager.insertManga(manga)
            }
            backupManager.restoreChaptersForMangaOffline(manga, chapters)
            backupManager.restoreCategoriesForManga(manga, categories, backupCategories)
            backupManager.restoreHistoryForManga(history)
            backupManager.restoreTrackForManga(manga, tracks)
        } catch (e: Exception) {
            XLog.e(e)
            errors.add("${backupManga.title} - ${e.message}")
        }
    }
}
