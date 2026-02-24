package eu.kanade.tachiyomi.ui.library

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
            LibrarySort.DragAndDrop -> compareByDragAndDrop(mangaOrder)
            LibrarySort.LastRead ->
                compareBy { lastReadMap[it.displayManga.mangaId] ?: lastReadMap.size }
            LibrarySort.LatestChapter -> compareByDescending { it.latestChapterDate }
            LibrarySort.Unread -> compareWithZeroAtEnd(categoryIsAscending) { it.unreadCount }
            LibrarySort.TotalChapters -> compareByDescending { it.totalChapterCount }
            LibrarySort.DateAdded -> compareByDescending { it.addedToLibraryDate }
            LibrarySort.DateFetched ->
                compareBy { lastFetchMap[it.displayManga.mangaId] ?: lastFetchMap.size }
            LibrarySort.Rating -> compareByDescending { it.rating }
            LibrarySort.Downloads -> compareWithZeroAtEnd(categoryIsAscending) { it.downloadCount }
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
            true -> it.titleWithoutArticles
            false -> it.displayManga.getTitle()
        }
    }
}

private fun compareByDragAndDrop(mangaOrder: List<Long>): Comparator<LibraryMangaItem> {
    // Drag and drop is a custom sort that doesn't need a tie-breaker.
    // It will be handled as a special case.
    val orderMap = mangaOrder.withIndex().associate { it.value to it.index }

    return Comparator { item1, item2 ->
        val index1 = orderMap[item1.displayManga.mangaId]
        val index2 = orderMap[item2.displayManga.mangaId]

        // If not in map, index is null. Treat as -1 (similar to List.indexOf).
        val i1 = index1 ?: -1
        val i2 = index2 ?: -1

        when {
            i1 == i2 -> 0
            i1 == -1 -> -1
            i2 == -1 -> 1
            else -> i1.compareTo(i2)
        }
    }
}

private fun compareWithZeroAtEnd(
    categoryIsAscending: Boolean,
    selector: (LibraryMangaItem) -> Int,
): Comparator<LibraryMangaItem> {
    return compareBy {
        val value = selector(it)
        when (value) {
            0 -> if (categoryIsAscending) Int.MAX_VALUE else Int.MIN_VALUE
            else -> value
        }
    }
}
