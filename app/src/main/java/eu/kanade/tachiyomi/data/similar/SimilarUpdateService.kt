package eu.kanade.tachiyomi.data.similar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MangaSimilarImpl
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.customize
import eu.kanade.tachiyomi.util.system.isServiceRunning
import eu.kanade.tachiyomi.util.system.notificationManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class SimilarUpdateService(
    val db: DatabaseHelper = Injekt.get()
) : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    var similarServiceScope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * Subscription where the update is done.
     */
    private var job: Job? = null

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelSimilarUpdatePendingBroadcast(this)
    }

    private val progressNotification by lazy {
        NotificationCompat.Builder(this, Notifications.CHANNEL_SIMILAR)
            .customize(
                this,
                getString(R.string.similar_loading_progress_start),
                R.drawable.ic_neko_notification,
                true
            )
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_clear_grey_24dp_img,
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
        startForeground(Notifications.ID_SIMILAR_PROGRESS, progressNotification.build())
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
        similarServiceScope.cancel()
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

        // Unsubscribe from any previous subscription if needed.
        job?.cancel()
        val handler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception)
            stopSelf(startId)
            showResultNotification(true)
            cancelProgressNotification()
        }
        job = similarServiceScope.launch(handler) {
            updateSimilar()
        }
        job?.invokeOnCompletion { stopSelf(startId) }

        return START_REDELIVER_INTENT
    }

    /**
     * Method that updates the similar database for manga
     */
    private suspend fun updateSimilar() = withContext(Dispatchers.IO) {
        val jsonString =
            SimilarHttpService.create().getSimilarResults().execute().body().toString()
        val similarPageResult = JSONObject(jsonString)
        val totalManga = similarPageResult.length()

        // Delete the old similar table
        db.deleteAllSimilar().executeAsBlocking()

        // Loop through each and insert into the database
        var counter: Int = 0
        val batchMultiple = 1000
        val dataToInsert = mutableListOf<MangaSimilarImpl>()
        for (key in similarPageResult.keys()) {

            // Check if the service is currently running
            if (!this.isActive) {
                break
            }

            // Get our two arrays of ids and titles
            val matchedIds =
                similarPageResult.getJSONObject(key).getJSONArray("m_ids")
            val matchedTitles =
                similarPageResult.getJSONObject(key).getJSONArray("m_titles")
            if (matchedIds.length() != matchedTitles.length()) {
                continue
            }

            // create the implementation and insert
            val similar = MangaSimilarImpl()
            similar.id = counter.toLong()
            similar.manga_id = key.toLong()
            similar.matched_ids = matchedIds.toString()
            similar.matched_titles = matchedTitles.toString()
            dataToInsert.add(similar)

            // display to the user
            counter++
            showProgressNotification(counter, totalManga)

            // Every batch of manga, insert into the database
            if (counter % batchMultiple == 0) {
                db.insertSimilar(dataToInsert).executeAsBlocking()
                dataToInsert.clear()
            }
        }

        // Insert the last bit in the case we are not divisable by 1000
        if (dataToInsert.isNotEmpty()) {
            db.insertSimilar(dataToInsert).executeAsBlocking()
            dataToInsert.clear()
        }
        cancelProgressNotification()
        showResultNotification(!this.isActive)
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
            Notifications.ID_SIMILAR_PROGRESS, progressNotification
                .setContentTitle(
                    getString(
                        R.string.similar_loading_percent,
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
    private fun showResultNotification(error: Boolean = false) {

        val title = if (error) {
            getString(R.string.similar_loading_complete_error)
        } else {
            getString(
                R.string.similar_loading_complete
            )
        }

        val result = NotificationCompat.Builder(this, Notifications.CHANNEL_SIMILAR)
            .customize(this, title, R.drawable.ic_neko_notification)
            .setAutoCancel(true)
        NotificationManagerCompat.from(this)
            .notify(Notifications.ID_SIMILAR_COMPLETE, result.build())
    }

    /**
     * Cancels the progress notification.
     */
    private fun cancelProgressNotification() {
        notificationManager.cancel(Notifications.ID_SIMILAR_PROGRESS)
    }

    companion object {

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(SimilarUpdateService::class.java)
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         * @param category a specific category to update, or null for global update.
         * @param target defines what should be updated.
         */
        fun start(context: Context) {
            if (!isRunning(context)) {
                val intent = Intent(context, SimilarUpdateService::class.java)
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
            context.stopService(Intent(context, SimilarUpdateService::class.java))
        }
    }
}
