package eu.kanade.tachiyomi.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R

fun NotificationCompat.Builder.customOngoing(
    context: Context,
    title: String,
    smallIcon: Int
): NotificationCompat.Builder {
    setContentTitle(title)
    setColor(ContextCompat.getColor(context, R.color.colorPrimary))
    setSmallIcon(smallIcon)
    setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
    setOnlyAlertOnce(true)
    return this
}
