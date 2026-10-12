package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DownloadsFirstTest {

    private val queue = listOf(download(1), download(2), download(3))

    @Test
    fun `puts the downloads first and keeps the order of the rest`() {
        val result = queue.withDownloadsFirst(listOf(download(4)))

        assertEquals(listOf(4L, 1L, 2L, 3L), result.map { it.chapterItem.id })
    }

    @Test
    fun `a chapter that is already queued is moved, not queued twice`() {
        val result = queue.withDownloadsFirst(listOf(download(3)))

        assertEquals(listOf(3L, 1L, 2L), result.map { it.chapterItem.id })
    }

    @Test
    fun `a chapter that is already queued keeps its queued download`() {
        val result = queue.withDownloadsFirst(listOf(download(2)))

        assertSame(queue[1], result.first())
    }

    private fun download(chapterId: Long): Download = mockk {
        every { chapterItem } returns mockk { every { id } returns chapterId }
    }
}
