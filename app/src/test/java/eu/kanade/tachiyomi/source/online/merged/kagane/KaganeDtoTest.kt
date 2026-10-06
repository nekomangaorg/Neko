package eu.kanade.tachiyomi.source.online.merged.kagane

import eu.kanade.tachiyomi.util.chapter.getChapterNum
import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.Test

class KaganeDtoTest {

    private fun chapter(number: Float) =
        ChapterBook(
                id = "b1",
                title = "",
                createdAt = null,
                number = number,
                chapterNo = null,
                volumeNo = null,
            )
            .toSChapter(
                actualSeriesId = "s1",
                sourceName = "Kagane",
                scanlatorSource = null,
                language = "en",
            )

    private fun <T> withLocale(tag: String, block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(tag))
        return try {
            block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `comma decimal locale still writes a dot`() {
        val chapter = withLocale("de-DE") { chapter(7.5f) }
        chapter.chapter_txt shouldBe "Ch.7.5"
        getChapterNum(chapter) shouldBe 7.5f
    }

    @Test
    fun `arabic locale still writes ascii digits`() {
        val chapter = withLocale("ar-EG") { chapter(12f) }
        chapter.chapter_txt shouldBe "Ch.12"
        getChapterNum(chapter) shouldBe 12f
    }
}
