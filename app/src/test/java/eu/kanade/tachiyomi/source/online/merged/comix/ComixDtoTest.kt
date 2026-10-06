package eu.kanade.tachiyomi.source.online.merged.comix

import eu.kanade.tachiyomi.util.chapter.getChapterNum
import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.Test

class ComixDtoTest {

    private fun chapter(number: Double) =
        Chapter(id = 1, number = number, name = "Title").toSChapter("slug")

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
        val chapter = withLocale("de-DE") { chapter(7.5) }
        chapter.chapter_txt shouldBe "Ch.7.5"
        chapter.name shouldBe "Ch.7.5 - Title"
        getChapterNum(chapter) shouldBe 7.5f
    }

    @Test
    fun `arabic locale still writes ascii digits`() {
        val chapter = withLocale("ar-EG") { chapter(7.0) }
        chapter.chapter_txt shouldBe "Ch.7"
        getChapterNum(chapter) shouldBe 7f
    }

    @Test
    fun `second decimal is kept`() {
        val chapter = chapter(7.25)
        chapter.chapter_txt shouldBe "Ch.7.25"
        getChapterNum(chapter) shouldBe 7.25f
    }
}
