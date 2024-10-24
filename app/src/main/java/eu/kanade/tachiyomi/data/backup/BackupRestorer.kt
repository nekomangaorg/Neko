package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.BackupUtil
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.notificationManager
import java.io.File
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import okio.source
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class BackupRestorer(val context: Context, val notifier: BackupNotifier) {

    private val restoreHelper = RestoreHelper(context)

    /** The progress of a backup restore */
    private var restoreProgress = 0

    /** Amount of manga in file (needed for restore) */
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
    private val sourceManager: SourceManager by injectLazy()

    suspend fun restoreBackup(uri: Uri): Boolean {
        val startTime = System.currentTimeMillis()
        restoreProgress = 0
        errors.clear()
        trackingErrors.clear()
        cancelled = 0

        if (!performRestore(uri)) {
            return false
        }
        val endTime = System.currentTimeMillis()
        val time = endTime - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(time, errors.size, logFile?.parent, logFile?.name)
        return true
    }

    suspend fun performRestore(uri: Uri): Boolean {
        val backup = BackupUtil.decodeBackup(context, uri)

        val partitionedList = backup.backupManga.partition { sourceManager.isMangadex(it.source) }

        return coroutineScope {
            val dexManga = partitionedList.first
            skippedTitles = partitionedList.second.map { it.title }
            totalAmount = backup.backupManga.size
            skippedAmount = totalAmount - dexManga.size
            restoreAmount = dexManga.size

            if (backup.backupCategories.isNotEmpty()) {
                restoreCategories(backup.backupCategories)
            }

            dexManga
                .groupBy { MdUtil.getMangaUUID(it.url) }
                .forEach { (_, mangaList) ->
                    if (!isActive) {
                        return@coroutineScope false
                    }
                    restoreManga(mangaList.first().title, mangaList, backup.backupCategories)
                }

            context.notificationManager.cancel(Notifications.ID_RESTORE_PROGRESS)

            cancelled = errors.count { it.contains("cancelled", true) }
            val tmpErrors = errors.filter { !it.contains("cancelled", true) }
            errors.clear()
            errors.addAll(tmpErrors)

            val logFile = writeErrorLog()
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

            true
        }
    }

    private fun restoreCategories(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        restoreHelper.restoreCategories(backupCategories)
        categoriesAmount = backupCategories.size
        restoreAmount += 1
        restoreProgress += 1
        totalAmount += 1
        restoreHelper.showProgressNotification(
            restoreProgress,
            totalAmount,
            context.getString(R.string.categories),
        )
    }

    private fun restoreManga(
        title: String,
        backupMangaList: List<BackupManga>,
        backupCategories: List<BackupCategory>,
    ) {
        try {
            restoreHelper.showProgressNotification(restoreProgress, totalAmount, title)
            restoreProgress += 1

            val backupManga =
                if (backupMangaList.size == 1) {
                    backupMangaList.first()
                } else {
                    val tempCategories = backupMangaList.map { it.categories }.distinct().flatten()
                    val tempChapters = backupMangaList.map { it.chapters }.distinct().flatten()
                    val tempHistory = backupMangaList.map { it.history }.distinct().flatten()
                    val tempTracks = backupMangaList.map { it.tracking }.distinct().flatten()

                    backupMangaList
                        .first()
                        .copy(
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

            val dbManga = db.getManga(manga.url, manga.source).executeAsBlocking()
            val dbMangaExists = dbManga != null

            // needed for legacy merge manga
            val mergeMangaList =
                if (manga.merge_manga_url != null) {
                    listOf(
                        MergeMangaImpl(
                            mangaId = 0L,
                            url = manga.merge_manga_url!!,
                            mergeType = MergeType.MangaLife,
                        )
                    )
                } else {
                    backupManga.getMergeMangaImpl()
                }

            if (dbMangaExists) {
                restoreHelper.restoreMangaNoFetch(manga, dbManga!!)
            } else {
                manga.initialized = false
                manga.id = db.insertManga(manga).executeAsBlocking().insertedId()
            }
            manga.user_cover?.let {
                runCatching {
                    coverCache.setCustomCoverToCache(manga, manga.user_cover!!)
                    MangaCoverMetadata.remove(manga.id!!)
                }
            }
            restoreHelper.restoreChaptersForMangaOffline(manga, chapters)
            restoreHelper.restoreMergeMangaForManga(manga, mergeMangaList)
            restoreHelper.restoreCategoriesForManga(manga, categories, backupCategories)
            restoreHelper.restoreHistoryForManga(history)
            restoreHelper.restoreTrackForManga(manga, tracks)
        } catch (e: Exception) {
            TimberKt.e(e)
            errors.add("$title - ${e.message}")
        }
    }

    fun writeErrorLog(): File? {
        return restoreHelper.writeErrorLog(errors, skippedAmount, skippedTitles)
    }
}
