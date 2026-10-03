package eu.kanade.tachiyomi.source.online.merged.atsumaru

import eu.kanade.tachiyomi.source.online.merged.atsumaru.Atsumaru.Companion.pageImageUrl
import eu.kanade.tachiyomi.source.online.merged.atsumaru.dto.ChapterDto
import io.kotest.matchers.shouldBe
import org.junit.Test

class AtsumaruTest {

    @Test
    fun `relative static path resolves against the cdn host`() {
        pageImageUrl("/static/pages/sVC2A/9L82jefe/28.webp") shouldBe
            "https://cdn.atsu.moe/static/pages/sVC2A/9L82jefe/28.webp"
    }

    @Test
    fun `relative path without static prefix resolves against the cdn host`() {
        pageImageUrl("pages/sVC2A/9L82jefe/0.webp") shouldBe
            "https://cdn.atsu.moe/static/pages/sVC2A/9L82jefe/0.webp"
    }

    @Test
    fun `absolute url passes through`() {
        pageImageUrl("https://other.example/img/1.webp") shouldBe "https://other.example/img/1.webp"
    }

    @Test
    fun `http url is upgraded to https`() {
        pageImageUrl("http://other.example/img/1.webp") shouldBe "https://other.example/img/1.webp"
    }

    @Test
    fun `protocol relative url gets https`() {
        pageImageUrl("//cdn.atsu.moe/static/pages/a/b/2.webp") shouldBe
            "https://cdn.atsu.moe/static/pages/a/b/2.webp"
    }

    private fun chapter(title: String) =
        ChapterDto(id = "c1", number = 1f, title = title).toSChapter("m1")

    @Test
    fun `dash separator in the title is not doubled`() {
        chapter("Chapter 1 - love affair!_").name shouldBe "Ch.1 - love affair!_"
    }

    @Test
    fun `colon separator in the title becomes a dash`() {
        chapter("Chapter 270: Recollections/After Stories - Part 6: Goodbye").name shouldBe
            "Ch.270 - Recollections/After Stories - Part 6: Goodbye"
    }

    @Test
    fun `separator with no space after the number is dropped`() {
        chapter("Chapter 270-End").name shouldBe "Ch.270 - End"
    }

    @Test
    fun `separator with nothing after it leaves only the number`() {
        chapter("Chapter 5 -").name shouldBe "Ch.5"
    }

    @Test
    fun `stacked separators in the title are all dropped`() {
        chapter("Chapter 442 - - Dok-Ja's Incarnation (4)").name shouldBe
            "Ch.442 - Dok-Ja's Incarnation (4)"
        chapter("Chapter 478: – One Single Fable (2)").name shouldBe "Ch.478 - One Single Fable (2)"
    }

    @Test
    fun `title without a separator keeps its text`() {
        chapter("Chapter  223 {Author Review}").name shouldBe "Ch.223 - {Author Review}"
    }

    @Test
    fun `title starting with dots keeps them`() {
        chapter("Chapter 3 ...and then").name shouldBe "Ch.3 - ...and then"
    }

    @Test
    fun `leading zeros are removed from the chapter number`() {
        val chapter = chapter("Chapter 01 - love affair!_")
        chapter.name shouldBe "Ch.1 - love affair!_"
        chapter.chapter_txt shouldBe "Ch.1"
    }

    @Test
    fun `leading zeros are removed from a decimal chapter number`() {
        chapter("Chapter 007.50").chapter_txt shouldBe "Ch.7.5"
    }

    @Test
    fun `chapter zero stays zero`() {
        chapter("Chapter 0").chapter_txt shouldBe "Ch.0"
        chapter("Chapter 00.5").chapter_txt shouldBe "Ch.0.5"
    }

    @Test
    fun `title without a number passes through`() {
        chapter("Prologue").name shouldBe "Prologue"
    }
}
