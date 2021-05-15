package eu.kanade.tachiyomi.data.similar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.CachedManga
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.customize
import eu.kanade.tachiyomi.util.system.isServiceRunning
import eu.kanade.tachiyomi.util.system.notificationManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class MangaCacheUpdateService(
    val db: DatabaseHelper = Injekt.get()
) : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock
    var scope = CoroutineScope(Dispatchers.IO + Job())
    private var job: Job? = null

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelCacheUpdatePendingBroadcast(this)
    }

    private val progressNotification by lazy {
        NotificationCompat.Builder(this, Notifications.CHANNEL_CACHE)
            .customize(
                this,
                getString(R.string.cache_loading_progress_start),
                R.drawable.ic_neko_notification,
                true
            )
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_close_24dp,
                getString(android.R.string.cancel),
                cancelIntent
            )
    }

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_CACHE_PROGRESS, progressNotification.build())
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "SimilarUpdateService:WakeLock"
        )
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(30))
    }

    /**
     * Method called when the service is destroyed. It destroys subscriptions and releases the wake
     * lock.
     */
    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
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
        if (intent == null) return Service.START_NOT_STICKY

        job?.cancel()
        val handler = CoroutineExceptionHandler { _, exception ->
            XLog.e(exception)
            stopSelf(startId)
            cancelProgressNotification()
        }
        job = scope.launch(handler) {
            updateCachedManga()
            cancelProgressNotification()
        }
        job?.invokeOnCompletion { stopSelf(startId) }

        return START_REDELIVER_INTENT
    }

    private suspend fun updateCachedManga() = withContext(Dispatchers.IO) {

        // Open the connection to the remove csv file
        // https://stackoverflow.com/a/38374532/7718197
        XLog.i("CACHE: Starting download!")
        val conn = URL(MdUtil.similarCacheMapping).openConnection()
        val bufferIn = BufferedReader(InputStreamReader(conn.getInputStream()))

        // Loop through each line
        val lines = ArrayList<String>()
        var str: String
        while (true) {
            str = bufferIn.readLine() ?: break
            lines.add(str)
        }
        bufferIn.close()

        // Delete the old search database
        kotlin.runCatching {
            db.deleteAllCached().executeAsBlocking()
        }

        // Insert into our database
        XLog.i("CACHE: Beginning cache manga insert")
        //db.insertCachedManga2(cachedManga)
        val totalManga = lines.size
        lines.mapIndexed { index, line ->

            // Return if job was canceled
            if (job?.isCancelled == true) {
                return@mapIndexed
            }

            // Insert into the database
            showProgressNotification(index, totalManga)
            val strs = line.split(",").toTypedArray()
            if(strs.size == 3) {
                val regex = Regex("[^A-Za-z0-9 ]")
                val manga = CachedManga(regex.replace(strs[1],""), strs[0], strs[2])
                db.insertCachedManga2Single(manga)
            }

        }
        db.optimizeCachedManga()
        showProgressNotification(totalManga, totalManga)
        XLog.i("CACHE: Inserted cached manga: ${db.getCachedMangaCount()}")

        // Done!
        XLog.i("CACHE: Done with cached manga")
        showResultNotification()

    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    private fun showProgressNotification(current: Int, total: Int) {
        notificationManager.notify(
            Notifications.ID_CACHE_PROGRESS,
            progressNotification
                .setContentTitle(
                    getString(
                        R.string.cache_loading_percent,
                        current,
                        total
                    )
                )
                .setProgress(total, current, false)
                .build()
        )
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of manga with new updates.
     */
    private fun showResultNotification(error: Boolean = false, message: String? = null) {
        val title = if (error) {
            message ?: getString(R.string.cache_loading_complete_error)
        } else {
            getString(
                R.string.cache_loading_complete
            )
        }
        val result = NotificationCompat.Builder(this, Notifications.CHANNEL_CACHE)
            .customize(this, title, R.drawable.ic_neko_notification)
            .setAutoCancel(true)
        NotificationManagerCompat.from(this)
            .notify(Notifications.ID_CACHE_COMPLETE, result.build())
    }

    /**
     * Cancels the progress notification.
     */
    private fun cancelProgressNotification() {
        notificationManager.cancel(Notifications.ID_CACHE_PROGRESS)
    }

    companion object {

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(MangaCacheUpdateService::class.java)
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         */
        fun start(context: Context) {
            if (!isRunning(context)) {
                val intent = Intent(context, MangaCacheUpdateService::class.java)
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
            context.stopService(Intent(context, MangaCacheUpdateService::class.java))
        }
    }
}