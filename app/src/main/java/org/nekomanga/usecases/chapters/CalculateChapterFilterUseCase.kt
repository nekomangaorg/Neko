package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.ui.manga.MangaConstants

class CalculateChapterFilterUseCase {

    operator fun invoke(
        currentFilter: MangaConstants.ChapterDisplay,
        option: MangaConstants.ChapterDisplayOptions,
    ): MangaConstants.ChapterDisplay {
        return when (option.displayType) {
            MangaConstants.ChapterDisplayType.All -> MangaConstants.ChapterDisplay(showAll = true)
            MangaConstants.ChapterDisplayType.Unread ->
                currentFilter.copy(showAll = false, unread = option.displayState)
            MangaConstants.ChapterDisplayType.Bookmarked ->
                currentFilter.copy(showAll = false, bookmarked = option.displayState)
            MangaConstants.ChapterDisplayType.Downloaded ->
                currentFilter.copy(showAll = false, downloaded = option.displayState)
            MangaConstants.ChapterDisplayType.HideTitles ->
                currentFilter.copy(hideChapterTitles = option.displayState)
            MangaConstants.ChapterDisplayType.Available ->
                currentFilter.copy(showAll = false, available = option.displayState)
        }
    }
}
