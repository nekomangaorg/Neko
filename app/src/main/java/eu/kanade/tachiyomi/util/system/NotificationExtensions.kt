package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.core.app.NotificationCompat
import org.nekomanga.R

fun NotificationCompat.Builder.customize(
    context: Context,
    title: String,
    smallIcon: Int,
    ongoing: Boolean = false,
): NotificationCompat.Builder {
    setContentTitle(title)
    setSmallIcon(smallIcon)
    color = context.contextCompatColor(R.color.iconOutline)
    if (ongoing) {
        setOngoing(true)
    }
    setOnlyAlertOnce(true)
    return this
}
