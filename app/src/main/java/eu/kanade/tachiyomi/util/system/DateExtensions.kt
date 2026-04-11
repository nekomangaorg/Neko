package eu.kanade.tachiyomi.util.system

import android.text.format.DateUtils
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

fun Date.toTimestampString(dateFormatter: DateFormat): String {
    val date = dateFormatter.format(this)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
    return "$date $time"
}

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

fun Long.toLocalDate(): LocalDate {
    return LocalDate.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

fun Instant.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return LocalDate.ofInstant(this, zoneId)
}
