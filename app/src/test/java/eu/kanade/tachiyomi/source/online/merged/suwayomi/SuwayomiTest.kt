package eu.kanade.tachiyomi.source.online.merged.suwayomi

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.serialization.json.Json
import okhttp3.RequestBody
import okio.Buffer
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

    private fun RequestBody.bodyText(): String = Buffer().also { writeTo(it) }.readUtf8()

    @Test
    fun `comma decimal locale still matches a fractional chapter`() {
        val name =
            withLocale("de-DE") {
                suwayomi.sanitizeName("7.5 - Title", 7.5f, Previous(chapter = 7f), 8f)
            }
        name shouldBe Name.Sanitized("Ch.7.5 - Title", "", 7.5f, "Ch.7.5", "Title")
    }

    @Test
    fun `zero padded chapter number is normalized`() {
        suwayomi.sanitizeName("07. Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `multi digit zero padded chapter number is normalized`() {
        suwayomi.sanitizeName("012. Title", 12f, Previous(), null) shouldBe
            Name.Sanitized("Ch.12 - Title", "", 12f, "Ch.12", "Title")
    }

    @Test
    fun `padded fractional chapter number is normalized`() {
        suwayomi.sanitizeName("07.5 - Title", 7.5f, Previous(chapter = 7f), 8f) shouldBe
            Name.Sanitized("Ch.7.5 - Title", "", 7.5f, "Ch.7.5", "Title")
    }

    @Test
    fun `unpadded chapter number still matches`() {
        suwayomi.sanitizeName("7. Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `sub one fractional chapter number still matches`() {
        suwayomi.sanitizeName("0.5 - Title", 0.5f, Previous(), null) shouldBe
            Name.Sanitized("Ch.0.5 - Title", "", 0.5f, "Ch.0.5", "Title")
    }

    @Test
    fun `padded number followed by another digit is not a match`() {
        // "071. Title" is chapter 71, not chapter 7 titled "1. Title"
        suwayomi.sanitizeName("071. Title", 7f, Previous(), null) shouldBe Name.NotSane
    }

    @Test
    fun `unpadded number followed by a digit is not a match`() {
        suwayomi.sanitizeName("17", 7f, Previous(), null) shouldBe Name.NotSane
    }

    @Test
    fun `padded number directly followed by text is still a match`() {
        suwayomi.sanitizeName("07Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `padded fractional number followed by another digit is not a match`() {
        suwayomi.sanitizeName("071.5 - Title", 7.5f, Previous(chapter = 7f), 8f) shouldBe
            Name.NotSane
    }

    @Test
    fun `number after a prefix followed by another digit is not sane`() {
        // Must not strip the "7" out of "071" and report the title as "1. Title"
        suwayomi.sanitizeName("Ch.071. Title", 7f, Previous(), null) shouldBe Name.NotSane
    }

    @Test
    fun `padded number after a prefix is still a match`() {
        suwayomi.sanitizeName("Ch.07. Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `fractional title number against whole chapter is not a match`() {
        suwayomi.sanitizeName("071.5 Title", 71f, Previous(), null) shouldBe Name.NotSane
    }

    @Test
    fun `fractional title number after a prefix is not sane`() {
        suwayomi.sanitizeName("Ch.071.5 Title", 71f, Previous(), null) shouldBe Name.NotSane
    }

    @Test
    fun `fractional title number against chapter seven is not a match`() {
        suwayomi.sanitizeName("071.5 Title", 7f, Previous(), null) shouldBe Name.NotSane
    }

    @Test
    fun `number at the very end of the title matches`() {
        suwayomi.sanitizeName("Ch.7", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7", "", 7f, "Ch.7", "")
    }

    @Test
    fun `space delimited padded number is a match`() {
        suwayomi.sanitizeName("07 Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `wide zero padding is a match`() {
        suwayomi.sanitizeName("007. Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `padded number after the hash prefix is a match`() {
        suwayomi.sanitizeName("#07. Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `padded number after the chapter prefix is a match`() {
        suwayomi.sanitizeName("Chapter 07. Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `dash delimited padded number after a prefix is a match`() {
        suwayomi.sanitizeName("Ch.07 - Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `chapter zero is a match`() {
        suwayomi.sanitizeName("0. Title", 0f, Previous(), null) shouldBe
            Name.Sanitized("Ch.0 - Title", "", 0f, "Ch.0", "Title")
    }

    @Test
    fun `padded fractional number at the end of the title is a match`() {
        suwayomi.sanitizeName("07.5", 7.5f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7.5", "", 7.5f, "Ch.7.5", "")
    }

    @Test
    fun `tab delimited padded number is a match`() {
        suwayomi.sanitizeName("07\tTitle", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `whitespace inside a kagane source tag is trimmed`() {
        suwayomi.splitSourceGroup("Berserk [ MangaPlus ]", "Kagane") shouldBe
            ("[MangaPlus] Berserk" to "MangaPlus")
    }

    @Test
    fun `extra whitespace before a kagane source tag is absorbed`() {
        suwayomi.splitSourceGroup("Berserk  [MangaPlus]", "Kagane") shouldBe
            ("[MangaPlus] Berserk" to "MangaPlus")
    }

    @Test
    fun `nested brackets are not treated as a kagane source tag`() {
        suwayomi.splitSourceGroup("A [B[C]]", "Kagane") shouldBe ("A [B[C]]" to null)
    }

    @Test
    fun `trailing whitespace after a kagane source tag prevents a match`() {
        suwayomi.splitSourceGroup("Berserk [MangaPlus] ", "Kagane") shouldBe
            ("Berserk [MangaPlus] " to null)
    }

    @Test
    fun `zero width space suffixed source name is not an aggregator`() {
        // fetchChapters must pass the raw name, not the one tagged for filteredBySource()
        suwayomi.splitSourceGroup("Berserk [MangaPlus]", "Kagane\u200B") shouldBe
            ("Berserk [MangaPlus]" to null)
    }

    @Test
    fun `leading emoji is stripped before parsing`() {
        suwayomi.sanitizeName("🔥 Chapter 7 - Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `volume prefix becomes the volume field`() {
        suwayomi.sanitizeName(
            "Volume 2 Chapter 1 - Title",
            1f,
            Previous(5f, volume = "1"),
            null,
        ) shouldBe Name.Sanitized("Vol.2 Ch.1 - Title", "2", 1f, "Ch.1", "Title")
    }

    @Test
    fun `season prefix becomes the volume field`() {
        // A "Season" title only reaches the volume parser when the chapter is not behind the
        // previous one, otherwise "season" is caught as an edge case word first
        suwayomi.sanitizeName(
            "Season 2 Chapter 7 - Title",
            7f,
            Previous(5f, volume = "1"),
            null,
        ) shouldBe Name.Sanitized("Vol.2 Ch.7 - Title", "2", 7f, "Ch.7", "Title")
    }

    @Test
    fun `single letter season prefix becomes the volume field`() {
        suwayomi.sanitizeName("S2 Chapter 1 - Title", 1f, Previous(5f, volume = "1"), null) shouldBe
            Name.Sanitized("Vol.2 Ch.1 - Title", "2", 1f, "Ch.1", "Title")
    }

    @Test
    fun `parenthesised season prefix is delimited by the closing paren`() {
        suwayomi.sanitizeName(
            "(S2) Chapter 1 - Title",
            1f,
            Previous(5f, volume = "1"),
            null,
        ) shouldBe Name.Sanitized("Vol.2 Ch.1 - Title", "2", 1f, "Ch.1", "Title")
    }

    @Test
    fun `bracketed season prefix is delimited by the closing bracket`() {
        suwayomi.sanitizeName(
            "[S2] Chapter 1 - Title",
            1f,
            Previous(5f, volume = "1"),
            null,
        ) shouldBe Name.Sanitized("Vol.2 Ch.1 - Title", "2", 1f, "Ch.1", "Title")
    }

    @Test
    fun `same volume as the previous chapter is not sane`() {
        // "S1" against a chapter already in volume 1 gives no new information
        suwayomi.sanitizeName("S1 Chapter 1 - Title", 1f, Previous(5f, volume = "1"), null) shouldBe
            Name.NotSane
    }

    @Test
    fun `epilogue increments off the previous chapter`() {
        suwayomi.sanitizeName("Epilogue", 0f, Previous(5f), null) shouldBe
            Name.Sanitized("Ch.6 - Epilogue", "", 6f, "Ch.6", "Epilogue", false)
    }

    @Test
    fun `season announcement becomes a half chapter`() {
        suwayomi.sanitizeName("Season 2 Announcement", 0f, Previous(5f), null) shouldBe
            Name.Sanitized(
                "Ch.5.5 - Season 2 Announcement",
                "",
                5f,
                "Ch.5.5",
                "Season 2 Announcement",
                false,
            )
    }

    @Test
    fun `afterword becomes a half chapter`() {
        suwayomi.sanitizeName("Afterword", 0f, Previous(5f), null) shouldBe
            Name.Sanitized("Ch.5.5 - Afterword", "", 5f, "Ch.5.5", "Afterword", false)
    }

    @Test
    fun `special extra numbered edge case increments off the previous chapter`() {
        suwayomi.sanitizeName("Special SE2", 0f, Previous(5f), null) shouldBe
            Name.Sanitized("Ch.6 - Special SE2", "", 6f, "Ch.6", "Special SE2", false)
    }

    @Test
    fun `spaced extra numbered edge case is recognised`() {
        suwayomi.sanitizeName("SE 2", 0f, Previous(5f), null) shouldBe
            Name.Sanitized("Ch.6 - SE 2", "", 6f, "Ch.6", "SE 2", false)
    }

    @Test
    fun `a lone season edge case is marked as a premiere`() {
        suwayomi.sanitizeName("Season 2", 0f, Previous(5f), null) shouldBe
            Name.Sanitized("Vol.2 Ch.6 - Season 2", "2", 6f, "Ch.6", "Season 2", true)
    }

    @Test
    fun `a previous premiere suppresses the increment`() {
        // Consecutive edge cases must all land on the same chapter number
        suwayomi.sanitizeName("Epilogue", 0f, Previous(5f, premiere = true), null) shouldBe
            Name.Sanitized("Ch.5 - Epilogue", "", 5f, "Ch.5", "Epilogue", false)
    }

    @Test
    fun `an edge case carries the previous volume through`() {
        suwayomi.sanitizeName("Epilogue", 0f, Previous(5f, volume = "3"), null) shouldBe
            Name.Sanitized("Vol.3 Ch.6 - Epilogue", "3", 6f, "Ch.6", "Epilogue", false)
    }

    @Test
    fun `a half chapter interstitial stays in the previous volume`() {
        // The ".5" does not increment the chapter, so it must not move the volume either
        suwayomi.sanitizeName(
            "Season 2 Announcement",
            0f,
            Previous(5f, volume = "1"),
            null,
        ) shouldBe
            Name.Sanitized(
                "Vol.1 Ch.5.5 - Season 2 Announcement",
                "1",
                5f,
                "Ch.5.5",
                "Season 2 Announcement",
                false,
            )
    }

    @Test
    fun `end is treated as an edge case`() {
        suwayomi.sanitizeName("The End", 0f, Previous(5f), null) shouldBe
            Name.Sanitized("Ch.6 - The End", "", 6f, "Ch.6", "The End", false)
    }

    @Test
    fun `an s prefix that is not a season is not read as a volume`() {
        // "S" is only a season prefix when digits follow, otherwise "Story" would give Vol.tory
        suwayomi.sanitizeName("Story 7 - Title", 7f, Previous(), null) shouldBe Name.NotSane
    }

    @Test
    fun `a leading end tag is removed`() {
        suwayomi.sanitizeName("7 [end]Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `a repeated chapter number is collapsed`() {
        suwayomi.sanitizeName("Chapter 7 Ch. 7 Title", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `renumbered series anchors an edge case before the next chapter`() {
        // Season 2 opens at chapter 1 again, so previous + 1 would put it at chapter 6
        suwayomi.sanitizeName("Season 2", 0f, Previous(5f, volume = "1"), 2f) shouldBe
            Name.Sanitized("Vol.2 Ch.1 - Season 2", "2", 1f, "Ch.1", "Season 2", true)
    }

    @Test
    fun `renumbered series puts a half chapter before the next chapter`() {
        suwayomi.sanitizeName(
            "Season 2 Announcement",
            0f,
            Previous(5f, volume = "1"),
            2f,
        ) shouldBe
            Name.Sanitized(
                "Vol.1 Ch.1.5 - Season 2 Announcement",
                "1",
                1f,
                "Ch.1.5",
                "Season 2 Announcement",
                false,
            )
    }

    @Test
    fun `a fractional next still anchors the edge case`() {
        // next of 1.5 means the chapter being parsed is chapter 1, not previous + 1
        suwayomi.sanitizeName("Season 2", 1f, Previous(5f, volume = "1"), 1.5f) shouldBe
            Name.Sanitized("Vol.2 Ch.1 - Season 2", "2", 1f, "Ch.1", "Season 2", true)
    }

    @Test
    fun `a reset to chapter one keeps the previous anchor`() {
        // next of 1 would anchor on chapter 0, which is not a sane slot, so the run applies
        suwayomi.sanitizeName("Season 2", 0f, Previous(5f, volume = "1"), 1f) shouldBe
            Name.Sanitized("Vol.2 Ch.6 - Season 2", "2", 6f, "Ch.6", "Season 2", true)
    }

    @Test
    fun `prologue becomes chapter zero`() {
        suwayomi.sanitizeName("Prologue", 3f, Previous(5f), null) shouldBe
            Name.Sanitized("Ch.0 - Prologue", "", 0f, "Ch.0", "Prologue")
    }

    @Test
    fun `unsorted chapter inserts a tenth after the previous`() {
        suwayomi.sanitizeName("Extra", 3f, Previous(5f), 4f) shouldBe
            Name.Sanitized("Ch.5.1 - Extra", "", 5.1f, "Ch.5.1", "Extra")
    }

    @Test
    fun `gap between chapters fills the missing number`() {
        suwayomi.sanitizeName("Extra", 3f, Previous(5f), 7f) shouldBe
            Name.Sanitized("Ch.6 - Extra", "", 6f, "Ch.6", "Extra")
    }

    @Test
    fun `unrecoverable unsorted chapter is not sane`() {
        suwayomi.sanitizeName("Mystery", 3f, Previous(5f), 8f) shouldBe Name.NotSane
    }

    @Test
    fun `webtoon chapter note is moved out of the title`() {
        suwayomi.sanitizeName("7 - Title(ch. 7)", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `cbz extension is dropped`() {
        suwayomi.sanitizeName("Ch.7 Title.cbz", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `trailing end tag is removed`() {
        suwayomi.sanitizeName("7 - Title[end]", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `trailing end tag in parens is removed`() {
        suwayomi.sanitizeName("7 - Title (end)", 7f, Previous(), null) shouldBe
            Name.Sanitized("Ch.7 - Title", "", 7f, "Ch.7", "Title")
    }

    @Test
    fun `fetch chapters mutation asks for the manga title`() {
        val body = suwayomi.fetchChaptersFormBuilder(41030).bodyText()
        body shouldBe
            $$"""{"operationName":"FETCH_MANGA_CHAPTERS","query":"mutation FETCH_MANGA_CHAPTERS($input: FetchMangaAndChaptersInput!){fetchMangaAndChapters(input: $input) { manga{title} chapters{id name chapterNumber sourceOrder uploadDate isRead scanlator}}}","variables":{"input":{"id":41030,"fetchChapters":true,"fetchManga":false}}}"""
    }

    @Test
    fun `get chapters query asks for the manga title`() {
        val body = suwayomi.getChaptersFormBuilder(41030).bodyText()
        body shouldBe
            $$"""{"operationName":"GET_MANGA_CHAPTERS","query":"query GET_MANGA_CHAPTERS($mangaId: Int!) {manga(id: $mangaId){title} chapters(filter: {mangaId: {equalTo: $mangaId}}) { nodes {id name chapterNumber sourceOrder uploadDate isRead scanlator}}}","variables":{"mangaId":41030}}"""
    }

    @Test
    fun `mutation response decodes chapters and manga title`() {
        val json = Json { ignoreUnknownKeys = true }
        val response =
            """
            {"data":{"fetchMangaAndChapters":{
              "manga":{"title":"Berserk [MangaPlus]","id":41030},
              "chapters":[{"id":1,"name":"Chapter 7","chapterNumber":7.0,
              "uploadDate":1,"sourceOrder":0,"isRead":false,"scanlator":"group a, group b"}]}}}
            """
                .trimIndent()
        val data =
            json
                .decodeFromString<SuwayomiGraphQLDto<SuwayomiFetchChaptersDto>>(response)
                .data
                ?.fetchMangaAndChapters
        data?.manga?.title shouldBe "Berserk [MangaPlus]"
        data?.chapters?.size shouldBe 1
        data?.chapters?.first()?.scanlator shouldBe "group a, group b"
    }

    @Test
    fun `kagane trailing source tag moves to the front`() {
        suwayomi.splitSourceGroup("Berserk [MangaPlus]", "Kagane") shouldBe
            ("[MangaPlus] Berserk" to "MangaPlus")
    }

    @Test
    fun `kagane title that is only a source tag has no trailing space`() {
        suwayomi.splitSourceGroup("[MangaPlus]", "Kagane") shouldBe ("[MangaPlus]" to "MangaPlus")
    }

    @Test
    fun `kagane title without a source tag is untouched`() {
        suwayomi.splitSourceGroup("Berserk", "Kagane") shouldBe ("Berserk" to null)
    }

    @Test
    fun `only the trailing tag is taken from a kagane title`() {
        suwayomi.splitSourceGroup("A [B] C [D]", "Kagane") shouldBe ("[D] A [B] C" to "D")
    }

    @Test
    fun `brackets in the middle of a kagane title are kept`() {
        suwayomi.splitSourceGroup("A [B] C", "Kagane") shouldBe ("A [B] C" to null)
    }

    @Test
    fun `non kagane sources keep the bracketed title`() {
        suwayomi.splitSourceGroup("Berserk [MangaPlus]", "MangaPlus") shouldBe
            ("Berserk [MangaPlus]" to null)
    }

    @Test
    fun `empty kagane source tag is ignored`() {
        suwayomi.splitSourceGroup("Berserk []", "Kagane") shouldBe ("Berserk []" to null)
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
