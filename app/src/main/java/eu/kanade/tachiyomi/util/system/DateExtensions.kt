package eu.kanade.tachiyomi.util.system

import android.text.format.DateUtils
import java.text.ParseException
import java.text.SimpleDateFormat

val Long.timeSpanFromNow: String
    get() = DateUtils.getRelativeTimeSpanString(this).toString()

fun SimpleDateFormat.tryParse(date: String?): Long {
    return try {
        when (date == null) {
            true -> 0L
            false -> this.parse(date)!!.time
        }
    } catch (_: ParseException) {
        0L
    }
}
