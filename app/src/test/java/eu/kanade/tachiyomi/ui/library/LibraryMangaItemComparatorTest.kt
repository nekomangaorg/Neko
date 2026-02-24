package eu.kanade.tachiyomi.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem

class LibraryMangaItemComparatorTest {

    private fun createItem(
        id: Long,
        title: String,
        unreadCount: Int = 0,
        downloadCount: Int = 0,
    ): LibraryMangaItem {
        val displayManga =
            DisplayManga(
                mangaId = id,
                inLibrary = true,
                currentArtwork = Artwork(mangaId = id),
                url = "",
                originalTitle = title,
                userTitle = "",
            )
        return LibraryMangaItem(
            displayManga = displayManga,
            userCover = null,
            unreadCount = unreadCount,
            downloadCount = downloadCount,
        )
    }

    @Test
    fun testDragAndDropSort() {
        val item1 = createItem(1, "A")
        val item2 = createItem(2, "B")
        val item3 = createItem(3, "C")

        val mangaOrder = listOf(2L, 1L, 3L) // B, A, C

        val comparator =
            libraryMangaItemComparator(
                categorySort = LibrarySort.DragAndDrop,
                categoryIsAscending = true,
                mangaOrder = mangaOrder,
            )

        val list = listOf(item1, item2, item3).sortedWith(comparator)

        // Expected: 2 (B), 1 (A), 3 (C)
        assertEquals(2L, list[0].displayManga.mangaId)
        assertEquals(1L, list[1].displayManga.mangaId)
        assertEquals(3L, list[2].displayManga.mangaId)
    }

    @Test
    fun testUnreadSortAscending() {
        // Ascending: 1, 5, 0 (MAX_VALUE) -> 1, 5, 0
        val item1 = createItem(1, "A", unreadCount = 1)
        val item2 = createItem(2, "B", unreadCount = 5)
        val item3 = createItem(3, "C", unreadCount = 0)

        val comparator =
            libraryMangaItemComparator(
                categorySort = LibrarySort.Unread,
                categoryIsAscending = true,
            )

        val list = listOf(item3, item2, item1).sortedWith(comparator)

        assertEquals(1, list[0].unreadCount)
        assertEquals(5, list[1].unreadCount)
        assertEquals(0, list[2].unreadCount)
    }

    @Test
    fun testUnreadSortDescending() {
        // Descending: 5, 1, 0
        val item1 = createItem(1, "A", unreadCount = 1)
        val item2 = createItem(2, "B", unreadCount = 5)
        val item3 = createItem(3, "C", unreadCount = 0)

        val comparator =
            libraryMangaItemComparator(
                categorySort = LibrarySort.Unread,
                categoryIsAscending = false,
            )

        val list = listOf(item3, item1, item2).sortedWith(comparator)

        assertEquals(5, list[0].unreadCount)
        assertEquals(1, list[1].unreadCount)
        assertEquals(0, list[2].unreadCount)
    }

    @Test
    fun testDownloadsSortAscending() {
        // Ascending: 1, 5, 0
        val item1 = createItem(1, "A", downloadCount = 1)
        val item2 = createItem(2, "B", downloadCount = 5)
        val item3 = createItem(3, "C", downloadCount = 0)

        val comparator =
            libraryMangaItemComparator(
                categorySort = LibrarySort.Downloads,
                categoryIsAscending = true,
            )

        val list = listOf(item3, item2, item1).sortedWith(comparator)

        assertEquals(1, list[0].downloadCount)
        assertEquals(5, list[1].downloadCount)
        assertEquals(0, list[2].downloadCount)
    }
}
