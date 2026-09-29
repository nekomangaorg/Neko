package eu.kanade.tachiyomi.crash

import android.content.ClipData
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.toast
import java.io.File
import org.nekomanga.R
import org.nekomanga.logging.TimberKt

/** Opens the share sheet for [report], or shows a toast if that fails. Call on the main thread. */
fun Context.shareCrashReport(report: File) {
    try {
        val uri = report.getUriCompat(this)
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(null, uri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        val chooser =
            Intent.createChooser(shareIntent, getString(R.string.share)).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = shareIntent.clipData
            }
        startActivity(chooser)
    } catch (e: Exception) {
        TimberKt.e(e) { "Could not share the crash report" }
        toast(R.string.crash_log_share_failed)
    }
}
