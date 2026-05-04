package org.nekomanga.usecases.chapters

import org.junit.Assert.assertEquals
import org.junit.Test

class ParseChapterNameUseCaseTest {

    private val parseChapterName = ParseChapterNameUseCase()

    @Test
    fun `test standard chapter and volume`() {
        val result = parseChapterName("vol.1 ch.2", "15")
        assertEquals("Ch.0002 Vol.0001 Pg.0015", result)
    }

    @Test
    fun `test standard chapter and title`() {
        val result = parseChapterName("ch.3 title", "10")
        assertEquals("Ch.0003 Pg.0010 title", result)
    }

    @Test
    fun `test missing volume`() {
        val result = parseChapterName("ch.4", "5")
        assertEquals("Ch.0004 Pg.0005", result)
    }

    @Test
    fun `test missing chapter`() {
        val result = parseChapterName("vol.5 title", "20")
        assertEquals("Vol.0005 Pg.0020 title", result)
    }

    @Test
    fun `test long title`() {
        val longTitle = "a".repeat(250)
        val result = parseChapterName("ch.6 $longTitle", "1")
        val expectedTitle = "a".repeat(199)
        assertEquals("Ch.0006 Pg.0001 $expectedTitle", result)
    }

    @Test
    fun `test missing title`() {
        val result = parseChapterName("vol.7 ch.8", "9")
        assertEquals("Ch.0008 Vol.0007 Pg.0009", result)
    }

    @Test
    fun `test case insensitivity`() {
        val result = parseChapterName("VoL.9 CH.10", "11")
        assertEquals("Ch.0010 Vol.0009 Pg.0011", result)
    }
}
