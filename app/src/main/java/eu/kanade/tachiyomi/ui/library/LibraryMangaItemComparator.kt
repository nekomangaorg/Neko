package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.util.lang.removeArticles
import org.nekomanga.domain.manga.LibraryMangaItem

fun LibraryMangaItemComparator(librarySort: LibrarySort, removeArticles: Boolean = false, lastReadMap : Map<Long, Int> = emptyMap()){
    when(librarySort){
        LibrarySort.Title -> LibraryMangaItemTitleComparator(removeArticles)
        LibrarySort.LastRead -> LibraryMangaItemLastReadComparator(lastReadMap)
        LibrarySort.LatestChapter -> LibraryMangaItemLatestChapterComparator()
        LibrarySort.Unread -> LibraryMangaItemUnreadCountComparator()
        LibrarySort.TotalChapters -> LibraryMangaItemTotalChapterComparator()
        LibrarySort.DateAdded -> LibraryMangaItemDateAddedComparator()
        LibrarySort.DateFetched -> TODO()
        LibrarySort.DragAndDrop -> TODO()
        LibrarySort.Rating -> TODO()
    }

}

class LibraryMangaItemTitleComparator(val removeArticles: Boolean = false) : Comparator<LibraryMangaItem> {
    override fun compare(
        libraryMangaItem1: LibraryMangaItem,
        libraryMangaItem2: LibraryMangaItem,
    ): Int {
        return when (removeArticles) {
            true -> libraryMangaItem1.displayManga.title.removeArticles()
                .compareTo(libraryMangaItem2.displayManga.title.removeArticles(), true)

            false -> libraryMangaItem1.displayManga.title.removeArticles()
                .compareTo(libraryMangaItem2.displayManga.title.removeArticles(), true)
        }
    }
}

class LibraryMangaItemLatestChapterComparator() : Comparator<LibraryMangaItem> {
    override fun compare(
        libraryMangaItem1: LibraryMangaItem,
        libraryMangaItem2: LibraryMangaItem,
    ): Int {
        return libraryMangaItem1.latestChapterDate.compareTo(libraryMangaItem2.latestChapterDate)
    }
}

class LibraryMangaItemUnreadCountComparator() : Comparator<LibraryMangaItem> {
    override fun compare(
        libraryMangaItem1: LibraryMangaItem,
        libraryMangaItem2: LibraryMangaItem,
    ): Int {
        return libraryMangaItem1.unreadCount.compareTo(libraryMangaItem2.unreadCount)
    }
}

class LibraryMangaItemLastReadComparator(val lastReadMangaMap : Map<Long, Int>) : Comparator<LibraryMangaItem> {
    override fun compare(
        libraryMangaItem1: LibraryMangaItem,
        libraryMangaItem2: LibraryMangaItem,
    ): Int {
        val manga1LastRead =
            lastReadMangaMap[libraryMangaItem1.displayManga.mangaId] ?: lastReadMangaMap.size
        val manga2LastRead =
            lastReadMangaMap[libraryMangaItem2.displayManga.mangaId] ?: lastReadMangaMap.size
        return manga1LastRead.compareTo(manga2LastRead)
    }
}

class LibraryMangaItemTotalChapterComparator() : Comparator<LibraryMangaItem> {
    override fun compare(
        libraryMangaItem1: LibraryMangaItem,
        libraryMangaItem2: LibraryMangaItem,
    ): Int {
        return libraryMangaItem2.totalChapterCount.compareTo(libraryMangaItem1.totalChapterCount)
    }
}

class LibraryMangaItemDateAddedComparator() : Comparator<LibraryMangaItem> {
    override fun compare(
        libraryMangaItem1: LibraryMangaItem,
        libraryMangaItem2: LibraryMangaItem,
    ): Int {
      return  libraryMangaItem2.addedToLibraryDate.compareTo(libraryMangaItem1.addedToLibraryDate)
    }
}





