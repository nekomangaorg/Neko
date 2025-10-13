package eu.kanade.tachiyomi.data.backup

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isLegacyMergedChapter
import eu.kanade.tachiyomi.source.online.merged.mangalife.MangaLife
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.notificationManager
import java.io.File
import kotlin.math.max
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class RestoreHelper(val context: Context) {

    val db: DatabaseHelper by injectLazy()
    val trackManager: TrackManager by injectLazy()

    /** Pending intent of action that cancels the library update */
    val cancelIntent by lazy {
        NotificationReceiver.cancelRestorePendingBroadcast(
            context,
            Notifications.ID_RESTORE_PROGRESS,
        )
    }

    /** keep a partially constructed progress notification for resuse */
    val progressNotification by lazy {
        NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS)
            .setContentTitle(context.getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_neko_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setColor(ContextCompat.getColor(context, R.color.iconOutline))
            .addAction(
                R.drawable.ic_close_24dp,
                context.getString(android.R.string.cancel),
                cancelIntent,
            )
    }

    /** Get the PendingIntent for the error log */
    fun getErrorLogIntent(path: String, file: String): PendingIntent {
        val destFile = File(path, file)
        val uri = destFile.getUriCompat(context.applicationContext)
        return NotificationReceiver.openFileExplorerPendingActivity(context, uri)
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(current: Int, total: Int, title: String) {
        context.notificationManager.notify(
            Notifications.ID_RESTORE_PROGRESS,
            progressNotification
                .setContentTitle(title.chop(30))
                .setContentText(context.getString(R.string.restoring_progress, current, total))
                .setProgress(total, current, false)
                .build(),
        )
    }

    /**
     * Show an error notification if something happens that prevents the restore from
     * starting/working
     */
    fun showErrorNotification(errorMessage: String) {
        val resultNotification =
            NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_ERROR)
                .setContentTitle(context.getString(R.string.restore_error))
                .setContentText(errorMessage)
                .setSmallIcon(R.drawable.ic_error_24dp)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ContextCompat.getColor(context, R.color.colorError))
        context.notificationManager.notify(
            Notifications.ID_BACKUP_RESTORE_ERROR,
            resultNotification.build(),
        )
    }

    /** Show the result notification with option to show the error log */
    fun showResultNotification(
        path: String?,
        file: String?,
        categoriesAmount: Int,
        restoreProgress: Int,
        restoreAmount: Int,
        skippedAmount: Int,
        totalAmount: Int,
        cancelled: Int,
        errors: List<String>,
        trackingErrorsInitial: List<String>,
    ) {
        val content = mutableListOf<String>()
        if (categoriesAmount > 0) {
            content.add(
                context.resources.getQuantityString(
                    R.plurals.restore_categories,
                    categoriesAmount,
                    categoriesAmount,
                )
            )
        }

        content.add(
            context.getString(
                R.string.restore_completed_successful,
                restoreProgress.toString(),
                restoreAmount.toString(),
            )
        )

        content.add(context.getString(R.string.restore_completed_errors, errors.size.toString()))

        if (skippedAmount > 0) {
            content.add(
                context.getString(
                    R.string.restore_skipped,
                    skippedAmount.toString(),
                    totalAmount.toString(),
                )
            )
        }

        val trackingErrors = trackingErrorsInitial.distinct()
        if (trackingErrors.isNotEmpty()) {
            val trackingErrorsString = trackingErrors.distinct().joinToString("\n")
            content.add(trackingErrorsString)
        }
        if (cancelled > 0) {
            content.add(context.getString(R.string.restore_content_skipped, cancelled))
        }

        val restoreString = content.joinToString("\n")

        val resultNotification =
            NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE)
                .setContentTitle(context.getString(R.string.restore_completed))
                .setContentText(restoreString)
                .setStyle(NotificationCompat.BigTextStyle().bigText(restoreString))
                .setSmallIcon(R.drawable.ic_neko_notification)
                .setColor(ContextCompat.getColor(context, R.color.iconOutline))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (!path.isNullOrEmpty() && !file.isNullOrEmpty()) {
            resultNotification.addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.view_log),
                getErrorLogIntent(path, file),
            )
        }
        context.notificationManager.notify(
            Notifications.ID_RESTORE_COMPLETE,
            resultNotification.build(),
        )
    }

    /** Write errors to error log */
    fun writeErrorLog(
        errors: List<String>,
        skippedAmount: Int,
        skippedTitles: List<String>,
    ): File? {
        try {
            if (errors.isNotEmpty() || skippedTitles.isNotEmpty()) {
                val destFile = File(context.externalCacheDir, "neko_restore.log")

                destFile.bufferedWriter().use { out ->
                    if (skippedAmount > 0) {
                        out.write("skipped titles: \n")
                        skippedTitles.forEach { message -> out.write("$message\n") }
                    }
                    if (errors.isNotEmpty()) {
                        out.write("\n\nErrors: \n")
                        errors.forEach { message -> out.write("$message\n") }
                    }
                }
                return destFile
            }
        } catch (e: Exception) {
            TimberKt.e(e) { "Error writing error file" }
        }
        return null
    }

    fun restoreMangaNoFetch(manga: Manga, dbManga: Manga) {
        manga.id = dbManga.id
        manga.copyFrom(dbManga)
        manga.initialized = false
        manga.favorite = dbManga.favorite || manga.favorite
        db.insertManga(manga).executeAsBlocking()
    }

    /**
     * Restore the categories from proto backup
     *
     * @param backupCategories list containing categories
     */
    internal fun restoreCategories(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        db.inTransaction {
            val dbCategories = db.getCategories().executeAsBlocking()

            // Iterate over them
            backupCategories
                .map { it.getCategoryImpl() }
                .forEach { category ->
                    // Used to know if the category is already in the db
                    var found = false
                    for (dbCategory in dbCategories) {
                        // If the category is already in the db, assign the id to the file's
                        // category
                        // and do nothing
                        if (category.name == dbCategory.name) {
                            category.id = dbCategory.id
                            found = true
                            break
                        }
                    }
                    // If the category isn't in the db, remove the id and insert a new category
                    // Store the inserted id in the category
                    if (!found) {
                        // Let the db assign the id
                        category.id = null
                        val result = db.insertCategory(category).executeAsBlocking()
                        category.id = result.insertedId()?.toInt()
                    }
                }
        }
    }

    /**
     * Restores the categories a manga is in.
     *
     * @param manga the manga whose categories have to be restored.
     * @param categories the categories to restore.
     */
    internal fun restoreCategoriesForManga(
        manga: Manga,
        categories: List<Int>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategories = db.getCategories().executeAsBlocking()
        val mangaCategoriesToUpdate = ArrayList<MangaCategory>(categories.size)
        categories.forEach { backupCategoryOrder ->
            backupCategories
                .firstOrNull { it.order == backupCategoryOrder }
                ?.let { backupCategory ->
                    dbCategories
                        .firstOrNull { dbCategory -> dbCategory.name == backupCategory.name }
                        ?.let { dbCategory ->
                            mangaCategoriesToUpdate += MangaCategory.create(manga, dbCategory)
                        }
                }
        }

        // Update database
        if (mangaCategoriesToUpdate.isNotEmpty()) {
            db.deleteOldMangaListCategories(listOf(manga)).executeAsBlocking()
            db.insertMangaListCategories(mangaCategoriesToUpdate).executeAsBlocking()
        }
    }

    /**
     * Restore history from Json
     *
     * @param history list containing history to be restored
     */
    internal fun restoreHistoryForManga(history: List<BackupHistory>) {
        // List containing history to be updated
        val historyToBeUpdated = ArrayList<History>(history.size)
        for ((url, lastRead, readDuration) in history) {
            val dbHistory = db.getHistoryByChapterUrl(url).executeAsBlocking()
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory.apply {
                    last_read = max(lastRead, dbHistory.last_read)
                    time_read = max(readDuration, dbHistory.time_read)
                }
                historyToBeUpdated.add(dbHistory)
            } else {
                // If not in database create
                db.getChapter(url).executeAsBlocking()?.let {
                    val historyToAdd =
                        History.create(it).apply {
                            last_read = lastRead
                            time_read = readDuration
                        }
                    historyToBeUpdated.add(historyToAdd)
                }
            }
        }
        db.upsertHistoryLastRead(historyToBeUpdated).executeAsBlocking()
    }

    /**
     * Restores the sync of a manga.
     *
     * @param manga the manga whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    internal fun restoreTrackForManga(manga: Manga, tracks: List<Track>) {
        // Fix foreign keys with the current manga id
        val needToUpdate = tracks.any { it.manga_id != manga.id!! }

        tracks.map { it.manga_id = manga.id!! }

        val validTracks = tracks.filter { TrackManager.isValidTracker(it.sync_id) }

        // Get tracks from database
        val dbTracks = db.getTracks(manga).executeAsBlocking()
        val trackToUpdate = mutableListOf<Track>()

        validTracks.forEach { track ->
            val service = trackManager.getService(track.sync_id)
            if (service != null) {
                var isInDatabase = false
                for (dbTrack in dbTracks) {
                    if (track.sync_id == dbTrack.sync_id) {
                        // The sync is already in the db, only update its fields
                        if (track.media_id != dbTrack.media_id) {
                            dbTrack.media_id = track.media_id
                        }
                        if (track.library_id != dbTrack.library_id) {
                            dbTrack.library_id = track.library_id
                        }
                        dbTrack.last_chapter_read =
                            max(dbTrack.last_chapter_read, track.last_chapter_read)
                        isInDatabase = true
                        trackToUpdate.add(dbTrack)
                        break
                    }
                }
                if (!isInDatabase) {
                    // Insert new sync. Let the db assign the id
                    track.id = null
                    trackToUpdate.add(track)
                }
            }
        }
        // Update database
        if (trackToUpdate.isNotEmpty() || needToUpdate) {
            db.insertTracks(trackToUpdate).executeAsBlocking()
            db.getTracks(manga).executeAsBlocking()
        }
    }

    fun restoreMergeMangaForManga(manga: Manga, mergeMangaList: List<MergeMangaImpl>) {
        val dbMergeMangaList = db.getMergeMangaList(manga).executeAsBlocking()
        mergeMangaList.forEach { mergeManga ->
            val dbMergeManga = dbMergeMangaList.find { it.mergeType == mergeManga.mergeType }
            if (dbMergeManga == null) {
                val newMergeManga = mergeManga.copy(mangaId = manga.id!!)
                db.insertMergeManga(newMergeManga).executeAsBlocking()
            }
        }
    }

    internal fun restoreChaptersForMangaOffline(manga: Manga, chapters: List<Chapter>) {
        val dbChapters = db.getChapters(manga).executeAsBlocking()

        chapters.forEach { chapter ->
            val dbChapter = dbChapters.find { it.url == chapter.url }

            if (chapter.isLegacyMergedChapter()) {
                chapter.scanlator = MangaLife.name
            }
            if (dbChapter != null) {
                chapter.id = dbChapter.id

                chapter.copyFrom(dbChapter as SChapter)
                if (dbChapter.read && !chapter.read) {
                    chapter.read = dbChapter.read
                    chapter.last_page_read = dbChapter.last_page_read
                } else if (chapter.last_page_read == 0 && dbChapter.last_page_read != 0) {
                    chapter.last_page_read = dbChapter.last_page_read
                }
                if (!chapter.bookmark && dbChapter.bookmark) {
                    chapter.bookmark = dbChapter.bookmark
                }
            } else {
                chapter.mangadex_chapter_id = MdUtil.getChapterUUID(chapter.url)
            }
            chapter.manga_id = manga.id
        }

        val newChapters = chapters.groupBy { it.id != null }
        newChapters[true]?.let { db.updateKnownChaptersBackup(it).executeAsBlocking() }
        newChapters[false]?.let { db.insertChapters(it).executeAsBlocking() }
    }
}
