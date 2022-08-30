package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.text.format.DateUtils
import eu.kanade.tachiyomi.R
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

val Long.timeSpanFromNow: String
    get() = DateUtils.getRelativeTimeSpanString(this).toString()

fun Long.timeSpanFromNow(context: Context): String {
    return if (this == 0L) {
        context.getString(R.string.a_while_ago).lowercase(Locale.ROOT)
    } else {
        DateUtils.getRelativeTimeSpanString(this).toString()
    }
}

fun Context.timeSpanFromNow(res: Int, time: Long) = getString(res, time.timeSpanFromNow(this))

/**
 * Convert local time millisecond value to Calendar instance in UTC
 *
 * @return UTC Calendar instance at supplied time. Null if time is 0.
 */
fun Long.toUtcCalendar(): Calendar? {
    if (this == 0L) {
        return null
    }
    val rawCalendar = Calendar.getInstance().apply {
        timeInMillis = this@toUtcCalendar
    }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            rawCalendar.get(Calendar.YEAR),
            rawCalendar.get(Calendar.MONTH),
            rawCalendar.get(Calendar.DAY_OF_MONTH),
            rawCalendar.get(Calendar.HOUR_OF_DAY),
            rawCalendar.get(Calendar.MINUTE),
            rawCalendar.get(Calendar.SECOND),
        )
    }
}

/**
 * Convert UTC time millisecond to Calendar instance in local time zone
 *
 * @return local Calendar instance at supplied UTC time. Null if time is 0.
 */
fun Long.toLocalCalendar(): Calendar? {
    if (this == 0L) {
        return null
    }
    val rawCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = this@toLocalCalendar
    }
    return Calendar.getInstance().apply {
        clear()
        set(
            rawCalendar.get(Calendar.YEAR),
            rawCalendar.get(Calendar.MONTH),
            rawCalendar.get(Calendar.DAY_OF_MONTH),
            rawCalendar.get(Calendar.HOUR_OF_DAY),
            rawCalendar.get(Calendar.MINUTE),
            rawCalendar.get(Calendar.SECOND),
        )
    }
}
