package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.dpToPxEnd
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import org.nekomanga.R
import org.nekomanga.constants.Constants

class ChapterUtil {
    companion object {

        fun relativeDate(chapter: Chapter): String? {
            return when (chapter.date_upload > 0) {
                true -> chapter.date_upload.timeSpanFromNow
                false -> null
            }
        }

        fun relativeDate(chapterDate: Long): String? {
            return when (chapterDate > 0) {
                true -> chapterDate.timeSpanFromNow
                false -> null
            }
        }

        private fun setBookmark(textView: TextView, chapter: Chapter) {
            if (chapter.bookmark) {
                val context = textView.context
                val drawable =
                    VectorDrawableCompat.create(
                        textView.resources,
                        R.drawable.ic_bookmark_24dp,
                        context.theme,
                    )
                drawable?.setBounds(0, 0, textView.textSize.toInt(), textView.textSize.toInt())
                textView.setCompoundDrawablesRelative(drawable, null, null, null)
                TextViewCompat.setCompoundDrawableTintList(
                    textView,
                    ColorStateList.valueOf(bookmarkedColor(context)),
                )
                textView.compoundDrawablePadding = 3.dpToPx
                textView.translationX = (-2f).dpToPxEnd
            } else {
                textView.setCompoundDrawablesRelative(null, null, null, null)
                textView.translationX = 0f
            }
        }

        fun chapterColor(context: Context, chapter: Chapter, hideStatus: Boolean = false): Int {
            return when {
                hideStatus -> unreadColor(context)
                chapter.read -> readColor(context)
                else -> unreadColor(context)
            }
        }

        fun bookmarkColor(context: Context, chapter: Chapter): Int {
            return when {
                chapter.bookmark -> bookmarkedColor(context)
                else -> readColor(context)
            }
        }

        private fun readColor(context: Context): Int =
            context.contextCompatColor(R.color.read_chapter)

        private fun unreadColor(context: Context): Int =
            context.getResourceColor(R.attr.colorOnBackground)

        private fun bookmarkedColor(context: Context): Int =
            context.getResourceColor(R.attr.colorSecondary)

        fun getScanlators(scanlators: String?): List<String> {
            if (scanlators.isNullOrBlank()) return emptyList()
            return scanlators.split(Constants.SCANLATOR_SEPARATOR).distinct()
        }

        fun getScanlatorString(scanlators: Set<String>): String {
            return scanlators.toList().sorted().joinToString(Constants.SCANLATOR_SEPARATOR)
        }

        fun getLanguages(language: String?): List<String> {
            if (language.isNullOrBlank()) return emptyList()
            return language.split(Constants.SCANLATOR_SEPARATOR).distinct()
        }

        fun getLanguageString(languages: Set<String>): String {
            return languages.toList().sorted().joinToString(Constants.SCANLATOR_SEPARATOR)
        }
    }
}
