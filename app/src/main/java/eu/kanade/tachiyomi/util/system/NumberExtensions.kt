package eu.kanade.tachiyomi.util.system

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

fun Double.roundToTwoDecimal(): Double {
    return (this * 100.0).roundToLong() / 100.0
}

/**
 * Formats a chapter number for chapter_txt with up to three decimals, always with ASCII digits and
 * a '.' separator. getChapterNum parses chapter_txt back and reads only those characters, so the
 * default locale's symbols ("7,5", Arabic-Indic digits) would break chapter sorting.
 */
fun Float.formatChapterNumber(): String {
    val format = DecimalFormat("0.###", DecimalFormatSymbols(Locale.US))
    format.isGroupingUsed = false
    return format.format(toBigDecimal())
}
