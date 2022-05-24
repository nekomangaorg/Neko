package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.Test

class ChapterExtensionsTest {
    @Test
    fun `Missing chapter Test`() {
        val list = listOf(createSChapter(1f, "test"), createSChapter(2f, "test2"), createSChapter(5f, "test5"))
        val count = list.getMissingChapterCount(SManga.ONGOING)
        count shouldBe "2"
    }

    private fun createSChapter(chapNum: Float, chapName: String): SChapter {
        return SChapter.create().apply {
            chapter_number = chapNum
            name = chapName
        }
    }
}
