package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants

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

        /**
         * returns true for a list filter, if the source name exists in the filtered sources, and
         * the chapter has the scanlator
         */
        fun filteredBySource(
            sourceName: String,
            scanlatorStr: String,
            isMerged: Boolean,
            isLocal: Boolean,
            filteredGroups: Set<String>,
        ): Boolean {
            if (filteredGroups.isEmpty()) {
                return false
            }

            val shouldCheck = sourceName in filteredGroups
            if (!shouldCheck) {
                return false
            }

            if (sourceName == MdConstants.name && !isLocal) {
                return !isMerged
            }

            if (sourceName == Constants.LOCAL_SOURCE) {
                return isLocal
            }

            // at this point if the chapter is not merged and the source is Not MangaDex or Local
            // then we
            // already
            // know its not a matching chapter
            if (!isMerged) {
                return false
            }

            return getScanlators(scanlatorStr).any { group -> group == sourceName }
        }

        /**
         * returns true for a list filter, if the language of the chapter exists in the filtered
         * language set
         */
        fun filterByLanguage(languageStr: String, filteredLanguages: Set<String>): Boolean {
            return when {
                filteredLanguages.isEmpty() -> false
                else -> {
                    getLanguages(languageStr).any { language -> language in filteredLanguages }
                }
            }
        }

        /**
         * returns true for a list filter, if the group of the chapter exists in the group set and
         * the any filter is used. if the all filter is used then all the groups for the chapter
         * need to be in the group set.
         */
        fun filterByGroup(
            scanlatorStr: String,
            uploaderStr: String,
            all: Boolean,
            filteredGroups: Set<String>,
        ): Boolean {
            val scanlators =
                getScanlators(scanlatorStr)
                    .filterNot { it in SourceManager.mergeSourceNames }
                    .toMutableList()
            // Match all should ignore No Group if uploader is filtered
            if (all && uploaderStr.isNotEmpty()) {
                if ("No Group" !in filteredGroups) {
                    scanlators.remove("No Group")
                }
                scanlators.add(uploaderStr)
            }
            return when {
                scanlators.isEmpty() || filteredGroups.isEmpty() -> false
                all -> scanlators.all { group -> group in filteredGroups }
                else -> scanlators.any { group -> group in filteredGroups }
            }
        }
    }
}
