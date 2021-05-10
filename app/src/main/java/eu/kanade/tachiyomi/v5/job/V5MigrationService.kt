package eu.kanade.tachiyomi.v5.job

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.filterIfUsingCache
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.isServiceRunning
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import eu.kanade.tachiyomi.v5.db.V5DbQueries
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/**
 * This class will perform migration of old mangas ids to the new v5 mangadex.
 */
class V5MigrationService(
        val db: DatabaseHelper = Injekt.get(),
        val dbV5: V5DbHelper = Injekt.get(),
        val preferences: PreferencesHelper = Injekt.get(),
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

    val count = AtomicInteger(0)
    val jobCount = AtomicInteger(0)

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        notifier = V5MigrationNotifier(this)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "V5MigrationService:WakeLock"
        )
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(30))
        startForeground(Notifications.ID_UPDATER, notifier.progressNotificationBuilder.build())
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
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

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
            // update mangas
            updateMangas()
            // update chapters
            //updateChapters()
        }
        job?.invokeOnCompletion { stopSelf(startId) }

        // Return that we have started
        return START_REDELIVER_INTENT
    }


    /**
     * This will migrate the mangas in the library to the new ids
     */
    private fun updateMangas() {

        val db = Injekt.get<DatabaseHelper>()
        val dbV5 = Injekt.get<V5DbHelper>()
        val mangas = db.getMangas().executeAsBlocking()
        mangas.forEach {

            // Return if job was canceled
            if (job?.isCancelled == true) {
                return
            }

            // Update progress bar
            notifier.showProgressNotification(it, 0, mangas.size)

            // Get the new id for this manga
            // TODO: need to skip mangas which have already been converted...
            val newId = V5DbQueries.getNewMangaId(dbV5.db, MdUtil.getMangaId(it.url))
            XLog.e("GOLDBATTLE: migrated ${MdUtil.getMangaId(it.url)} to $newId")
            if (newId != "") {
                it.url = "/manga/${newId}/"
            } else {
                failedUpdatesMangas[it] = "unable to find new id"
            }

            // Commit to the database
            //db.insertManga(it).executeAsBlocking()

        }

    }

    private fun finishUpdates(isDexUp: Boolean = true) {
        if (jobCount.get() != 0) return
        if (failedUpdatesMangas.isNotEmpty() || failedUpdatesChapters.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdatesMangas, failedUpdatesChapters)
            notifier.showUpdateErrorNotification(
                    failedUpdatesMangas.map { it.key.title }
                            +failedUpdatesChapters.map { it.key.chapter_title },
                    errorFile.getUriCompat(this)
            )
        }
        notifier.cancelProgressNotification()
    }


    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    private fun showProgressNotification(manga: SManga, current: Int, total: Int) {
        notifier.showProgressNotification(manga, current, total)
    }

    /**
     * Writes basic file of update errors to cache dir.
     */
    private fun writeErrorFile(errors1: Map<Manga, String?>, errors2: Map<Chapter, String?>): File {
        try {
            if (errors1.isNotEmpty() || errors2.isNotEmpty()) {
                val destFile = File(externalCacheDir, "neko_update_errors.txt")
                destFile.bufferedWriter().use { out ->
                    errors1.forEach { (manga, error) ->
                        out.write("${manga.title}: $error\n")
                    }
                    errors2.forEach { (chapter, error) ->
                        out.write("vol ${chapter.vol} - ${chapter.chapter_number} - ${chapter.name}: $error\n")
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

        private var instance: V5MigrationService? = null

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
            instance?.job?.cancel()
            GlobalScope.launch {
                instance?.jobCount?.set(0)
                instance?.finishUpdates()
            }
            context.stopService(Intent(context, V5MigrationService::class.java))
        }

    }
}

