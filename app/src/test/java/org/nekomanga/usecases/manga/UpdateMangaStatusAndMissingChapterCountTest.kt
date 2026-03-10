package org.nekomanga.usecases.manga

import com.pushtorefresh.storio.sqlite.operations.get.PreparedGetListOfObjects
import com.pushtorefresh.storio.sqlite.operations.put.PreparedPutObject
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.chapter.MissingChapterHolder
import eu.kanade.tachiyomi.util.chapter.getMissingChapters
import eu.kanade.tachiyomi.util.chapter.isAvailable
import eu.kanade.tachiyomi.util.system.executeOnIO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter

class UpdateMangaStatusAndMissingChapterCountTest {

    private lateinit var db: DatabaseHelper
    private lateinit var downloadManager: DownloadManager
    private lateinit var updateMangaStatusAndMissingChapterCount:
        UpdateMangaStatusAndMissingChapterCount

    @Before
    fun setup() {
        db = mockk()
        downloadManager = mockk()
        updateMangaStatusAndMissingChapterCount =
            UpdateMangaStatusAndMissingChapterCount(db, downloadManager)

        mockkStatic("eu.kanade.tachiyomi.util.system.DatabaseExtensionsKt")
        mockkStatic("org.nekomanga.domain.chapter.ChapterKt")
        mockkStatic("eu.kanade.tachiyomi.util.chapter.ChapterExtensionsKt")
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given database throws exception when calculating missing chapters then manga is updated with empty missing chapters and completed status is checked`() =
        runTest {
            // Arrange
            val manga =
                MangaImpl().apply {
                    id = 1L
                    status = SManga.ONGOING
                    missing_chapters = "1" // Set it to something different than empty
                    url = ""
                    title = ""
                }

            val getChaptersMock =
                mockk<PreparedGetListOfObjects<eu.kanade.tachiyomi.data.database.models.Chapter>>()
            every { db.getChapters(manga) } returns getChaptersMock
            coEvery { getChaptersMock.executeOnIO() } throws Exception("Database error")

            val insertMangaMock =
                mockk<PreparedPutObject<eu.kanade.tachiyomi.data.database.models.Manga>>()
            every { db.insertManga(manga) } returns insertMangaMock
            coEvery { insertMangaMock.executeOnIO() } returns mockk()

            // Act
            updateMangaStatusAndMissingChapterCount(manga)

            // Assert
            assertEquals("", manga.missing_chapters)
            coVerify(exactly = 1) { db.insertManga(manga) }
        }

    @Test
    fun `given valid chapters list with missing chapters when calculating then missing chapters string is updated correctly`() =
        runTest {
            // Arrange
            val manga =
                MangaImpl().apply {
                    id = 1L
                    status = SManga.ONGOING
                    missing_chapters = null
                    url = ""
                    title = ""
                }

            val chapter1 =
                ChapterImpl().apply {
                    id = 1L
                    chapter_number = 1f
                    name = "Chapter 1"
                    chapter_txt = "1"
                    url = ""
                }

            // Missing Chapter 2

            val chapter3 =
                ChapterImpl().apply {
                    id = 3L
                    chapter_number = 3f
                    name = "Chapter 3"
                    chapter_txt = "3"
                    url = ""
                }

            val chaptersList = listOf(chapter1, chapter3)

            val getChaptersMock =
                mockk<PreparedGetListOfObjects<eu.kanade.tachiyomi.data.database.models.Chapter>>()
            every { db.getChapters(manga) } returns getChaptersMock
            coEvery { getChaptersMock.executeOnIO() } returns chaptersList

            val simpleChapter1 = mockk<SimpleChapter>()
            val chapterItem1 = mockk<ChapterItem>()
            every { chapter1.toSimpleChapter(any()) } returns simpleChapter1
            every { simpleChapter1.toChapterItem() } returns chapterItem1
            every { chapterItem1.chapter } returns simpleChapter1
            every { simpleChapter1.chapterNumber } returns 1f
            every { simpleChapter1.chapterText } returns "1"
            every { simpleChapter1.volume } returns ""
            every { chapterItem1.isAvailable() } returns true

            val simpleChapter3 = mockk<SimpleChapter>()
            val chapterItem3 = mockk<ChapterItem>()
            every { chapter3.toSimpleChapter(any()) } returns simpleChapter3
            every { simpleChapter3.toChapterItem() } returns chapterItem3
            every { chapterItem3.chapter } returns simpleChapter3
            every { simpleChapter3.chapterNumber } returns 3f
            every { simpleChapter3.chapterText } returns "3"
            every { simpleChapter3.volume } returns ""
            every { chapterItem3.isAvailable() } returns true

            val chapterItemList = listOf(chapterItem1, chapterItem3)
            every { chapterItemList.getMissingChapters() } returns MissingChapterHolder(count = "2")

            val insertMangaMock =
                mockk<PreparedPutObject<eu.kanade.tachiyomi.data.database.models.Manga>>()
            every { db.insertManga(manga) } returns insertMangaMock
            coEvery { insertMangaMock.executeOnIO() } returns mockk()

            // Act
            updateMangaStatusAndMissingChapterCount(manga)

            // Assert
            assertEquals("2", manga.missing_chapters)
            coVerify(exactly = 1) { db.insertManga(manga) }
        }

    @Test
    fun `given manga cancelled and final chapter available when calculating then manga status is updated to completed`() =
        runTest {
            // Arrange
            val manga =
                MangaImpl().apply {
                    id = 1L
                    status = SManga.CANCELLED
                    last_chapter_number = 5
                    missing_chapters = null
                    url = ""
                    title = ""
                }

            val finalChapter =
                ChapterImpl().apply {
                    id = 5L
                    chapter_number = 5f
                    name = "Chapter 5"
                    chapter_txt = "5"
                    url = ""
                }

            val chaptersList = listOf(finalChapter)

            val getChaptersMock =
                mockk<PreparedGetListOfObjects<eu.kanade.tachiyomi.data.database.models.Chapter>>()
            every { db.getChapters(manga) } returns getChaptersMock
            coEvery { getChaptersMock.executeOnIO() } returns chaptersList

            val simpleChapterFinal = mockk<SimpleChapter>()
            val chapterItemFinal = mockk<ChapterItem>()
            every { finalChapter.toSimpleChapter(any()) } returns simpleChapterFinal
            every { simpleChapterFinal.toChapterItem() } returns chapterItemFinal
            every { chapterItemFinal.chapter } returns simpleChapterFinal
            every { simpleChapterFinal.chapterNumber } returns 5f
            every { simpleChapterFinal.chapterText } returns "5"
            every { simpleChapterFinal.volume } returns ""
            every { chapterItemFinal.isAvailable() } returns true

            val chapterItemList = listOf(chapterItemFinal)
            every { chapterItemList.getMissingChapters() } returns MissingChapterHolder(count = "")

            // Mock DownloadManager check for isAvailable
            every { finalChapter.isAvailable(downloadManager, manga) } returns true

            val insertMangaMock =
                mockk<PreparedPutObject<eu.kanade.tachiyomi.data.database.models.Manga>>()
            every { db.insertManga(manga) } returns insertMangaMock
            coEvery { insertMangaMock.executeOnIO() } returns mockk()

            // Act
            updateMangaStatusAndMissingChapterCount(manga)

            // Assert
            assertEquals(SManga.COMPLETED, manga.status)
            coVerify(exactly = 1) { db.insertManga(manga) }
        }
}
