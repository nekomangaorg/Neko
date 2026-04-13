package org.nekomanga.usecases.chapters

import android.content.Context
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.R

class GetChapterFilterText(private val context: Context) {
    operator fun invoke(
        chapterDisplay: MangaConstants.ChapterDisplay,
        chapterSourceFilter: MangaConstants.ScanlatorFilter,
        chapterScanlatorFilter: MangaConstants.ScanlatorFilter,
        languageFilter: MangaConstants.LanguageFilter,
    ): String {
        val filters = buildList {
            if (chapterDisplay.unread == ToggleableState.Indeterminate) add(R.string.read)
            if (chapterDisplay.unread == ToggleableState.On) add(R.string.unread)
            if (chapterDisplay.downloaded == ToggleableState.On) add(R.string.downloaded)
            if (chapterDisplay.downloaded == ToggleableState.Indeterminate)
                add(R.string.not_downloaded)
            if (chapterDisplay.bookmarked == ToggleableState.On) add(R.string.bookmarked)
            if (chapterDisplay.bookmarked == ToggleableState.Indeterminate)
                add(R.string.not_bookmarked)
            if (chapterDisplay.available == ToggleableState.On) add(R.string.available)
            if (chapterDisplay.available == ToggleableState.Indeterminate) add(R.string.unavailable)
            if (languageFilter.languages.any { it.disabled }) add(R.string.language)
            if (chapterScanlatorFilter.scanlators.any { it.disabled }) add(R.string.scanlators)
            if (chapterSourceFilter.scanlators.any { it.disabled }) add(R.string.sources)
        }

        return filters.joinToString(", ") { context.getString(it) }
    }
}
