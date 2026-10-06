package eu.kanade.tachiyomi.source.online.merged.suwayomi

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class SuwayomiTest {

    private lateinit var suwayomi: Suwayomi

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        val sourceUrl = mockk<Preference<String>> { every { get() } returns "http://localhost" }
        val preferences = mockk<PreferencesHelper>(relaxed = true)
        every { preferences.sourceUrl(any()) } returns sourceUrl
        Injekt.addSingleton(preferences)
        Injekt.addSingleton(mockk<NetworkHelper>(relaxed = true))
        suwayomi = Suwayomi()
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

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
    fun `comma decimal locale still matches a fractional chapter`() {
        val name =
            withLocale("de-DE") {
                suwayomi.sanitizeName("7.5 - Title", 7.5f, Previous(chapter = 7f), 8f)
            }
        name shouldBe Name.Sanitized("Ch.7.5 - Title", "", 7.5f, "Ch.7.5", "Title")
    }

    @Test
    fun `arabic locale still writes ascii digits`() {
        val name =
            withLocale("ar-EG") {
                suwayomi.sanitizeName("7.5 - Title", 7.5f, Previous(chapter = 7f), 8f)
            }
        name shouldBe Name.Sanitized("Ch.7.5 - Title", "", 7.5f, "Ch.7.5", "Title")
    }
}
