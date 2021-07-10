package eu.kanade.tachiyomi.util.log

import android.content.Context
import android.os.Environment
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import java.io.File
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class XLogSetup(context: Context) {

    private val defaultFolder =
        context.getString(R.string.app_name) + when (BuildConfig.DEBUG) {
            true -> "_DEBUG"
            false -> ""
        }

    init {
        XLogLevel.init(context)

        val logLevel = if (XLogLevel.shouldLog(XLogLevel.EXTRA) || BuildConfig.DEBUG) {
            LogLevel.ALL
        } else {
            LogLevel.WARN
        }

        val logConfig = LogConfiguration.Builder()
            .logLevel(logLevel)
            .disableBorder()
            .enableStackTrace(2)
            .tag("||NEKO")
            .build()

        val printers = mutableListOf<Printer>(AndroidPrinter())

        val logFolder = File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                defaultFolder,
            "logs"
        )
        printers += FilePrinter
            .Builder(
                logFolder.absolutePath
            )
            .fileNameGenerator(object : DateFileNameGenerator() {
                override fun generateFileName(logLevel: Int, timestamp: Long): String {
                    return super.generateFileName(
                        logLevel,
                        timestamp
                    ) + "-${BuildConfig.BUILD_TYPE}.txt"
                }
            })
            .cleanStrategy(FileLastModifiedCleanStrategy(Duration.days(1).inWholeMilliseconds))
            .backupStrategy(NeverBackupStrategy())
            .build()

        // Install Crashlytics in prod
        if (!BuildConfig.DEBUG) {
            printers += CrashlyticsPrinter(LogLevel.ERROR)
        }

        XLog.init(
            logConfig,
            *printers.toTypedArray()
        )
    }
}
