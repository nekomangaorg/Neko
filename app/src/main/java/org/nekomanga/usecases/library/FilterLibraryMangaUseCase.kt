package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.ui.library.LibraryFilters
import org.nekomanga.domain.manga.LibraryMangaItem

class FilterLibraryMangaUseCase {
    operator fun invoke(manga: LibraryMangaItem, filters: LibraryFilters): Boolean {
        // The order of checks is intentional for performance, with more common filters first.
        return filters.filterUnread.matches(manga) &&
            filters.filterDownloaded.matches(manga) &&
            filters.filterBookmarked.matches(manga) &&
            filters.filterCompleted.matches(manga) &&
            filters.filterMangaType.matches(manga) &&
            filters.filterMerged.matches(manga) &&
            filters.filterUnavailable.matches(manga) &&
            filters.filterTracked.matches(manga)
    }
}
