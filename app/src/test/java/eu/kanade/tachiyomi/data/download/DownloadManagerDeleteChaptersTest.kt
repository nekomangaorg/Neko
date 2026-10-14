package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

/**
 * Deleting chapters removes their folders, and the manga folder once nothing is left in it. A
 * chapter that is still downloading sits in the manga folder as a `_tmp` folder.
 */
class DownloadManagerDeleteChaptersTest {

    @get:Rule val folder = TemporaryFolder()

    private val manga = Manga.create("/title/1", "Manga").apply { id = 1L }

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
        mangaDir = File(downloadsDir, "MangaDex (EN)/Manga").apply { mkdirs() }

        val source = mockk<MangaDex> { every { this@mockk.toString() } returns "MangaDex" }
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
        downloadManager = DownloadManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `deleting the last downloaded chapter keeps a chapter that is still downloading`() {
        val downloaded = File(mangaDir, "Ch.1 - $CHAPTER_1_ID").apply { mkdir() }
        File(downloaded, "001.png").writeBytes(PAGE)
        val downloading = File(mangaDir, "Ch.2 - ${CHAPTER_2_ID}_tmp").apply { mkdir() }
        File(downloading, "001.png").writeBytes(PAGE)

        deleteChapters(chapter(1, CHAPTER_1_ID))

        assertFalse(downloaded.exists())
        assertTrue(File(downloading, "001.png").exists())
    }

    @Test
    fun `deleting the last chapter removes the manga folder`() {
        val downloaded = File(mangaDir, "Ch.1 - $CHAPTER_1_ID").apply { mkdir() }
        File(downloaded, "001.png").writeBytes(PAGE)

        deleteChapters(chapter(1, CHAPTER_1_ID))

        assertFalse(mangaDir.exists())
    }

    /** Deletes [chapters] and waits until the manager reports them removed. */
    private fun deleteChapters(vararg chapters: Chapter) = runBlocking {
        withTimeout(10_000) {
            val removed =
                async(start = CoroutineStart.UNDISPATCHED) {
                    downloadManager.removedChaptersFlow.first()
                }
            downloadManager.deleteChapters(manga, chapters.toList())
            assertEquals(manga.id, removed.await())
        }
    }

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
