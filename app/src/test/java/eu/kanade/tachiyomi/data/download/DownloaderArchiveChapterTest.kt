package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.text.TextUtils
import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.nekomanga.R
import org.nekomanga.constants.Constants.TMP_DIR_SUFFIX

class DownloaderArchiveChapterTest {

    @get:Rule val folder = TemporaryFolder()

    private val context =
        mockk<Context> {
            every { getString(R.string.download_notifier_cannot_create_file) } returns
                CANNOT_CREATE_FILE
        }
    private lateinit var mangaDir: File
    private lateinit var tmpDir: File

    @Before
    fun setup() {
        // UniFile's RawFile checks names with TextUtils, which the android.jar stub does not run.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }

        mangaDir = folder.newFolder("manga")
        tmpDir = File(mangaDir, "$CHAPTER$TMP_DIR_SUFFIX").apply { mkdir() }
        File(tmpDir, "001.png").writeBytes(PAGE_1)
        File(tmpDir, "002.png").writeBytes(PAGE_2)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `archives the pages as a cbz and deletes the temp directory`() {
        archive()

        assertEquals(listOf("$CHAPTER.cbz"), mangaDir.list()!!.toList())
        ZipFile(File(mangaDir, "$CHAPTER.cbz")).use { zip ->
            val entries = zip.entries().toList().sortedBy { it.name }
            assertEquals(listOf("001.png", "002.png"), entries.map { it.name })
            assertEquals(listOf(ZipEntry.STORED, ZipEntry.STORED), entries.map { it.method })
            assertArrayEquals(PAGE_1, zip.getInputStream(entries[0]).readBytes())
            assertArrayEquals(PAGE_2, zip.getInputStream(entries[1]).readBytes())
        }
    }

    @Test
    fun `fails with a message and keeps the pages when the cbz file cannot be created`() {
        // A directory in the way makes createFile return null.
        File(mangaDir, "$CHAPTER.cbz$TMP_DIR_SUFFIX").mkdir()

        val error = assertThrows(Exception::class.java) { archive() }

        assertEquals(CANNOT_CREATE_FILE, error.message)
        assertEquals(listOf("001.png", "002.png"), tmpDir.list()!!.sorted())
    }

    @Test
    fun `removes the partial cbz and keeps the pages when a page cannot be read`() {
        // A directory among the pages makes its openInputStream throw.
        File(tmpDir, "003").mkdir()

        assertThrows(Exception::class.java) { archive() }

        assertEquals(listOf(tmpDir.name), mangaDir.list()!!.toList())
        assertEquals(listOf("001.png", "002.png", "003"), tmpDir.list()!!.sorted())
    }

    @Test
    fun `fails with a message and keeps the pages when the cbz cannot be renamed`() {
        // A non-empty directory under the final name makes the rename fail.
        File(mangaDir, "$CHAPTER.cbz").mkdir()
        File(mangaDir, "$CHAPTER.cbz/x").writeBytes(byteArrayOf(0))

        val error = assertThrows(Exception::class.java) { archive() }

        assertEquals(CANNOT_CREATE_FILE, error.message)
        assertEquals(listOf("$CHAPTER.cbz", tmpDir.name), mangaDir.list()!!.sorted())
        assertEquals(listOf("001.png", "002.png"), tmpDir.list()!!.sorted())
    }

    private fun archive() {
        Downloader.archiveChapter(
            context,
            UniFile.fromFile(mangaDir)!!,
            CHAPTER,
            UniFile.fromFile(tmpDir)!!,
        )
    }

    companion object {
        private const val CHAPTER = "Group_Ch.1"
        private const val CANNOT_CREATE_FILE = "Couldn't create a file in the download folder"
        private val PAGE_1 = byteArrayOf(1, 2, 3)
        private val PAGE_2 = byteArrayOf(4, 5, 6, 7)
    }
}
