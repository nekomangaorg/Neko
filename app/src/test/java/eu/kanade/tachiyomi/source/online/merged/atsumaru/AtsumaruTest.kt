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

    private fun chapter(title: String, number: Float = 1f) =
        ChapterDto(id = "c1", number = number, title = title).toSChapter("m1")

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

    @Test
    fun `title without a number sorts by the source chapter number`() {
        val chapter = chapter("Afterword", number = 74.1f)
        chapter.name shouldBe "Afterword"
        chapter.chapter_txt shouldBe "Ch.74.1"
    }

    @Test
    fun `extra keeps its own name and sorts by the source chapter number`() {
        val chapter = chapter("Extra 1 - ~ love panic ~", number = 2f)
        chapter.name shouldBe "Extra 1 - ~ love panic ~"
        chapter.chapter_txt shouldBe "Ch.2"
    }

    @Test
    fun `numbered prologue and notice keep their own name`() {
        val prologue = chapter("Prologue 16", number = 16f)
        prologue.name shouldBe "Prologue 16"
        prologue.chapter_txt shouldBe "Ch.16"
        val notice = chapter("Notice.110", number = 154f)
        notice.name shouldBe "Notice.110"
        notice.chapter_txt shouldBe "Ch.154"
    }

    @Test
    fun `series words for a chapter still give the chapter number`() {
        chapter("Episode 308 - Clash").name shouldBe "Ch.308 - Clash"
        chapter("Ep. 12").chapter_txt shouldBe "Ch.12"
        chapter("#425").chapter_txt shouldBe "Ch.425"
    }

    @Test
    fun `volume then chapter sets both`() {
        val chapter = chapter("Vol.3 Chapter 19: Dancing in Tears")
        chapter.name shouldBe "Vol.3 Ch.19 - Dancing in Tears"
        chapter.vol shouldBe "3"
        chapter.chapter_txt shouldBe "Ch.19"
    }

    @Test
    fun `volume and chapter numbers lose their leading zeros`() {
        val chapter = chapter("Vol. 01 Ch. 003")
        chapter.name shouldBe "Vol.1 Ch.3"
        chapter.vol shouldBe "1"
    }

    @Test
    fun `volume without a chapter word sorts by the source chapter number`() {
        val chapter = chapter("Volume 14: 4 - Well-Prepared Traps (Part 11)", number = 136f)
        chapter.name shouldBe "Vol.14 - 4 - Well-Prepared Traps (Part 11)"
        chapter.vol shouldBe ""
        chapter.chapter_txt shouldBe "Ch.136"
        chapter("Volume 14 - English Character Cards").name shouldBe
            "Vol.14 - English Character Cards"
    }

    @Test
    fun `whole volume shows the volume and sorts by the source chapter number`() {
        val chapter = chapter("Volume 5", number = 5f)
        chapter.name shouldBe "Vol.5"
        chapter.vol shouldBe ""
        chapter.chapter_txt shouldBe "Ch.5"
    }
}
