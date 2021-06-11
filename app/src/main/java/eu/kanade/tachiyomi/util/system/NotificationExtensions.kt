package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R

fun NotificationCompat.Builder.customize(
    context: Context,
    title: String,
    smallIcon: Int,
    ongoing: Boolean = false
): NotificationCompat.Builder {
    setContentTitle(title)
    setSmallIcon(smallIcon)
    color = context.contextCompatColor(R.color.neko_green_darker)
    if (ongoing) {
        setOngoing(true)
    }
    setOnlyAlertOnce(true)
    return this
}
