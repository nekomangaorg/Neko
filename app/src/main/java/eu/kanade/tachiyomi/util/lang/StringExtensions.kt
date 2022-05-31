package eu.kanade.tachiyomi.util.lang

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor
import java.util.Locale
import kotlin.math.floor

/**
 * Replaces the given string to have at most [count] characters using [replacement] at its end.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.chop(count: Int, replacement: String = "…"): String {
    return if (length > count) {
        take(count - replacement.length) + replacement
    } else {
        this
    }
}

fun String.chopByWords(count: Int): String {
    return if (length > count) {
        val splitWords = split(" ")
        val iterator = splitWords.iterator()
        var newString = iterator.next()
        return if (newString.length > count) {
            chop(count)
        } else {
            var next = iterator.next()
            while ("$newString $next".length <= count) {
                newString = "$newString $next"
                next = iterator.next()
            }
            newString
        }
    } else {
        this
    }
}

fun String.removeArticles(): String {
    return when {
        startsWith("a ", true) -> substring(2)
        startsWith("an ", true) -> substring(3)
        startsWith("the ", true) -> substring(4)
        else -> this
    }
}

val String.sqLite: String
    get() = replace("'", "''")

fun String.trimOrNull(): String? {
    val trimmed = trim()
    return if (trimmed.isBlank()) null else trimmed
}

/**
 * Replaces the given string to have at most [count] characters using [replacement] near the center.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.truncateCenter(count: Int, replacement: String = "..."): String {
    if (length <= count) {
        return this
    }

    val pieceLength: Int = floor((count - replacement.length).div(2.0)).toInt()

    return "${take(pieceLength)}$replacement${takeLast(pieceLength)}"
}

fun String.capitalizeWords(): String {
    val firstReplace = split(" ").joinToString(" ") {
        it.replaceFirstChar { text ->
            text.titlecase(Locale.getDefault())
        }
    }
    return firstReplace.split("-").joinToString("-") {
        it.replaceFirstChar { text ->
            text.titlecase(Locale.getDefault())
        }
    }
}

/**
 * Case-insensitive natural comparator for strings.
 */
fun String.compareToCaseInsensitiveNaturalOrder(other: String): Int {
    return String.CASE_INSENSITIVE_ORDER.then(naturalOrder()).compare(this, other)
}

fun String.capitalized(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(Locale.US)
        } else it.toString()
    }
}

fun CharSequence.tintText(@ColorInt color: Int): Spanned {
    val s = SpannableString(this)
    s.setSpan(ForegroundColorSpan(color), 0, this.length, 0)
    return s
}

fun String.highlightText(highlight: String, @ColorInt color: Int): Spanned {
    val wordToSpan: Spannable = SpannableString(this)
    if (highlight.isBlank()) return wordToSpan
    indexesOf(highlight).forEach {
        wordToSpan.setSpan(
            BackgroundColorSpan(color),
            it,
            it + highlight.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    return wordToSpan
}

fun String.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    val list = mutableListOf<Int>()
    if (substr.isBlank()) return list

    var i = -1
    while (true) {
        i = indexOf(substr, i + 1, ignoreCase)
        when (i) {
            -1 -> return list
            else -> list.add(i)
        }
    }
}

fun String.withSubtitle(context: Context, @StringRes subtitleRes: Int) =
    withSubtitle(context, context.getString(subtitleRes))

fun String.withSubtitle(context: Context, subtitle: String): Spanned {
    val spannable = SpannableStringBuilder(this + "\n" + subtitle)
    spannable.setSpan(
        ForegroundColorSpan(context.getResourceColor(android.R.attr.textColorSecondary)),
        this.length + 1,
        spannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return spannable
}

fun String.addBetaTag(context: Context): Spanned {
    val betaText = context.getString(R.string.beta)
    val betaSpan = SpannableStringBuilder(this + betaText)
    betaSpan.setSpan(
        SuperscriptSpan(),
        length,
        length + betaText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    betaSpan.setSpan(
        RelativeSizeSpan(0.75f),
        length,
        length + betaText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    betaSpan.setSpan(
        StyleSpan(Typeface.BOLD),
        length,
        length + betaText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    betaSpan.setSpan(
        ForegroundColorSpan(context.getResourceColor(R.attr.colorSecondary)),
        length,
        length + betaText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return betaSpan
}

private val uuidFormatLines = arrayOf(8, 13, 18, 23)
private val uuidFormatDigits = arrayOf((0..7), (9..12), (14..17), (19..22), (24..35))

/**
 * Check if a string is in UUID format.
 */
fun String.isUUID() =
    this.length == 36 &&
        uuidFormatLines.all { idx -> this[idx] == '-' } &&
        uuidFormatDigits.all { range ->
            range.all { idx ->
                this[idx].let { char -> char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F' }
            }
        }

/**
 * HTML-decode the string
 */
fun String.htmlDecode(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        Html.fromHtml(this).toString()
    }
}
