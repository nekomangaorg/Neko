package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.ui.library.LibraryFilters
import org.nekomanga.domain.manga.LibraryMangaItem

class FilterLibraryMangaUseCase {
    operator fun invoke(manga: LibraryMangaItem, filters: LibraryFilters): Boolean {
        // Check Unread first (most common filter)
        if (!filters.filterUnread.matches(manga)) return false

        // Check Downloaded second (common and quick)
        if (!filters.filterDownloaded.matches(manga)) return false

        // Check the rest
        if (!filters.filterBookmarked.matches(manga)) return false
        if (!filters.filterCompleted.matches(manga)) return false
        if (!filters.filterMangaType.matches(manga)) return false
        if (!filters.filterMerged.matches(manga)) return false
        if (!filters.filterUnavailable.matches(manga)) return false
        if (!filters.filterTracked.matches(manga)) return false

        return true // passed all checks
    }
}
