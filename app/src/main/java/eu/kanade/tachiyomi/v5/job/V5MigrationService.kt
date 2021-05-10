package eu.kanade.tachiyomi.v5.job

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.isServiceRunning
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import eu.kanade.tachiyomi.v5.db.V5DbQueries
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.lang.Double.parseDouble
import java.util.concurrent.TimeUnit

/**
 * This class will perform migration of old mangas ids to the new v5 mangadex.
 */
class V5MigrationService(
        val db: DatabaseHelper = Injekt.get(),
        val dbV5: V5DbHelper = Injekt.get(),
        val preferences: PreferencesHelper = Injekt.get()
) : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var notifier: V5MigrationNotifier

    private var job: Job? = null

    // List containing failed updates
    private val failedUpdatesMangas = mutableMapOf<Manga, String?>()
    private val failedUpdatesChapters = mutableMapOf<Chapter, String?>()
    private val failedUpdatesErrors = mutableListOf<String>()

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        notifier = V5MigrationNotifier(this)
        startForeground(Notifications.ID_V5_MIGRATION_PROGRESS, notifier.progressNotificationBuilder.build())
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "V5MigrationService:WakeLock"
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
     * This will migrate the mangas in the library to the new ids
     */
    private fun migrateLibraryToV5() {
        val mangas = db.getMangas().executeAsBlocking()
        mangas.forEachIndexed { index, manga ->

            // Return if job was canceled
            if (job?.isCancelled == true) {
                return
            }

            // Update progress bar
            notifier.showProgressNotification(manga, index, mangas.size)

            // Get the old id and check if it is a number
            val oldMangaId = MdUtil.getMangaId(manga.url)
            var numeric = true
            try {
                parseDouble(oldMangaId)
            } catch (e: NumberFormatException) {
                numeric = false
            }

            // Get the new id for this manga
            // We skip mangas which have already been converted (non-numeric ids)
            var mangaDeleted = false
            if(numeric) {
                val newMangaId = V5DbQueries.getNewMangaId(dbV5.db, oldMangaId)
                if (newMangaId != "") {
                    manga.url = "/manga/${newMangaId}/"
                    db.insertManga(manga).executeAsBlocking()
                } else {
                    failedUpdatesMangas[manga] = "unable to find new manga id"
                    failedUpdatesErrors.add(manga.title+": unable to find new manga id")
                    db.deleteManga(manga).executeAsBlocking()
                    mangaDeleted = true
                }
            }

            // Now loop through the chapters for this manga and update them
            val chapters = db.getChapters(manga).executeAsBlocking()
            val chapterErrors = mutableListOf<String>()
            if(!mangaDeleted) {
                chapters.forEach { chapter ->
                    // Return if job was canceled
                    if (job?.isCancelled == true) {
                        return
                    }
                    // Get the old id and check if it is a number
                    //val oldChapterId = MdUtil.getChapterId(chapter.url).trimEnd('/')
                    val oldChapterId = chapter.mangadex_chapter_id
                    numeric = true
                    try {
                        parseDouble(oldChapterId)
                    } catch (e: NumberFormatException) {
                        numeric = false
                    }
                    // Get the new id for this chapter
                    // We skip chapters which have already been converted (non-numeric ids)
                    if(numeric) {
                        val newChapterId = V5DbQueries.getNewChapterId(dbV5.db, oldChapterId)
                        if (newChapterId != "") {
                            chapter.mangadex_chapter_id = newChapterId
                            chapter.url = MdUtil.chapterSuffix + newChapterId
                            db.insertChapter(chapter).executeAsBlocking()
                        } else {
                            failedUpdatesChapters[chapter] = "unable to find new chapter V5 id"
                            chapterErrors.add("\t- unable to find new chapter id for " +
                                    "vol ${chapter.vol} - ${chapter.chapter_number} - ${chapter.name}")
                            db.deleteChapter(chapter).executeAsBlocking()
                        }
                    }
                }
                // Append chapter errors if we have them
                if(chapterErrors.size > 0) {
                    failedUpdatesErrors.add(manga.title+": has chapter conversion errors")
                    chapterErrors.forEach {
                        failedUpdatesErrors.add(it)
                    }
                }
            } else {
                chapters.forEach { chapter ->
                    failedUpdatesErrors.add("\t- deleting vol ${chapter.vol} - ${chapter.chapter_number} - ${chapter.name}")
                    db.deleteChapter(chapter).executeAsBlocking()
                }
            }

        }
    }

    /**
     * Finall function called when we have finished / requested to stop the update
     */
    private fun finishUpdates() {
        if (failedUpdatesMangas.isNotEmpty() || failedUpdatesChapters.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdatesErrors)
            notifier.showUpdateErrorNotification(
                    failedUpdatesMangas.map { it.key.title }
                            + failedUpdatesChapters.map { it.key.chapter_title },
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

