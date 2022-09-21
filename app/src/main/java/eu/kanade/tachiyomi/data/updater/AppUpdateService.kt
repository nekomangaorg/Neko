package eu.kanade.tachiyomi.data.updater

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.system.acquireWakeLock
import eu.kanade.tachiyomi.util.system.toast
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import uy.kohesive.injekt.injectLazy

class AppUpdateService : Service() {

    private val network: NetworkHelper by injectLazy()

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var notifier: AppUpdateNotifier

    private var runningJob: Job? = null

    private var runningCall: Call? = null

    override fun onCreate() {
        super.onCreate()
        notifier = AppUpdateNotifier(this)

        startForeground(
            Notifications.ID_UPDATER,
            notifier.onDownloadStarted(getString(R.string.app_name)).build(),
        )

        wakeLock = acquireWakeLock()
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        instance = this

        val handler = CoroutineExceptionHandler { _, exception ->
            XLog.e(exception)
            stopSelf(startId)
        }

        val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
        val title = intent.getStringExtra(EXTRA_DOWNLOAD_TITLE) ?: getString(R.string.app_name)
        val notifyOnInstall = intent.getBooleanExtra(EXTRA_NOTIFY_ON_INSTALL, false)

        runningJob = GlobalScope.launch(handler) {
            downloadApk(title, url, notifyOnInstall)
        }

        runningJob?.invokeOnCompletion { stopSelf(startId) }

        return START_NOT_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        destroyJob()
        return super.stopService(name)
    }

    override fun onDestroy() {
        destroyJob()
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun destroyJob() {
        runningJob?.cancel()
        runningCall?.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    /**
     * Called to start downloading apk of new update
     *
     * @param url url location of file
     */
    private suspend fun downloadApk(title: String, url: String, notifyOnInstall: Boolean) {
        // Show notification download starting.
        notifier.onDownloadStarted(title)

        val progressListener = object : ProgressListener {
            // Progress of the download
            var savedProgress = 0

            // Keep track of the last notification sent to avoid posting too many.
            var lastTick = 0L

            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = (100 * (bytesRead.toFloat() / contentLength)).toInt()
                val currentTime = System.currentTimeMillis()
                if (progress > savedProgress && currentTime - 200 > lastTick) {
                    savedProgress = progress
                    lastTick = currentTime
                    notifier.onProgressChange(progress)
                }
            }
        }

        try {
            // Download the new update.
            val call = network.client.newCallWithProgress(GET(url), progressListener)
            val response = call.await()

            // File where the apk will be saved.
            val apkFile = File(externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body!!.source().saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startInstalling(apkFile, notifyOnInstall)
            } else {
                notifier.onDownloadFinished(apkFile.getUriCompat(this))
            }
        } catch (error: Exception) {
            XLog.e(error)
            if (error is CancellationException ||
                (error is StreamResetException && error.errorCode == ErrorCode.CANCEL)
            ) {
                notifier.cancel()
            } else {
                notifier.onDownloadError(url)
            }
        }
    }

    @RequiresApi(31)
    private fun startInstalling(file: File, notifyOnInstall: Boolean) {
        try {
            val packageInstaller = packageManager.packageInstaller
            val data = file.inputStream()

            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            )
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            session.openWrite("package", 0, -1).use { packageInSession ->
                data.copyTo(packageInSession)
            }
            if (notifyOnInstall) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                prefs.edit {
                    putBoolean(NOTIFY_ON_INSTALL_KEY, true)
                }
            }

            val newIntent = Intent(this, AppUpdateBroadcast::class.java)
                .setAction(PACKAGE_INSTALLED_ACTION)
                .putExtra(EXTRA_NOTIFY_ON_INSTALL, notifyOnInstall)
                .putExtra(EXTRA_FILE_URI, file.getUriCompat(this).toString())

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                -10053,
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            val statusReceiver = pendingIntent.intentSender
            session.commit(statusReceiver)
            data.close()
        } catch (error: Exception) {
            // Either install package can't be found (probably bots) or there's a security exception
            // with the download manager. Nothing we can workaround.
            toast(error.message)
        }
    }

    companion object {

        const val PACKAGE_INSTALLED_ACTION =
            "${BuildConfig.APPLICATION_ID}.SESSION_SELF_API_PACKAGE_INSTALLED"
        internal const val EXTRA_NOTIFY_ON_INSTALL =
            "${BuildConfig.APPLICATION_ID}.UpdaterService.ACTION_ON_INSTALL"
        internal const val EXTRA_DOWNLOAD_URL =
            "${BuildConfig.APPLICATION_ID}.UpdaterService.DOWNLOAD_URL"
        internal const val EXTRA_FILE_URI = "${BuildConfig.APPLICATION_ID}.UpdaterService.FILE_URI"
        internal const val EXTRA_DOWNLOAD_TITLE =
            "${BuildConfig.APPLICATION_ID}.UpdaterService.DOWNLOAD_TITLE"

        internal const val NOTIFY_ON_INSTALL_KEY = "notify_on_install_complete"

        private var instance: AppUpdateService? = null

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(): Boolean = instance != null

        /**
         * Downloads a new update and let the user install the new version from a notification.
         * @param context the application context.
         * @param url the url to the new update.
         */
        fun start(context: Context, url: String, notifyOnInstall: Boolean) {
            if (!isRunning()) {
                val title = context.getString(R.string.app_name)
                val intent = Intent(context, AppUpdateService::class.java).apply {
                    putExtra(EXTRA_DOWNLOAD_TITLE, title)
                    putExtra(EXTRA_DOWNLOAD_URL, url)
                    putExtra(EXTRA_NOTIFY_ON_INSTALL, notifyOnInstall)
                }
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
            context.stopService(Intent(context, AppUpdateService::class.java))
        }

        /**
         * Returns [PendingIntent] that starts a service which downloads the apk specified in url.
         *
         * @param url the url to the new update.
         * @return [PendingIntent]
         */
        internal fun downloadApkPendingService(
            context: Context,
            url: String,
            notifyOnInstall: Boolean = false,
        ): PendingIntent {
            val intent = Intent(context, AppUpdateService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, url)
                putExtra(EXTRA_NOTIFY_ON_INSTALL, notifyOnInstall)
            }
            return PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
