package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.util.lang.removeArticles
import org.nekomanga.domain.manga.LibraryMangaItem

fun LibraryMangaItemComparator(
    librarySort: LibrarySort,
    removeArticles: Boolean = false,
    mangaOrder: List<Long>,
    lastReadMapFn: () -> Map<Long, Int>,
    lastFetchMapFn: () -> Map<Long, Int>,
): Comparator<LibraryMangaItem> {
    return when (librarySort) {
        LibrarySort.Title -> {
            compareBy {
                when (removeArticles) {
                    true -> it.displayManga.title.removeArticles()
                    false -> it.displayManga.title
                }
            }
        }
        LibrarySort.DragAndDrop -> {
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
        LibrarySort.LastRead -> {
            val lastReadMap = lastReadMapFn()
            Comparator { item1, item2 ->
                val lastRead1 = lastReadMap[item1.displayManga.mangaId] ?: lastReadMap.size
                val lastRead2 = lastReadMap[item2.displayManga.mangaId] ?: lastReadMap.size
                lastRead1.compareTo(lastRead2)
            }
        }
        LibrarySort.LatestChapter -> compareByDescending { it.latestChapterDate }
        LibrarySort.Unread -> compareByDescending { it.unreadCount }
        LibrarySort.TotalChapters -> compareByDescending { it.totalChapterCount }
        LibrarySort.DateAdded -> compareByDescending { it.addedToLibraryDate }
        LibrarySort.DateFetched -> {
            val lastFetchMap = lastFetchMapFn()
            Comparator { item1, item2 ->
                val lastFetched1 = lastFetchMap[item1.displayManga.mangaId] ?: lastFetchMap.size
                val lastFetched2 = lastFetchMap[item2.displayManga.mangaId] ?: lastFetchMap.size
                lastFetched1.compareTo(lastFetched2)
            }
        }
        LibrarySort.Rating -> compareByDescending { it.rating }
    }
}
