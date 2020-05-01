package eu.kanade.tachiyomi.util.system

import android.text.format.DateUtils

val Long.timeSpanFromNow: String
    get() = DateUtils.getRelativeTimeSpanString(this).toString()
