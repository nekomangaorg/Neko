package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import eu.kanade.tachiyomi.data.database.tables.ChapterTable
import eu.kanade.tachiyomi.data.database.tables.MangaTable
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaChapterGetResolverTest {

    @Test
    fun `test chapter id resolution with duplicate _id column`() {
        val cursor = mockk<Cursor>(relaxed = true)
        val resolver = MangaChapterGetResolver()

        val mangaId = 100L
        val chapterId = 200L // This is what we WANT, but won't get if not fixed

        // Simulate the query result where _id appears twice but getColumnIndex returns the first
        // one
        every { cursor.getColumnIndex("_id") } returns 0
        every { cursor.getLong(0) } returns mangaId

        // Mock other required columns to avoid NPE or errors in resolvers
        // Manga columns
        every { cursor.getColumnIndex(MangaTable.COL_SOURCE) } returns 1
        every { cursor.getLong(1) } returns 1L
        every { cursor.getColumnIndex(MangaTable.COL_URL) } returns 2
        every { cursor.getString(2) } returns "manga_url"
        every { cursor.getColumnIndex("mangaUrl") } returns 3
        every { cursor.getString(3) } returns "manga_url_alias"

        // Chapter columns
        // Note: ChapterGetResolver also looks for _id and gets index 0 -> mangaId

        // Mocking chapter specific columns that don't clash
        every { cursor.getColumnIndex(ChapterTable.COL_MANGA_ID) } returns 4
        every { cursor.getLong(4) } returns mangaId // Correctly linked

        // New column alias
        every { cursor.getColumnIndex("chapter_id") } returns 5
        every { cursor.getLong(5) } returns chapterId

        // Execute
        val result = resolver.mapFromCursor(cursor)

        // Assert
        assertEquals(mangaId, result.manga.id)

        // After fix: it should pick up chapterId from the alias
        assertEquals(chapterId, result.chapter.id)
    }
}
