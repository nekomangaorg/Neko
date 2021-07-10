package eu.kanade.tachiyomi.data.backup

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.backup.full.FullRestore
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.isServiceRunning
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Restores backup.
 */
class BackupRestoreService : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    private val restoreHelper = RestoreHelper(this)

    /**
     * Subscription where the update is done.
     */
    private var job: Job? = null

    /**
     * Method called when the service is created. It injects dependencies and acquire the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(
            Notifications.ID_RESTORE_PROGRESS,
            restoreHelper.progressNotification.build()
        )
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BackupRestoreService:WakeLock"
        )
        wakeLock.acquire(TimeUnit.HOURS.toMillis(3))
    }

    /**
     * Method called when the service is destroyed. It destroys the running subscription and
     * releases the wake lock.
     */
    override fun onDestroy() {
        job?.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent): IBinder? = null

    /**
     * Method called when the service receives an intent.
     *
     * @param intent the start intent from.
     * @param flags the flags of the command.
     * @param startId the start id of this command.
     * @return the start value of the command.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.getParcelableExtra<Uri>(BackupConst.EXTRA_URI) ?: return START_NOT_STICKY
        val mode = intent.getIntExtra(BackupConst.EXTRA_MODE, BackupConst.BACKUP_TYPE_FULL)

        // Unsubscribe from any previous subscription if needed.
        job?.cancel()
        val handler = CoroutineExceptionHandler { _, exception ->
            XLog.e(exception)
            restoreHelper.showErrorNotification(exception.message!!)
            stopSelf(startId)
        }
        job = GlobalScope.launch(handler) {
            when {
                mode == BackupConst.BACKUP_TYPE_FULL -> FullRestore(
                    this@BackupRestoreService,
                    job
                ).restoreBackup(uri)
                // else -> LegacyRestore(this@BackupRestoreService, job).restoreBackup(uri)
            }
        }
        job?.invokeOnCompletion { stopSelf(startId) }

        return START_NOT_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        job?.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        return super.stopService(name)
    }

    companion object {

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(BackupRestoreService::class.java)

        /**
         * Starts a service to restore a backup from Json
         *
         * @param context context of application
         * @param uri path of Uri
         */
        fun start(context: Context, uri: Uri, mode: Int, isOnline: Boolean) {
            if (!isRunning(context)) {
                val intent = Intent(context, BackupRestoreService::class.java).apply {
                    putExtra(BackupConst.EXTRA_URI, uri)
                    putExtra(BackupConst.EXTRA_MODE, mode)
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }

        /**
         * Stops the service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, BackupRestoreService::class.java))
        }
    }
}
