package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import android.text.format.DateUtils
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.util.system.contextCompatColor
import java.util.Date

class ChapterUtil {
    companion object {

        fun relativeDate(chapter: Chapter): String? {
            return when (chapter.date_upload > 0) {
                true -> DateUtils.getRelativeTimeSpanString(chapter.date_upload, Date().time, DateUtils.HOUR_IN_MILLIS).toString()
                false -> null
            }
        }

        fun chapterColor(context: Context, chapter: Chapter, hideStatus: Boolean = false): Int {
            return when {
                hideStatus -> unreadColor(context)
                chapter.bookmark && chapter.read -> bookmarkedAndReadColor(context)
                chapter.bookmark -> bookmarkedColor(context)
                chapter.read -> readColor(context)
                else -> unreadColor(context)
            }
        }

        fun readColor(context: Context, chapter: Chapter): Int {
            return when {
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

        private fun readColor(context: Context): Int = context.contextCompatColor(R.color.read_chapter)

        private fun unreadColor(context: Context): Int = context.contextCompatColor(R.color.unread_chapter)

        private fun bookmarkedColor(context: Context): Int = context.contextCompatColor(R.color.bookmarked_chapter)

        private fun bookmarkedAndReadColor(context: Context): Int = ColorUtils.setAlphaComponent(context.contextCompatColor(R.color.bookmarked_chapter), 150)
    }
}
