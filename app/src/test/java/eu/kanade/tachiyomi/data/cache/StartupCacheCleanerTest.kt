package eu.kanade.tachiyomi.data.cache

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StartupCacheCleanerTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val startedAt = 1_800_000_000_000L

    private fun File.withTimestamp(time: Long): File = apply { setLastModified(time) }

    @Test
    fun `deletes tmp files that an earlier run left in the cache folder`() = runTest {
        val cacheDir = tempFolder.newFolder("cache")
        val leftover =
            File(cacheDir, "Ch.1 - A Useless Guy123.tmp").apply {
                writeText("zip")
                withTimestamp(startedAt - 60_000)
            }

        StartupCacheCleaner(cacheDir, mockk(relaxed = true)).clean(startedAt)

        assertFalse(leftover.exists())
    }

    @Test
    fun `keeps tmp files created after the app started`() = runTest {
        val cacheDir = tempFolder.newFolder("cache")
        val inUse =
            File(cacheDir, "Ch.2456.tmp").apply {
                writeText("zip")
                withTimestamp(startedAt + 60_000)
            }

        StartupCacheCleaner(cacheDir, mockk(relaxed = true)).clean(startedAt)

        assertTrue(inUse.exists())
    }

    @Test
    fun `keeps other files and anything inside folders`() = runTest {
        val cacheDir = tempFolder.newFolder("cache")
        val otherFile =
            File(cacheDir, "cover.jpg").apply {
                writeText("jpg")
                withTimestamp(startedAt - 60_000)
            }
        val folderNamedTmp = File(cacheDir, "folder.tmp").apply { mkdirs() }
        val nestedTmp =
            File(File(cacheDir, "chapter_disk_cache").apply { mkdirs() }, "page.tmp").apply {
                writeText("page")
                withTimestamp(startedAt - 60_000)
            }
        folderNamedTmp.withTimestamp(startedAt - 60_000)

        StartupCacheCleaner(cacheDir, mockk(relaxed = true)).clean(startedAt)

        assertTrue(otherFile.exists())
        assertTrue(folderNamedTmp.exists())
        assertTrue(nestedTmp.exists())
    }

    @Test
    fun `trims online covers to the size limit, oldest first`() = runTest {
        val cacheDir = tempFolder.newFolder("cache")
        val filesDir = tempFolder.newFolder("files")
        val context =
            mockk<Context> {
                every { getCacheDir() } returns cacheDir
                every { getFilesDir() } returns filesDir
                every { getExternalFilesDir(any()) } returns null
            }
        val coverCache = CoverCache(context)
        val covers =
            (0 until 4).map { index ->
                File(coverCache.onlineCoverDirectory, "cover$index").apply {
                    RandomAccessFile(this, "rw").use { it.setLength(20L * 1024L * 1024L) }
                    withTimestamp(startedAt - 60_000 + index * 1_000L)
                }
            }

        StartupCacheCleaner(cacheDir, coverCache).clean(startedAt)

        assertFalse(covers[0].exists())
        assertFalse(covers[1].exists())
        assertTrue(covers[2].exists())
        assertTrue(covers[3].exists())
    }
}
