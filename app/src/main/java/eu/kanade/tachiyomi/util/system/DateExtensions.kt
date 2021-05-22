package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.text.format.DateUtils
import eu.kanade.tachiyomi.R
import java.util.Locale

val Long.timeSpanFromNow: String
    get() = DateUtils.getRelativeTimeSpanString(this).toString()

fun Long.timeSpanFromNow(context: Context): String {
    return if (this == 0L) {
        context.getString(R.string.a_while_ago).lowercase(Locale.ROOT)
    } else {
        DateUtils.getRelativeTimeSpanString(this).toString()
    }
}
