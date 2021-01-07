package eu.kanade.tachiyomi.data.similar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.text.isDigitsOnly
import com.elvishew.xlog.XLog
import com.squareup.moshi.JsonReader
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
import okio.buffer
import okio.sink
import okio.source
import okio.BufferedSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.concurrent.TimeUnit
import java.nio.charset.StandardCharsets.UTF_8

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
        val localFile = intent.getStringExtra("localFile")

        // Unsubscribe from any previous subscription if needed.
        job?.cancel()
        val handler = CoroutineExceptionHandler { _, exception ->
            XLog.e(exception)
            stopSelf(startId)
            showResultNotification(true, exception.message)
            cancelProgressNotification()
        }
        job = similarServiceScope.launch(handler) {
            updateSimilar(localFile)
        }
        job?.invokeOnCompletion { stopSelf(startId) }

        return START_REDELIVER_INTENT
    }

    /**
     * Method that updates the similar database for manga
     */
    private suspend fun updateSimilar(localFile: String?) = withContext(Dispatchers.IO) {

        // If we do not have a local file, then we should fetch it from network
        // Otherwise try to load it from the local file destination
        // NOTE: the path is a URI if we are loading it locally
        // NOTE: the URI requires us to use android content resolver to stream it
        val listSimilar: List<SimilarFromJson>
        if (localFile == null) {
            val response = SimilarHttpService.create().getSimilarResults().execute()
            if (!response.isSuccessful) {
                throw Exception("Error trying to download similar file")
            }
            val destinationFile = File(filesDir, "neko-similar.json")
            val buffer = destinationFile.sink().buffer()
            // write json to file
            response.body()?.byteStream()?.source()?.use { input ->
                buffer.use { output ->
                    output.writeAll(input)
                }
            }
            val reader = JsonReader.of(destinationFile.source().buffer())
            listSimilar = getSimilar(reader)
            destinationFile.delete()
            reader.close()
        } else {
            if(localFile.substring(localFile.lastIndexOf(".") + 1) != "json") {
                throw Exception("We can only load .json similar database files")
            }
            val localFileUri = Uri.parse(localFile)
            val localFileStream: InputStream? = contentResolver.openInputStream(localFileUri)
            if (localFileStream == null) {
                throw Exception("Unable to open file from disk")
            }
            val source: BufferedSource = localFileStream.source().buffer()
            val reader = JsonReader.of(source)
            listSimilar = getSimilar(reader)
            reader.close()
        }

        // Loop through each and insert into the databas
        val totalManga = listSimilar.size
        val dataToInsert = listSimilar.mapIndexed { index, similarFromJson ->
            showProgressNotification(index, totalManga)
            if (similarFromJson.similarIds.size != similarFromJson.similarTitles.size) {
                return@mapIndexed null
            }
            val similar = MangaSimilarImpl()
            similar.id = index.toLong()
            similar.manga_id = similarFromJson.id.toLong()
            similar.matched_ids = similarFromJson.similarIds.joinToString(MangaSimilarImpl.DELIMITER)
            similar.matched_titles = similarFromJson.similarTitles.joinToString(MangaSimilarImpl.DELIMITER)
            return@mapIndexed similar
        }.filterNotNull()

        // Delete the old similar table, and then insert into the database
        showProgressNotification(dataToInsert.size, totalManga)
        if (dataToInsert.isNotEmpty()) {
            db.deleteAllSimilar().executeAsBlocking()
            db.insertSimilar(dataToInsert).executeAsBlocking()
        }
        showResultNotification(!this.isActive)
        cancelProgressNotification()
    }

    private fun getSimilar(reader: JsonReader): List<SimilarFromJson> {

        var processingManga = false
        var processingTitles = false
        var mangaId: String? = null
        var similarIds = mutableListOf<String>()
        var similarTitles = mutableListOf<String>()
        var similars = mutableListOf<SimilarFromJson>()

        while (reader.peek() != JsonReader.Token.END_DOCUMENT) {
            val nextToken = reader.peek()

            if (JsonReader.Token.BEGIN_OBJECT == nextToken) {
                reader.beginObject()
            } else if (JsonReader.Token.NAME == nextToken) {
                val name = reader.nextName()
                if (!processingManga && name.isDigitsOnly()) {
                    processingManga = true
                    // similar add id
                    mangaId = name
                } else if (name == "m_titles") {
                    processingTitles = true
                }
            } else if (JsonReader.Token.BEGIN_ARRAY == nextToken) {
                reader.beginArray()
            } else if (JsonReader.Token.END_ARRAY == nextToken) {
                reader.endArray()
                if (processingTitles) {
                    processingManga = false
                    processingTitles = false
                    similars.add(SimilarFromJson(mangaId!!, similarIds.toList(), similarTitles.toList()))
                    mangaId = null
                    similarIds = mutableListOf<String>()
                    similarTitles = mutableListOf<String>()
                }
            } else if (JsonReader.Token.NUMBER.equals(nextToken)) {
                similarIds.add(reader.nextInt().toString())
            } else if (JsonReader.Token.STRING.equals(nextToken)) {
                if (processingTitles) {
                    similarTitles.add(reader.nextString())
                }
            } else if (JsonReader.Token.END_OBJECT.equals(nextToken)) {
                reader.endObject()
            }
        }

        return similars
    }

    data class SimilarFromJson(val id: String, val similarIds: List<String>, val similarTitles: List<String>)

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    private fun showProgressNotification(current: Int, total: Int) {
        notificationManager.notify(
            Notifications.ID_SIMILAR_PROGRESS,
            progressNotification
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
    private fun showResultNotification(error: Boolean = false, message: String? = null) {

        val title = if (error) {
            message ?: getString(R.string.similar_loading_complete_error)
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
         * @param localFile URI of the file we want to load locally, or null to get from the network
         */
        fun start(context: Context, localFile: String? = null) {
            if (!isRunning(context)) {
                val intent = Intent(context, SimilarUpdateService::class.java)
                intent.putExtra("localFile", localFile)
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
