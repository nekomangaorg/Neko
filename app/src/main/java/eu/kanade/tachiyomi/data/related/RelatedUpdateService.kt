package eu.kanade.tachiyomi.data.related

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MangaRelatedImpl
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.customize
import eu.kanade.tachiyomi.util.isServiceRunning
import eu.kanade.tachiyomi.util.notificationManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RelatedUpdateService(
    val db: DatabaseHelper = Injekt.get()
) : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    var relatedServiceScope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * Subscription where the update is done.
     */
    private var job: Job? = null

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelRelatedUpdatePendingBroadcast(this)
    }

    private val progressNotification by lazy {
        NotificationCompat.Builder(this, Notifications.CHANNEL_RELATED)
            .customize(
                this,
                getString(R.string.pref_related_loading_progress_start),
                R.drawable.ic_neko_notification,
                true
            )
            .setAutoCancel(true)
            .addAction(R.drawable.ic_clear_grey, getString(android.R.string.cancel), cancelIntent)
    }

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_RELATED_PROGRESS, progressNotification.build())
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "RelatedUpdateService:WakeLock"
        )
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(30))
    }

    /**
     * Method called when the service is destroyed. It destroys subscriptions and releases the wake
     * lock.
     */
    override fun onDestroy() {
        job?.cancel()
        relatedServiceScope.cancel()
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
        job = relatedServiceScope.launch(handler) {
            updateRelated()
        }
        job?.invokeOnCompletion { stopSelf(startId) }

        return START_REDELIVER_INTENT
    }

    /**
     * Method that updates the related database for manga
     */
    private fun updateRelated() {
        val jsonString =
            RelatedHttpService.create().getRelatedResults().execute().body().toString()
        val relatedPageResult = JSONObject(jsonString)
        val totalManga = relatedPageResult.length()

        // Delete the old related table
        db.deleteAllRelated().executeAsBlocking()

        // Loop through each and insert into the database
        var counter: Int = 0
        val batchMultiple = 1000
        val dataToInsert = mutableListOf<MangaRelatedImpl>()
        for (key in relatedPageResult.keys()) {

            // Check if the service is currently running
            if (job?.isCancelled!!) {
                cancelProgressNotification()
                showResultNotification(true)
                return
            }

            // Get our two arrays of ids and titles
            val matchedIds =
                relatedPageResult.getJSONObject(key).getJSONArray("m_ids")
            val matchedTitles =
                relatedPageResult.getJSONObject(key).getJSONArray("m_titles")
            if (matchedIds.length() != matchedTitles.length()) {
                continue
            }

            // create the implementation and insert
            val related = MangaRelatedImpl()
            related.id = counter.toLong()
            related.manga_id = key.toLong()
            related.matched_ids = matchedIds.toString()
            related.matched_titles = matchedTitles.toString()
            dataToInsert.add(related)

            // display to the user
            counter++
            showProgressNotification(counter, totalManga)

            // Every batch of manga, insert into the database
            if (counter % batchMultiple == 0) {
                db.insertManyRelated(dataToInsert).executeAsBlocking()
                dataToInsert.clear()
            }
        }

        // Insert the last bit in the case we are not divisable by 1000
        if (dataToInsert.isNotEmpty()) {
            db.insertManyRelated(dataToInsert).executeAsBlocking()
            dataToInsert.clear()
        }
        cancelProgressNotification()
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
            Notifications.ID_RELATED_PROGRESS, progressNotification
                .setContentTitle(
                    getString(
                        R.string.pref_related_loading_percent,
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
            getString(R.string.pref_related_loading_complete_error)
        } else {
            getString(
                R.string.pref_related_loading_complete
            )
        }

        val result = NotificationCompat.Builder(this, Notifications.CHANNEL_RELATED)
            .customize(this, title, R.drawable.ic_neko_notification)
            .setAutoCancel(true)
        NotificationManagerCompat.from(this)
            .notify(Notifications.ID_RELATED_COMPLETE, result.build())
    }

    /**
     * Cancels the progress notification.
     */
    private fun cancelProgressNotification() {
        notificationManager.cancel(Notifications.ID_RELATED_PROGRESS)
    }

    companion object {

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(RelatedUpdateService::class.java)
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
                val intent = Intent(context, RelatedUpdateService::class.java)
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
            context.stopService(Intent(context, RelatedUpdateService::class.java))
        }
    }
}
