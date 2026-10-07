package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.source.model.Page
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloaderReIndexPagesTest {

    @Test
    fun `re-indexes pages from zero`() {
        val pages = listOf(page(1, "a.png"), page(2, "b.png"), page(3, "c.png"))

        val reIndexed = Downloader.reIndexPages(pages)

        assertEquals(listOf(0, 1, 2), reIndexed.map { it.index })
        assertEquals(pages.map { it.url }, reIndexed.map { it.url })
        assertEquals(pages.map { it.imageUrl }, reIndexed.map { it.imageUrl })
    }

    @Test
    fun `keeps the MangaDex chapter id that image requests fetch the at-home server for`() {
        val pages = listOf(page(1, "a.png"), page(2, "b.png"))

        val reIndexed = Downloader.reIndexPages(pages)

        assertEquals(listOf(CHAPTER_ID, CHAPTER_ID), reIndexed.map { it.mangaDexChapterId })
    }

    private fun page(index: Int, file: String) =
        Page(index, AT_HOME_BASE_URL, "/data/$HASH/$file", CHAPTER_ID)

    companion object {
        private const val CHAPTER_ID = "5e8bc984-5f1f-4ef4-9ab0-b0bba9b2a9ad"
        private const val HASH = "0123456789abcdef"
        private const val AT_HOME_BASE_URL = "https://node.example.org/token"
    }
}
