package eu.kanade.tachiyomi.v5.job

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.text.isDigitsOnly
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.dto.LegacyIdDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.isServiceRunning
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * This class will perform migration of old mangaList ids to the new v5 mangadex.
 */
class V5MigrationService(
    val db: DatabaseHelper = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val trackManager: TrackManager = Injekt.get(),
    val networkHelper: NetworkHelper = Injekt.get(),
) : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var notifier: V5MigrationNotifier

    private var job: Job? = null

    // List containing failed updates
    private val failedUpdatesMangaList = mutableMapOf<Manga, String?>()
    private val failedUpdatesChapters = mutableMapOf<Chapter, String?>()
    private val failedUpdatesErrors = mutableListOf<String>()

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        notifier = V5MigrationNotifier(this)
        startForeground(
            Notifications.ID_V5_MIGRATION_PROGRESS,
            notifier.progressNotificationBuilder.build()
        )
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "V5MigrationService:WakeLock"
        )
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(30))
    }

    /**
     * Method called when the service is destroyed. It cancels jobs and releases the wake lock.
     */
    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent) = null

    /**
     * Method called when the service receives an intent.
     *
     * @param intent the start intent from.
     * @param flags the flags of the command.
     * @param startId the start id of this command.
     * @return the start value of the command.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        // Update our library
        val handler = CoroutineExceptionHandler { _, exception ->
            XLog.e(exception)
            stopSelf(startId)
        }
        job = GlobalScope.launch(handler) {
            migrateLibraryToV5()
            finishUpdates()
        }
        job?.invokeOnCompletion { stopSelf(startId) }

        // Return that we have started
        return START_REDELIVER_INTENT
    }

    /**
     * This will migrate the mangaList in the library to the new ids
     */
    private suspend fun migrateLibraryToV5() {
        val mangaList = db.getMangaList().executeAsBlocking()
        mangaList.forEachIndexed { index, manga ->

            // Return if job was canceled
            if (job?.isCancelled == true) {
                return
            }

            // Update progress bar
            notifier.showProgressNotification(manga, index, mangaList.size)

            // Get the old id and check if it is a number
            val oldMangaId = MdUtil.getMangaId(manga.url)
            val isNumericId = oldMangaId.isDigitsOnly()

            // Get the new id for this manga
            // We skip mangaList which have already been converted (non-numeric ids)
            var mangaErroredOut = false
            if (isNumericId) {
                val responseDto = networkHelper.service.legacyMapping(
                    LegacyIdDto(
                        type = "manga",
                        listOf(oldMangaId.toInt())
                    )
                )

                if (responseDto.isSuccessful) {
                    val newId = responseDto.body()!!.first().data.attributes.newId
                    manga.url = "/title/$newId"
                    manga.initialized = false
                    manga.thumbnail_url = null
                    db.insertManga(manga).executeAsBlocking()
                    val tracks = db.getTracks(manga).executeAsBlocking()
                    tracks.firstOrNull { it.sync_id == trackManager.mdList.id }?.let {
                        it.tracking_url = MdUtil.baseUrl + manga.url
                        db.insertTrack(it).executeAsBlocking()
                    }
                } else {
                    failedUpdatesMangaList[manga] = "unable to find new manga id"
                    failedUpdatesErrors.add(manga.title + ": unable to find new manga id, MangaDex might have removed this manga or the id changed")
                    mangaErroredOut = true
                }
            }

            // Now loop through the chapters for this manga and update them
            val chapters = db.getChapters(manga).executeAsBlocking()
            val chapterErrors = mutableListOf<String>()
            if (!mangaErroredOut) {
                val chapterMap = chapters.filter { it.isMergedChapter().not() }
                    .filter { it.mangadex_chapter_id.isDigitsOnly() }
                    .map { it.mangadex_chapter_id.toInt() to it }
                    .toMap()
                val chapterChunks = chapters.filter { it.isMergedChapter().not() }
                    .filter { it.mangadex_chapter_id.isDigitsOnly() }
                    .map { it.mangadex_chapter_id.toInt() }
                    .chunked(100)

                chapterChunks.asSequence().forEach { legacyIds ->
                    val responseDto =
                        networkHelper.service.legacyMapping(LegacyIdDto("chapter", legacyIds))
                    if (responseDto.isSuccessful) {
                        responseDto.body()!!.forEach { legacyMappingDto ->
                            if (job?.isCancelled == true) {
                                return
                            }
                            val oldId = legacyMappingDto.data.attributes.legacyId
                            val newId = legacyMappingDto.data.attributes.newId
                            val chapter = chapterMap[oldId]!!
                            chapter.mangadex_chapter_id = newId
                            chapter.url = MdUtil.chapterSuffix + newId
                            chapter.old_mangadex_id = oldId.toString()
                            db.insertChapter(chapter).executeAsBlocking()
                        }
                    } else {
                        legacyIds.forEach {
                            val failedChapter = chapterMap[it]!!
                            failedUpdatesChapters[failedChapter] =
                                "unable to find new chapter V5 id deleting chapter"
                            chapterErrors.add(
                                "\t- unable to find new chapter id for " +
                                    "vol ${failedChapter.vol} - ${failedChapter.chapter_number} - ${failedChapter.name}"
                            )
                            db.deleteChapter(failedChapter).executeAsBlocking()
                        }
                    }
                }

                // Append chapter errors if we have them
                if (chapterErrors.size > 0) {
                    failedUpdatesErrors.add(manga.title + ": has chapter conversion errors")
                    chapterErrors.forEach {
                        failedUpdatesErrors.add(it)
                    }
                }
            }
        }
    }

    /**
     * Finall function called when we have finished / requested to stop the update
     */
    private fun finishUpdates() {
        if (failedUpdatesMangaList.isNotEmpty() || failedUpdatesChapters.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdatesErrors)
            notifier.showUpdateErrorNotification(
                failedUpdatesMangaList.map { it.key.title } +
                    failedUpdatesChapters.map { it.key.chapter_title },
                errorFile.getUriCompat(this)
            )
        }
        notifier.cancelProgressNotification()
    }

    /**
     * Writes basic file of update errors to cache dir.
     */
    private fun writeErrorFile(errors: MutableList<String>): File {
        try {
            if (errors.isNotEmpty()) {
                val destFile = File(externalCacheDir, "neko_v5_migration_errors.txt")
                destFile.bufferedWriter().use { out ->
                    errors.forEach { error ->
                        out.write("$error\n")
                    }
                }
                return destFile
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }

    companion object {

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(V5MigrationService::class.java)
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         */
        fun start(context: Context) {
            if (!isRunning(context)) {
                val intent = Intent(context, V5MigrationService::class.java)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(intent)
                } else {
                    context.startForegroundService(intent)
                }
            }
        }

        /**
         * Stops the service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            GlobalScope.launch {
                context.getSystemService(V5MigrationService::class.java)?.finishUpdates()
            }
            context.stopService(Intent(context, V5MigrationService::class.java))
        }
    }
}
