package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.shouldBe
import java.util.Locale
import org.junit.Test

class NumberExtensionsTest {

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
    fun `whole numbers have no fraction`() {
        7f.formatChapterNumber() shouldBe "7"
        0f.formatChapterNumber() shouldBe "0"
    }

    @Test
    fun `fractions keep up to three decimals`() {
        7.5f.formatChapterNumber() shouldBe "7.5"
        7.25f.formatChapterNumber() shouldBe "7.25"
        7.125f.formatChapterNumber() shouldBe "7.125"
        12.1f.formatChapterNumber() shouldBe "12.1"
        0.5f.formatChapterNumber() shouldBe "0.5"
    }

    @Test
    fun `large numbers are not grouped`() {
        1000.5f.formatChapterNumber() shouldBe "1000.5"
    }

    @Test
    fun `output does not depend on the default locale`() {
        withLocale("de-DE") { 1000.5f.formatChapterNumber() } shouldBe "1000.5"
        withLocale("ar-EG") { 12.5f.formatChapterNumber() } shouldBe "12.5"
        withLocale("fa-IR") { 7f.formatChapterNumber() } shouldBe "7"
    }
}
