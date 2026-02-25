package eu.kanade.tachiyomi.data.updater

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.system.connectivityManager
import eu.kanade.tachiyomi.util.system.jobIsRunning
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.tryToSetForeground
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withUIContext
import java.io.File
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import org.nekomanga.BuildConfig
import org.nekomanga.core.network.GET
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.ProgressListener
import tachiyomi.core.network.await
import tachiyomi.core.network.newCachelessCallWithProgress
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class AppDownloadInstallJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val notifier = AppUpdateNotifier(context)
    private val network: NetworkHelper by injectLazy()
    private var runningCall: Call? = null
    val preferences = Injekt.get<PreferencesHelper>()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notifier.onDownloadStarted().build()
        val id = Notifications.ID_UPDATER
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    override suspend fun doWork(): Result {
        tryToSetForeground()
        val idleRun = inputData.getBoolean(IDLE_RUN, false)
        val url: String
        val version: String
        if (idleRun) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                return Result.failure()
            }
            if (
                preferences.appShouldAutoUpdate().get() == ONLY_ON_UNMETERED &&
                    context.connectivityManager.isActiveNetworkMetered
            ) {
                return Result.retry()
            }

            val result = withIOContext {
                AppUpdateChecker()
                    .checkForUpdate(isUserPrompt = true, doExtrasAfterNewUpdate = false)
            }
            if (result is AppUpdateResult.NewUpdate) {
                AppUpdateNotifier(context).cancel()
                AppUpdateNotifier.releasePageUrl = result.release.releaseLink
                url = result.release.downloadLink
                version = result.release.version
            } else {
                return Result.success()
            }
        } else {
            url = inputData.getString(EXTRA_DOWNLOAD_URL) ?: return Result.failure()
            version = inputData.getString(EXTRA_VERSION) ?: return Result.failure()
        }

        // Persist the version being downloaded so it survives process death and can be
        // checked by start() when the user tries to trigger another download of the same version.
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(DOWNLOADING_VERSION_KEY, version)
        }

        instance = WeakReference(this)

        val notifyOnInstall = inputData.getBoolean(EXTRA_NOTIFY_ON_INSTALL, false)

        withIOContext { downloadApk(url, notifyOnInstall, version) }

        // Clear the downloading version now that the job has finished (success or handled error).
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            remove(DOWNLOADING_VERSION_KEY)
        }

        runningCall?.cancel()
        instance = null
        return Result.success()
    }

    /**
     * Called to start downloading apk of new update
     *
     * @param url url location of file
     */
    private suspend fun downloadApk(url: String, notifyOnInstall: Boolean, version: String) =
        coroutineScope {
            val progressListener =
                object : ProgressListener {
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
                val call = network.client.newCachelessCallWithProgress(GET(url), progressListener)
                runningCall = call
                val response = call.await()
                if (isStopped) {
                    cancel()
                    return@coroutineScope
                }

                // File where the apk will be saved.
                val apkFile = File(context.externalCacheDir, "update.apk")

                if (response.isSuccessful) {
                    response.body.source().saveTo(apkFile)
                } else {
                    response.close()
                    throw Exception("Unsuccessful response")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startInstalling(apkFile, notifyOnInstall)
                } else {
                    notifier.onDownloadFinished(apkFile.getUriCompat(context))
                }
            } catch (error: Exception) {
                TimberKt.e(error)
                if (
                    error is CancellationException ||
                        isStopped ||
                        (error is StreamResetException && error.errorCode == ErrorCode.CANCEL)
                ) {
                    notifier.cancel()
                } else {
                    notifier.onDownloadError(url, version)
                }
            }
        }

    @RequiresApi(31)
    private suspend fun startInstalling(file: File, notifyOnInstall: Boolean) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val data = file.inputStream()

            val params =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            session.openWrite("package", 0, -1).use { packageInSession ->
                data.copyTo(packageInSession)
            }
            if (notifyOnInstall) {
                PreferenceManager.getDefaultSharedPreferences(context).edit {
                    putBoolean(NOTIFY_ON_INSTALL_KEY, true)
                }
            }

            val newIntent =
                Intent(context, AppUpdateBroadcast::class.java)
                    .setAction(PACKAGE_INSTALLED_ACTION)
                    .putExtra(EXTRA_NOTIFY_ON_INSTALL, notifyOnInstall)
                    .putExtra(EXTRA_FILE_URI, file.getUriCompat(context).toString())

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    -10053,
                    newIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
            val statusReceiver = pendingIntent.intentSender
            session.commit(statusReceiver)
            notifier.onInstalling()

            withContext(Dispatchers.IO) { data.close() }
            withUIContext {
                delay(5000)
                val hasNotification =
                    context.notificationManager.activeNotifications.any {
                        it.id == Notifications.ID_UPDATER
                    }
                // If the package manager crashes for whatever reason (china phone)
                // set a timeout and let the user manually install
                if (packageInstaller.getSessionInfo(sessionId) == null && !hasNotification) {
                    notifier.cancelInstallNotification()
                    notifier.onDownloadFinished(file.getUriCompat(context))
                    PreferenceManager.getDefaultSharedPreferences(context).edit {
                        remove(NOTIFY_ON_INSTALL_KEY)
                    }
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Either install package can't be found (probably bots) or there's a security exception
            // with the download manager. Nothing we can workaround.
            withContext(Dispatchers.Main) { context.toast(error.message) }
            notifier.cancelInstallNotification()
            notifier.onDownloadFinished(file.getUriCompat(context))
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                remove(NOTIFY_ON_INSTALL_KEY)
            }
        }
    }

    companion object {
        private const val TAG = "AppDownloadInstaller"
        const val PACKAGE_INSTALLED_ACTION =
            "${BuildConfig.APPLICATION_ID}.SESSION_SELF_API_PACKAGE_INSTALLED"
        internal const val EXTRA_FILE_URI = "${BuildConfig.APPLICATION_ID}.AppInstaller.FILE_URI"
        internal const val EXTRA_NOTIFY_ON_INSTALL = "ACTION_ON_INSTALL"
        internal const val EXTRA_DOWNLOAD_URL = "DOWNLOAD_URL"
        internal const val EXTRA_VERSION = "DOWNLOAD_VERSION"
        internal const val NOTIFY_ON_INSTALL_KEY = "notify_on_install_complete"
        internal const val DOWNLOADING_VERSION_KEY = "downloading_version"
        private const val IDLE_RUN = "idle_run"

        const val ALWAYS = 0
        const val ONLY_ON_UNMETERED = 1
        const val NEVER = 2

        private var instance: WeakReference<AppDownloadInstallJob>? = null

        fun start(
            context: Context,
            url: String?,
            notifyOnInstall: Boolean,
            waitUntilIdle: Boolean = false,
            version: String,
        ) {
            // Idempotency guard: if the exact same version is already downloading, do nothing.
            // If a *different* (newer) version is requested we fall through and WorkManager's
            // REPLACE policy will cancel the old job and start a fresh one.
            if (isRunning(context)) {
                val downloadingVersion =
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(DOWNLOADING_VERSION_KEY, null)
                if (downloadingVersion != null && downloadingVersion == version) {
                    return
                }
            }

            val data = Data.Builder()
            data.putString(EXTRA_DOWNLOAD_URL, url)
            data.putBoolean(EXTRA_NOTIFY_ON_INSTALL, notifyOnInstall)
            data.putString(EXTRA_VERSION, version)
            val request =
                OneTimeWorkRequestBuilder<AppDownloadInstallJob>()
                    .addTag(TAG)
                    .apply {
                        if (waitUntilIdle) {
                            data.putBoolean(IDLE_RUN, true)
                            val shouldAutoUpdate =
                                Injekt.get<PreferencesHelper>().appShouldAutoUpdate().get()
                            val constraints =
                                Constraints.Builder()
                                    .setRequiredNetworkType(
                                        if (shouldAutoUpdate == ALWAYS) {
                                            NetworkType.CONNECTED
                                        } else {
                                            NetworkType.UNMETERED
                                        }
                                    )
                                    .setRequiresDeviceIdle(true)
                                    .build()
                            setConstraints(constraints)
                        } else {
                            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        }
                        setInputData(data.build())
                    }
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            instance?.get()?.runningCall?.cancel()
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
            // Clear the persisted version so a fresh start is allowed after an explicit stop.
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                remove(DOWNLOADING_VERSION_KEY)
            }
        }

        fun isRunning(context: Context) = WorkManager.getInstance(context).jobIsRunning(TAG)
    }
}
