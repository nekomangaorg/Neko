package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.util.lang.removeArticles
import org.nekomanga.domain.manga.LibraryMangaItem

fun libraryMangaItemComparator(
    categorySort: LibrarySort,
    categoryIsAscending: Boolean,
    removeArticles: Boolean = false,
    mangaOrder: List<Long> = emptyList(),
    lastReadMap: Map<Long, Int> = emptyMap(),
    lastFetchMap: Map<Long, Int> = emptyMap(),
): Comparator<LibraryMangaItem> {

    val titleComparator = compareByTitle(removeArticles)

    val primaryComparator =
        when (categorySort) {
            LibrarySort.Title -> titleComparator
            LibrarySort.DragAndDrop -> {
                // Drag and drop is a custom sort that doesn't need a tie-breaker.
                // It will be handled as a special case.
                Comparator { item1, item2 ->
                    val index1 = mangaOrder.indexOf(item1.displayManga.mangaId)
                    val index2 = mangaOrder.indexOf(item2.displayManga.mangaId)
                    when {
                        index1 == index2 -> 0
                        index1 == -1 -> -1
                        index2 == -1 -> 1
                        else -> index1.compareTo(index2)
                    }
                }
            }

            LibrarySort.LastRead ->
                compareBy { lastReadMap[it.displayManga.mangaId] ?: lastReadMap.size }

            LibrarySort.LatestChapter -> compareByDescending { it.latestChapterDate }
            LibrarySort.Unread -> compareBy { it.unreadCount }
            LibrarySort.TotalChapters -> compareByDescending { it.totalChapterCount }
            LibrarySort.DateAdded -> compareByDescending { it.addedToLibraryDate }
            LibrarySort.DateFetched ->
                compareBy { lastFetchMap[it.displayManga.mangaId] ?: lastFetchMap.size }

            LibrarySort.Rating -> compareByDescending { it.rating }
        }
    return when (categorySort) {
        LibrarySort.DragAndDrop -> primaryComparator // No secondary sort for drag and drop
        LibrarySort.Title ->
            if (categoryIsAscending) primaryComparator else primaryComparator.reversed()
        else -> {
            if (categoryIsAscending) {
                primaryComparator.then(titleComparator)
            } else {
                primaryComparator.reversed().then(titleComparator)
            }
        }
    }
}

private fun compareByTitle(removeArticles: Boolean): Comparator<LibraryMangaItem> {
    return compareBy {
        when (removeArticles) {
            true -> it.displayManga.title.removeArticles()
            false -> it.displayManga.title
        }
    }
}
