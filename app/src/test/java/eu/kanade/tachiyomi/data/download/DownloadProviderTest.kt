package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.text.TextUtils
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.nekomanga.constants.Constants.TMP_DIR_SUFFIX
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class DownloadProviderTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var provider: DownloadProvider
    private lateinit var mangaDir: File

    private val manga = Manga.create("/manga/1", "Sample Manga").apply { id = 1L }
    private val chapter =
        Chapter.create().apply {
            name = "Ch.3 - Sisters"
            mangadex_chapter_id = CHAPTER_UUID
            url = "/chapter/$CHAPTER_UUID"
        }

    @Before
    fun setup() {
        // UniFile's RawFile checks names with TextUtils, which the android.jar stub does not run.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(mockk<SourceManager>(relaxed = true))

        val downloadsDir = folder.newFolder("downloads")
        val storageManager =
            mockk<StorageManager> {
                every { getDownloadsDirectory() } returns UniFile.fromFile(downloadsDir)
            }
        provider = DownloadProvider(mockk<Context>(relaxed = true), storageManager)

        mangaDir =
            File(File(downloadsDir, provider.getSourceDirName()), provider.getMangaDirName(manga))
        mangaDir.mkdirs()
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `findChapterDir finds a finished cbz`() {
        File(mangaDir, "$CHAPTER_DIR.cbz").writeBytes(byteArrayOf(0))

        assertEquals("$CHAPTER_DIR.cbz", provider.findChapterDir(chapter, manga)?.name)
    }

    @Test
    fun `findChapterDir finds a renamed chapter by its id`() {
        File(mangaDir, "Old name - $CHAPTER_UUID").mkdir()

        assertEquals("Old name - $CHAPTER_UUID", provider.findChapterDir(chapter, manga)?.name)
    }

    @Test
    fun `findChapterDir ignores the temp folder of a download in progress`() {
        File(mangaDir, "$CHAPTER_DIR$TMP_DIR_SUFFIX").mkdir()

        assertNull(provider.findChapterDir(chapter, manga))
    }

    @Test
    fun `findChapterDir ignores a partial cbz`() {
        File(mangaDir, "$CHAPTER_DIR.cbz$TMP_DIR_SUFFIX").writeBytes(byteArrayOf(0))

        assertNull(provider.findChapterDir(chapter, manga))
    }

    @Test
    fun `chapterDirDoesNotExist is false for a finished cbz`() {
        File(mangaDir, "$CHAPTER_DIR.cbz").writeBytes(byteArrayOf(0))

        assertEquals(false, provider.chapterDirDoesNotExist(chapter, mangaDirFiles()))
    }

    @Test
    fun `chapterDirDoesNotExist is true when only temp files of the chapter exist`() {
        File(mangaDir, "$CHAPTER_DIR$TMP_DIR_SUFFIX").mkdir()
        File(mangaDir, "$CHAPTER_DIR.cbz$TMP_DIR_SUFFIX").writeBytes(byteArrayOf(0))

        assertTrue(provider.chapterDirDoesNotExist(chapter, mangaDirFiles()))
    }

    @Test
    fun `findTempChapterDirs returns the temp folder and the partial cbz`() {
        File(mangaDir, "$CHAPTER_DIR$TMP_DIR_SUFFIX").mkdir()
        File(mangaDir, "$CHAPTER_DIR.cbz$TMP_DIR_SUFFIX").writeBytes(byteArrayOf(0))

        assertEquals(
            listOf("$CHAPTER_DIR.cbz$TMP_DIR_SUFFIX", "$CHAPTER_DIR$TMP_DIR_SUFFIX"),
            provider.findTempChapterDirs(listOf(chapter), manga).mapNotNull { it.name }.sorted(),
        )
    }

    @Test
    fun `findUnmatchedChapterDirs keeps the temp files of a queued chapter`() {
        val otherChapter =
            Chapter.create().apply {
                name = "Ch.4 - Rivals"
                mangadex_chapter_id = OTHER_CHAPTER_UUID
                url = "/chapter/$OTHER_CHAPTER_UUID"
            }
        File(mangaDir, "$CHAPTER_DIR$TMP_DIR_SUFFIX").mkdir()
        File(mangaDir, "$CHAPTER_DIR.cbz$TMP_DIR_SUFFIX").writeBytes(byteArrayOf(0))
        File(mangaDir, "$OTHER_CHAPTER_DIR$TMP_DIR_SUFFIX").mkdir()
        File(mangaDir, "$OTHER_CHAPTER_DIR.cbz$TMP_DIR_SUFFIX").writeBytes(byteArrayOf(0))

        val unmatched =
            provider.findUnmatchedChapterDirs(listOf(chapter, otherChapter), manga) {
                listOf(chapter)
            }

        assertEquals(
            listOf("$OTHER_CHAPTER_DIR.cbz$TMP_DIR_SUFFIX", "$OTHER_CHAPTER_DIR$TMP_DIR_SUFFIX"),
            unmatched.mapNotNull { it.name }.sorted(),
        )
    }

    @Test
    fun `findUnmatchedChapterDirs reads the queue after listing the folder`() {
        // The chapter gets queued right after the queue read and its download creates the temp
        // folder. A listing taken after that read would hold the new folder as unmatched.
        val unmatched =
            provider.findUnmatchedChapterDirs(listOf(chapter), manga) {
                File(mangaDir, "$CHAPTER_DIR$TMP_DIR_SUFFIX").mkdir()
                emptyList()
            }

        assertEquals(emptyList<String>(), unmatched.mapNotNull { it.name })
    }

    private fun mangaDirFiles(): List<UniFile> = UniFile.fromFile(mangaDir)!!.listFiles()!!.toList()

    companion object {
        private const val CHAPTER_UUID = "08e4952c-9141-4650-9236-83f6f2b22e01"
        private const val CHAPTER_DIR = "Ch.3 - Sisters - $CHAPTER_UUID"
        private const val OTHER_CHAPTER_UUID = "5b1c2f1e-7d0a-4c55-9f43-2a8e6d3b9c10"
        private const val OTHER_CHAPTER_DIR = "Ch.4 - Rivals - $OTHER_CHAPTER_UUID"
    }
}
