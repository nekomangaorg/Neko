package eu.kanade.tachiyomi.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.setThemeByPref
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.nekomanga.R
import org.nekomanga.presentation.screens.CrashScreen
import org.nekomanga.presentation.theme.NekoTheme
import uy.kohesive.injekt.injectLazy

class CrashActivity : AppCompatActivity() {

    private val preferences: PreferencesHelper by injectLazy()

    private val crashReport by lazy { CrashReport.forContext(this) }

    private val isSharing = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeByPref(preferences)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        val reportName = GlobalExceptionHandler.getReportNameFromIntent(intent)
        setContent {
            // Runs after the first composition. If the crash screen crashes while composing, the
            // report stays pending and the next launch offers it.
            LaunchedEffect(Unit) { withIOContext { crashReport.markSeen() } }
            val sharing by isSharing.collectAsStateWithLifecycle()
            NekoTheme {
                CrashScreen(
                    exception = exception,
                    isSharing = sharing,
                    onShareClick = { share(reportName, exception) },
                    onRestartClick = {
                        finishAffinity()
                        startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                    },
                )
            }
        }
    }

    private fun share(reportName: String?, exception: Throwable?) {
        if (isSharing.value) return
        isSharing.value = true
        lifecycleScope.launchIO {
            val report =
                crashReport.report(reportName)
                    ?: exception?.let { crashReport.saveTraceOnly(traceOnlyHead(it)) }
            withUIContext {
                if (report != null) {
                    shareCrashReport(report)
                } else {
                    toast(R.string.crash_log_share_failed)
                }
                isSharing.value = false
            }
        }
    }

    private fun traceOnlyHead(exception: Throwable): String =
        CrashReportText.head(
            debugInfo = CrashReportText.debugInfo(),
            details = mapOf("Note" to "Saved from the crash screen, without logcat"),
            // The intent carries the stack trace as the message of a new Throwable.
            trace = exception.message.orEmpty(),
        )
}
