package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.constants.MdConstants
import org.nekomanga.data.database.repository.ChapterRepository

class ChapterSourceSyncTest {

    private lateinit var chapterRepository: ChapterRepository
    private val manga = Manga.create("/title/1", "Same Title").apply { id = 1L }
    private val uuid = "2fec6641-beb1-4ae9-a32f-204dd06688ac"
    private val fileName = "Ch.1 - $uuid"

    @Before
    fun setup() {
        chapterRepository = mockk()
    }

    @Test
    fun `chapter of another manga with the same title is not ours`() = runTest {
        coEvery { chapterRepository.getChapterByUrl(MdConstants.chapterSuffix + uuid) } returns
            Chapter.create().apply { manga_id = 2L }

        assertTrue(isOtherMangaChapter(fileName, manga, chapterRepository))
    }

    @Test
    fun `chapter of this manga is ours`() = runTest {
        coEvery { chapterRepository.getChapterByUrl(MdConstants.chapterSuffix + uuid) } returns
            Chapter.create().apply { manga_id = 1L }

        assertFalse(isOtherMangaChapter(fileName, manga, chapterRepository))
    }

    @Test
    fun `chapter unknown to the database is treated as ours`() = runTest {
        coEvery { chapterRepository.getChapterByUrl(any()) } returns null

        assertFalse(isOtherMangaChapter(fileName, manga, chapterRepository))
    }

    @Test
    fun `file without a chapter uuid skips the lookup`() = runTest {
        assertFalse(isOtherMangaChapter("Ch.1", manga, chapterRepository))
        assertFalse(isOtherMangaChapter("Ch.1 - Scanlator", manga, chapterRepository))

        coVerify(exactly = 0) { chapterRepository.getChapterByUrl(any()) }
    }
}
