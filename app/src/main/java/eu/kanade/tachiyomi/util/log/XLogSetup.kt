package eu.kanade.tachiyomi.util.log

import android.content.Context
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
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.days

@ExperimentalTime
class XLogSetup(context: Context) {
    init {
        XLogLevel.init(context)

        val logLevel = if (XLogLevel.shouldLog(XLogLevel.EXTRA)) {
            LogLevel.ALL
        } else {
            LogLevel.WARN
        }

        val logConfig = LogConfiguration.Builder()
                .logLevel(logLevel)
                .t()
                .st(2)
                .nb()
                .build()

        val printers = mutableListOf<Printer>(AndroidPrinter())

        val logFolder = File(
                context.filesDir,
                "logs"
        )

        printers += FilePrinter
                .Builder(logFolder.absolutePath)
                .fileNameGenerator(object : DateFileNameGenerator() {
                    override fun generateFileName(logLevel: Int, timestamp: Long): String {
                        return super.generateFileName(logLevel, timestamp) + "-${BuildConfig.BUILD_TYPE}"
                    }
                })
                .cleanStrategy(FileLastModifiedCleanStrategy(7.days.inMilliseconds.toLong()))
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

        XLog.d("Application booting...")

    }
}