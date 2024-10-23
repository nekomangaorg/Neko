package eu.kanade.tachiyomi.util

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter

class ChapterExtensionsTest {
    @Test
    fun `Missing chapter Test`() {
        val list = listOf(createChp(1f, "test"), createChp(2f, "test2"), createChp(5f, "test5"))
        val count = list.getMissingChapters().count
        count shouldBe "2"
    }

    private fun createChp(chapNum: Float, chapName: String): ChapterItem {
        return ChapterItem(
            chapter =
                SimpleChapter(
                    id = 0,
                    mangaId = 0,
                    read = false,
                    bookmark = false,
                    lastPageRead = 0,
                    dateFetch = 0,
                    sourceOrder = 1,
                    url = "",
                    name = chapName,
                    dateUpload = 0,
                    chapterNumber = chapNum,
                    pagesLeft = 0,
                    volume = "",
                    chapterText = "",
                    chapterTitle = "",
                    language = "",
                    mangaDexChapterId = "",
                    oldMangaDexChapterId = null,
                    scanlator = "",
                )
        )
    }
}
