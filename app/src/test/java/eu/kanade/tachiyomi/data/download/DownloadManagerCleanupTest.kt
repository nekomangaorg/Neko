package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.SimpleManga
import org.nekomanga.domain.storage.StorageManager
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * Cleaning up downloads deletes leftover temp folders, but not the one a queued chapter is still
 * downloading into.
 */
class DownloadManagerCleanupTest {

    @get:Rule val folder = TemporaryFolder()

    private val manga = Manga.create("/title/1", "Manga").apply { id = 1L }
    private val source = mockk<MangaDex> { every { this@mockk.toString() } returns "MangaDex" }
    private val queueState = MutableStateFlow<List<Download>>(emptyList())

    private lateinit var mangaDir: File
    private lateinit var downloadManager: DownloadManager

    @Before
    fun setup() {
        // UniFile's RawFile and the chapter folder lookup call TextUtils, which the android.jar
        // stub does not run.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        every { TextUtils.isDigitsOnly(any()) } answers
            {
                firstArg<CharSequence>().all { it.isDigit() }
            }

        val downloadsDir = folder.newFolder("downloads")
        // Without it, getMangaDir creates one and asks the media scanner, which the stub does not
        // run.
        File(downloadsDir, DiskUtil.NOMEDIA_FILE).writeBytes(byteArrayOf())
        mangaDir = File(downloadsDir, "MangaDex (EN)/Manga").apply { mkdirs() }

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(mockk<SourceManager> { every { mangaDex } returns source })
        Injekt.addSingleton(
            mockk<StorageManager> {
                every { getDownloadsDirectory() } returns UniFile.fromFile(downloadsDir)
                every { baseDirChanges } returns MutableSharedFlow()
            }
        )
        Injekt.addSingleton(
            mockk<MangaRepository> { coEvery { getMangaList() } returns emptyList() }
        )

        val preferences =
            mockk<SharedPreferences>(relaxed = true) {
                every { all } returns emptyMap<String, Any>()
                every { getString(any(), any()) } returns null
            }
        val context =
            mockk<Context>(relaxed = true) {
                every { getSharedPreferences(any(), any()) } returns preferences
            }
        mockkConstructor(Downloader::class)
        every { anyConstructed<Downloader>().queueState } returns queueState
        downloadManager = DownloadManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `cleanup keeps the temp folder of a queued chapter missing from the chapter list`() {
        // The chapter list is read from the database before the cleanup starts, so a chapter
        // added and queued since then is only in the download queue.
        queueState.value = listOf(download(chapter(2, CHAPTER_2_ID)))
        val downloading = File(mangaDir, "Ch.2 - ${CHAPTER_2_ID}_tmp").apply { mkdir() }
        File(downloading, "001.png").writeBytes(PAGE)

        cleanup(chapter(1, CHAPTER_1_ID))

        assertTrue(File(downloading, "001.png").exists())
    }

    @Test
    fun `cleanup keeps the temp folder of a queued chapter renamed after it was queued`() {
        // A refresh renames the chapter in the database but not in the queue, and the download
        // names its temp folder from the database.
        queueState.value = listOf(download(chapter(2, CHAPTER_2_ID)))
        val renamed = chapter(2, CHAPTER_2_ID).apply { name = "Ch.2 - New title" }
        val downloading = File(mangaDir, "Ch.2 - New title - ${CHAPTER_2_ID}_tmp").apply { mkdir() }
        File(downloading, "001.png").writeBytes(PAGE)

        cleanup(chapter(1, CHAPTER_1_ID), renamed)

        assertTrue(File(downloading, "001.png").exists())
    }

    @Test
    fun `cleanup deletes the temp folder of a chapter that is not queued`() {
        val leftover = File(mangaDir, "Ch.2 - ${CHAPTER_2_ID}_tmp").apply { mkdir() }
        File(leftover, "001.png").writeBytes(PAGE)

        cleanup(chapter(1, CHAPTER_1_ID), chapter(2, CHAPTER_2_ID))

        assertFalse(leftover.exists())
    }

    private fun cleanup(vararg chapters: Chapter) {
        downloadManager.cleanupChapters(
            chapters.toList(),
            manga,
            removeRead = false,
            removeNonFavorite = false,
        )
    }

    private fun download(chapter: Chapter): Download =
        Download(source, SimpleManga(manga.title, manga.id!!), chapter.toSimpleChapter()!!)

    private fun chapter(id: Long, mangadexId: String): Chapter =
        Chapter.create().apply {
            this.id = id
            manga_id = manga.id
            url = "/chapter/$mangadexId"
            name = "Ch.$id"
            mangadex_chapter_id = mangadexId
        }

    private companion object {
        const val CHAPTER_1_ID = "11111111-1111-4111-8111-111111111111"
        const val CHAPTER_2_ID = "22222222-2222-4222-8222-222222222222"
        val PAGE = byteArrayOf(1, 2, 3)
    }
}
