package org.nekomanga.presentation.screens.feed

import eu.kanade.tachiyomi.data.download.model.Download
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoveDownloadTest {

    private val queue = listOf(download(1), download(2), download(3))

    @Test
    fun `moves the chapter to the top`() {
        val moved = queue.withChapterMoved(3L, MoveDownloadDirection.Top)

        assertEquals(listOf(3L, 1L, 2L), moved?.map { it.chapterItem.id })
    }

    @Test
    fun `moves the chapter to the bottom`() {
        val moved = queue.withChapterMoved(1L, MoveDownloadDirection.Bottom)

        assertEquals(listOf(2L, 3L, 1L), moved?.map { it.chapterItem.id })
    }

    @Test
    fun `moves a middle chapter to the top`() {
        val moved = queue.withChapterMoved(2L, MoveDownloadDirection.Top)

        assertEquals(listOf(2L, 1L, 3L), moved?.map { it.chapterItem.id })
    }

    @Test
    fun `moves a middle chapter to the bottom`() {
        val moved = queue.withChapterMoved(2L, MoveDownloadDirection.Bottom)

        assertEquals(listOf(1L, 3L, 2L), moved?.map { it.chapterItem.id })
    }

    @Test
    fun `a chapter already at the destination moves nothing`() {
        assertNull(queue.withChapterMoved(1L, MoveDownloadDirection.Top))
        assertNull(queue.withChapterMoved(3L, MoveDownloadDirection.Bottom))
    }

    @Test
    fun `a chapter that already left the queue moves nothing`() {
        assertNull(queue.withChapterMoved(4L, MoveDownloadDirection.Top))
        assertNull(queue.withChapterMoved(4L, MoveDownloadDirection.Bottom))
    }

    @Test
    fun `an empty queue moves nothing`() {
        assertNull(emptyList<Download>().withChapterMoved(1L, MoveDownloadDirection.Top))
    }

    private fun download(chapterId: Long): Download = mockk {
        every { chapterItem } returns mockk { every { id } returns chapterId }
    }
}
