package eu.kanade.tachiyomi.crash

import android.content.Context
import android.content.Intent
import android.os.Process
import java.io.File
import kotlin.system.exitProcess
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.nekomanga.logging.TimberKt

class GlobalExceptionHandler
private constructor(
    private val applicationContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler,
    private val activityToBeLaunched: Class<*>,
    private val processName: String,
    private val activityTracker: VisibleActivityTracker,
) : Thread.UncaughtExceptionHandler {

    object ThrowableSerializer : KSerializer<Throwable> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Throwable", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Throwable =
            Throwable(message = decoder.decodeString())

        override fun serialize(encoder: Encoder, value: Throwable) =
            encoder.encodeString(value.stackTraceToString())
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        // A second thread that crashes waits here, so it cannot replace the first report.
        synchronized(this) {
            val route =
                crashRoute(isErrorHandlerProcess(processName), activityTracker.isActivityVisible)
            if (route != CrashRoute.DefaultHandlerWithoutReport) {
                try {
                    TimberKt.e(exception) { "Uncaught Exception" }
                } catch (_: Throwable) {}
                val report = saveReport(thread, exception)
                if (route == CrashRoute.CrashScreen) {
                    try {
                        launchActivity(applicationContext, activityToBeLaunched, exception, report)
                        exitProcess(0)
                    } catch (_: Throwable) {}
                }
            }
            defaultHandler.uncaughtException(thread, exception)
        }
    }

    private fun saveReport(thread: Thread, exception: Throwable): File? =
        try {
            val head =
                CrashReportText.head(
                    debugInfo = CrashReportText.debugInfo(),
                    details =
                        mapOf(
                            "Crash time" to CrashReportText.formatTime(System.currentTimeMillis()),
                            "Process" to processName,
                            "PID" to Process.myPid().toString(),
                            "Thread" to thread.name,
                        ),
                    trace = exception.stackTraceToString(),
                )
            CrashReport.forContext(applicationContext).saveCrash(head)
        } catch (_: Throwable) {
            null
        }

    private fun launchActivity(
        applicationContext: Context,
        activity: Class<*>,
        exception: Throwable,
        report: File?,
    ) {
        val intent =
            Intent(applicationContext, activity).apply {
                putExtra(INTENT_EXTRA, Json.encodeToString(ThrowableSerializer, exception))
                putExtra(REPORT_EXTRA, report?.name)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        applicationContext.startActivity(intent)
    }

    companion object {
        private const val INTENT_EXTRA = "Throwable"
        private const val REPORT_EXTRA = "CrashReport"

        fun initialize(
            applicationContext: Context,
            activityToBeLaunched: Class<*>,
            processName: String,
            activityTracker: VisibleActivityTracker,
        ) {
            val handler =
                GlobalExceptionHandler(
                    applicationContext,
                    Thread.getDefaultUncaughtExceptionHandler() as Thread.UncaughtExceptionHandler,
                    activityToBeLaunched,
                    processName,
                    activityTracker,
                )
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }

        fun getThrowableFromIntent(intent: Intent): Throwable? {
            return try {
                Json.decodeFromString(ThrowableSerializer, intent.getStringExtra(INTENT_EXTRA)!!)
            } catch (e: Exception) {
                TimberKt.e(e) { "Wasn't able to retrive throwable from intent" }
                null
            }
        }

        /** The file name of the report the crash handler saved, or null if it saved none. */
        fun getReportNameFromIntent(intent: Intent): String? = intent.getStringExtra(REPORT_EXTRA)
    }
}
