package eu.kanade.tachiyomi.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R

fun NotificationCompat.Builder.customize(
    context: Context,
    title: String,
    smallIcon: Int,
    ongoing: Boolean = false
): NotificationCompat.Builder {

    setContentTitle(title)
    setSmallIcon(smallIcon)
    setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
    if (ongoing) {
        setOngoing(true)
    }
    setColor(ContextCompat.getColor(context, R.color.colorPrimary))
    setOnlyAlertOnce(true)
    return this
}
