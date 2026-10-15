package org.nekomanga.presentation.screens.feed

import eu.kanade.tachiyomi.data.download.model.Download
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoveDownloadSeriesTest {

    private val queue = listOf(download(1, 10), download(2, 20), download(3, 10))

    @Test
    fun `moves the series to the top`() {
        val moved = queue.withSeriesMoved(20L, MoveDownloadDirection.Top)

        assertEquals(listOf(2L, 1L, 3L), moved?.map { it.chapterItem.id })
    }

    @Test
    fun `moves the series to the bottom`() {
        val moved = queue.withSeriesMoved(10L, MoveDownloadDirection.Bottom)

        assertEquals(listOf(2L, 1L, 3L), moved?.map { it.chapterItem.id })
    }

    @Test
    fun `a series already at the destination moves nothing`() {
        val grouped = listOf(download(1, 10), download(2, 10), download(3, 20))

        assertNull(grouped.withSeriesMoved(10L, MoveDownloadDirection.Top))
        assertNull(grouped.withSeriesMoved(20L, MoveDownloadDirection.Bottom))
    }

    @Test
    fun `a series that already left the queue moves nothing`() {
        assertNull(queue.withSeriesMoved(30L, MoveDownloadDirection.Top))
        assertNull(queue.withSeriesMoved(30L, MoveDownloadDirection.Bottom))
    }

    private fun download(chapterId: Long, mangaId: Long): Download = mockk {
        every { chapterItem } returns mockk { every { id } returns chapterId }
        every { mangaItem } returns mockk { every { id } returns mangaId }
    }
}
