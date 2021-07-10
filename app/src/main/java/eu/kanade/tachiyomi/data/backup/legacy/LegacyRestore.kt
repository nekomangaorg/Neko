package eu.kanade.tachiyomi.data.backup.legacy

import android.content.Context
import android.net.Uri
import com.elvishew.xlog.XLog
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.RestoreHelper
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup
import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.notificationManager
import kotlinx.coroutines.Job
import uy.kohesive.injekt.injectLazy

class LegacyRestore(val context: Context, val job: Job?) {

    lateinit var backupManager: LegacyBackupManager

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

    private var skippedTitles = mutableListOf<String>()

    private var categoriesAmount = 0

    /**
     * List containing errors
     */
    private val errors = mutableListOf<String>()

    /**
     * count of cancelled
     */
    private var cancelled = 0

    /**
     * List containing distinct errors
     */
    private val trackingErrors = mutableListOf<String>()

    /**
     * Database
     */
    private val db: DatabaseHelper by injectLazy()

    /**
     * Tracking manager
     */
    internal val trackManager: TrackManager by injectLazy()

    /**
     * Restore a backup json file
     */
    suspend fun restoreBackup(uri: Uri) {
        val reader = JsonReader(context.contentResolver.openInputStream(uri)!!.bufferedReader())
        val json = JsonParser.parseReader(reader).asJsonObject

        // Get parser version
        val version = json.get(Backup.VERSION)?.asInt ?: 1

        // Initialize manager
        backupManager = LegacyBackupManager(context, version)

        val mangaListJson = json.get(Backup.MANGAS).asJsonArray

        val mangdexManga = mangaListJson.filter {
            val manga = backupManager.parser.fromJson<MangaImpl>(it.asJsonObject.get(Backup.MANGA))
            val isMangaDex = backupManager.sourceManager.isMangadex(manga.source)
            if (!isMangaDex) {
                skippedTitles.add(manga.title)
            }
            isMangaDex
        }
        // +1 for categories
        totalAmount = mangaListJson.size()
        skippedAmount = mangaListJson.size() - mangdexManga.count()
        restoreAmount = mangdexManga.count()
        trackingErrors.clear()
        errors.clear()
        cancelled = 0
        // Restore categories
        restoreCategories(json, backupManager)

        mangdexManga.forEach {
            restoreManga(it.asJsonObject)
        }

        context.notificationManager.cancel(Notifications.ID_RESTORE_PROGRESS)

        cancelled = errors.count { it.contains("cancelled", true) }
        val tmpErrors = errors.filter { !it.contains("cancelled", true) }
        errors.clear()
        errors.addAll(tmpErrors)

        val logFile = restoreHelper.writeErrorLog(errors, skippedAmount, skippedTitles)
        restoreHelper.showResultNotification(
            logFile.parent,
            logFile.name,
            categoriesAmount,
            restoreProgress,
            restoreAmount,
            skippedAmount,
            totalAmount,
            cancelled,
            errors,
            trackingErrors
        )
    }

    /**Restore categories if they were backed up
     *
     */
    private fun restoreCategories(json: JsonObject, backupManager: LegacyBackupManager) {
        val element = json.get(Backup.CATEGORIES)
        if (element != null) {
            backupManager.restoreCategories(element.asJsonArray)
            categoriesAmount = element.asJsonArray.size()
            restoreAmount += 1
            restoreProgress += 1
            totalAmount += 1
            restoreHelper.showProgressNotification(restoreProgress, totalAmount, "Categories added")
        }
    }

    /**
     * Restore manga from json  this should be refactored more at some point to prevent the manga object from being mutable
     */
    private suspend fun restoreManga(obj: JsonObject) {
        val manga = backupManager.parser.fromJson<MangaImpl>(obj.get(Backup.MANGA))
        val chapters = backupManager.parser.fromJson<List<ChapterImpl>>(
            obj.get(Backup.CHAPTERS)
                ?: JsonArray()
        )
        val categories =
            backupManager.parser.fromJson<List<String>>(obj.get(Backup.CATEGORIES) ?: JsonArray())
        val history =
            backupManager.parser.fromJson<List<DHistory>>(obj.get(Backup.HISTORY) ?: JsonArray())
        val tracks =
            backupManager.parser.fromJson<List<TrackImpl>>(obj.get(Backup.TRACK) ?: JsonArray())
        val source = backupManager.sourceManager.getMangadex()

        try {
            if (job?.isCancelled == false) {
                restoreHelper.showProgressNotification(restoreProgress, totalAmount, manga.title)
                restoreProgress += 1
            } else {
                throw java.lang.Exception("Job was cancelled")
            }
            val dbManga = backupManager.getMangaFromDatabase(manga)
            val dbMangaExists = dbManga != null

            if (dbMangaExists) {
                // Manga in database copy information from manga already in database
                backupManager.restoreMangaNoFetch(manga, dbManga!!)
            } else {
                // manga gets details from network
                backupManager.restoreMangaFetch(source, manga)
            }

            // Restore categories
            backupManager.restoreCategoriesForManga(manga, categories)

            if (!dbMangaExists || !backupManager.restoreChaptersForManga(manga, chapters)) {
                // manga gets chapters added
                backupManager.restoreChapterFetch(source, manga, chapters)
            }
            // Restore history
            backupManager.restoreHistoryForManga(history)
            // Restore tracking
            backupManager.restoreTrackForManga(manga, tracks)

            trackingFetch(manga, tracks)
        } catch (e: Exception) {
            XLog.e(e)
            errors.add("${manga.title} - ${e.message}")
        }
    }

    /**
     * [refreshes tracking information
     * @param manga manga that needs updating.
     * @param tracks list containing tracks from restore file.
     */
    private suspend fun trackingFetch(manga: Manga, tracks: List<Track>) {
        // add mdlist tracker backup has it missing

        val validTracks =
            tracks.filter { it.sync_id == TrackManager.MYANIMELIST || it.sync_id == TrackManager.ANILIST || it.sync_id == TrackManager.KITSU }

        if (validTracks.isEmpty()) {
            // always create an mdlist tracker
            val track = trackManager.mdList.createInitialTracker(manga)
            db.insertTrack(track).executeAsBlocking()
        }

        validTracks.forEach { track ->
            val service = trackManager.getService(track.sync_id)
            if (service != null && service.isLogged) {
                try {
                    service.refresh(track)
                    db.insertTrack(track).executeAsBlocking()
                } catch (e: Exception) {
                    errors.add("${manga.title} - ${e.message}")
                }
            } else {
                errors.add(
                    "${manga.title} - ${
                    context.getString(
                        R.string.not_logged_into_,
                        context.getString(service?.nameRes()!!)
                    )
                    }"
                )
                val notLoggedIn = context.getString(
                    R.string.not_logged_into_,
                    context.getString(service?.nameRes()!!)
                )
                trackingErrors.add(notLoggedIn)
            }
        }
    }
}
