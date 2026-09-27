package eu.kanade.tachiyomi.source.online.merged.mangaball

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import okio.Buffer
import org.junit.Test

class MangaBallTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val titles by lazy { json.decodeFromString<SearchResponse>(SEARCH).data }

    private val chapters by lazy {
        json.decodeFromString<ChapterListResponse>(CHAPTERS).data.associateBy { it.id }
    }

    @Test
    fun `search url asks for every enabled language in one parameter`() {
        val url = MangaBall.searchUrl("  one piece ", listOf("en", "vi"))

        url.host shouldBe "mangaball.com"
        url.encodedPath shouldBe "/api/v1/title/search-advanced"
        url.queryParameter("keyword") shouldBe "one piece"
        url.queryParameterValues("translated_language") shouldBe listOf("en,vi")
        url.queryParameter("sort_by") shouldBe null
    }

    @Test
    fun `search result keeps the slug and id url shape of existing merges`() {
        val manga = titles[0].toSManga()

        manga.url shouldBe "one-piece-68515540702284f8341784c8"
        manga.title shouldBe "One Piece"
        manga.thumbnail_url shouldBe
            "https://bulbasaur.poke-black-and-white.net/covers/68515540702284f8341784c8/cover_1752071224451.webp"
    }

    @Test
    fun `cover path with a backslash wins over the mirrors`() {
        titles[1].toSManga().thumbnail_url shouldBe
            "https://bulbasaur.poke-black-and-white.net/covers/6853838d9f4d36c6be7869b4/cover_1754638019639.jpg"
    }

    @Test
    fun `title without a cover uses its file url`() {
        titles[2].toSManga().thumbnail_url shouldBe "https://cdn.mangaupdates.com/image/i529923.jpg"
    }

    @Test
    fun `title without a slug is stored by its id`() {
        json
            .decodeFromString<SearchResponse>("""{"data":[{"id":"a","name":"X"}]}""")
            .data[0]
            .toSManga()
            .url shouldBe "a"
    }

    @Test
    fun `blank cover candidates are skipped`() {
        val search =
            """{"data":[{"id":"a","slug":"x","name":"X","image":""" +
                """{"cover":null,"file":"","cdn_mangadex":" ","cdn_mangaupdate":"https://cdn.mangaupdates.com/image/i1.jpg"}}]}"""

        json.decodeFromString<SearchResponse>(search).data[0].toSManga().thumbnail_url shouldBe
            "https://cdn.mangaupdates.com/image/i1.jpg"
    }

    @Test
    fun `chapter title drops its chapter prefix`() {
        val chapter = chapters.getValue("6a6922e6b617755de80cf24b").toSChapter(listOf("en"))!!

        chapter.url shouldBe "6a6922e6b617755de80cf24b"
        chapter.name shouldBe "Ch.81 - The Floating Continent"
        chapter.chapter_txt shouldBe "Ch.81"
        chapter.chapter_number shouldBe 81f
        chapter.vol shouldBe ""
        chapter.scanlator shouldBe "Manga Ball & Tyranitar"
        chapter.language shouldBe "en"
        chapter.date_upload shouldBe 1785275110991L
    }

    @Test
    fun `chapter named only by its number has no title part`() {
        chapters.getValue("69c1a523e8ded0ca88fc7fa4").toSChapter(listOf("en"))!!.name shouldBe
            "Ch.75"
    }

    @Test
    fun `chapter without a name shows its volume`() {
        val chapter = chapters.getValue("68cad9cdcc2d126b0513f760").toSChapter(listOf("en"))!!

        chapter.name shouldBe "Vol.13 Ch.69"
        chapter.vol shouldBe "Vol.13"
        chapter.scanlator shouldBe "Manga Ball & Raikou"
        chapter.date_upload shouldBe 1758124085000L
    }

    @Test
    fun `decimal chapter number keeps its fraction`() {
        val chapter = chapters.getValue("68caec8f18bc45fda8be2805").toSChapter(listOf("en"))!!

        chapter.name shouldBe "Ch.65.2 - City Eater, the Monster"
        chapter.chapter_number shouldBe 65.2f
    }

    @Test
    fun `chapter in a language that is not enabled is skipped`() {
        chapters.getValue("6a945122c01e2cf095f81bea").toSChapter(listOf("en")) shouldBe null
    }

    @Test
    fun `chapter without a number keeps its whole name for chapter recognition`() {
        val chapter = chapters.getValue("6852d8286c8186610af2a49d").toSChapter(listOf("vi"))!!

        chapter.name shouldBe "Vol.1 Chap 0: Prologue"
        chapter.chapter_txt shouldBe ""
        chapter.chapter_number shouldBe -1f
        chapter.language shouldBe "vi"
        chapter.date_upload shouldBe 1691485402000L
    }

    @Test
    fun `group id that is not an object id is added to the scanlator`() {
        val chapter =
            json
                .decodeFromString<ChapterListResponse>(
                    """{"data":[{"id":"a","lang":"en","number":1,"group":{"id":"lugia","name":"Lugia"}}]}"""
                )
                .data[0]
                .toSChapter(listOf("en"))!!

        chapter.scanlator shouldBe "Manga Ball & Lugia & (lugia)"
    }

    @Test
    fun `group without a name leaves only the source in the scanlator`() {
        val chapter =
            json
                .decodeFromString<ChapterListResponse>(
                    """{"data":[{"id":"a","lang":"en","number":1,"group":{"id":null,"name":null}}]}"""
                )
                .data[0]
                .toSChapter(listOf("en"))!!

        chapter.scanlator shouldBe "Manga Ball"
    }

    @Test
    fun `chapter without a language is skipped`() {
        json
            .decodeFromString<ChapterListResponse>(
                """{"data":[{"id":"a","lang":null,"number":1}]}"""
            )
            .data[0]
            .toSChapter(listOf("en")) shouldBe null
    }

    @Test
    fun `page list keeps the image urls in order`() {
        val pages = json.decodeFromString<ChapterDetailResponse>(PAGES).toPageList()

        pages.map { it.index } shouldBe listOf(0, 1, 2)
        pages.map { it.imageUrl } shouldBe
            listOf(
                "https://chikorita.red-and-blue.net/storage/685164d6702284f83417b6b1/0/774/komiku/id/69bba5ac0fb428c9d4d1d4e6-001.webp",
                "https://chikorita.red-and-blue.net/storage/685164d6702284f83417b6b1/0/774/komiku/id/69bba5ac0fb428c9d4d1d4e6-002.webp",
                "https://chikorita.red-and-blue.net/storage/685164d6702284f83417b6b1/0/774/komiku/id/69bba5ac0fb428c9d4d1d4e6-003.webp",
            )
    }

    @Test
    fun `missing chapter has no pages`() {
        json
            .decodeFromString<ChapterDetailResponse>(
                """{"status":"error","message":"Chapter not found","data":null}"""
            )
            .toPageList() shouldBe emptyList()
    }

    @Test
    fun `chapter list body is json without a charset`() {
        val body = MangaBall.titleIdBody("685164d6702284f83417b6b1")

        body.contentType().toString() shouldBe "application/json"
        Buffer().also { body.writeTo(it) }.readUtf8() shouldBe
            """{"title_id":"685164d6702284f83417b6b1"}"""
    }

    private companion object {
        // Trimmed responses from mangaball.com, fetched 2026-09-27.
        const val SEARCH =
            """
            {"code":200,"status":"success","data":[
            {"_id":"68515540702284f8341784c8","id":"68515540702284f8341784c8","name":"One Piece","slug":"one-piece","image":{"cdn_mangadex":"https://uploads.mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/249fa95b-2214-4ae3-a8f7-77338fe34542.png","cdn_mangaupdate":null,"file":null,"cover":{"name":"cover_1752071224451.webp","path":"68515540702284f8341784c8/cover_1752071224451.webp"}},"status":"ongoing","is18plus":false},
            {"_id":"6853838d9f4d36c6be7869b4","id":"6853838d9f4d36c6be7869b4","name":"One Piece - Damn Summer (Doujinshi)","slug":"one-piece-damn-summer-doujinshi","image":{"cdn_mangadex":"https://uploads.mangadex.org/covers/3e80185a-416e-496f-94a3-423efec8cd50/56717a97-a273-4d6a-a44f-08c82805c156.jpg","cdn_mangaupdate":null,"file":"https://meo.comick.pictures/eBJwE.jpg","cover":{"name":"cover_1754638019639.jpg","path":"6853838d9f4d36c6be7869b4\\cover_1754638019639.jpg"}},"status":"completed","is18plus":true},
            {"_id":"6a376120678c441af9371cf7","id":"6a376120678c441af9371cf7","name":"Basic Attacks Infinitely Stack Health, I Blow Up Kings With a Single Arrow","slug":"basic-attacks-infinitely-stack-health-i-blow-up-kings-with-a-single-arrow","image":{"cdn_mangadex":"","cdn_mangaupdate":"","file":"https://cdn.mangaupdates.com/image/i529923.jpg","cdn_mangaupdates":"https://cdn.mangaupdates.com/image/i529923.jpg","cover":null},"status":"ongoing","is18plus":false}
            ],"pagination":{"total":3,"page":1,"limit":24,"total_pages":1}}
            """

        const val CHAPTERS =
            """
            {"status":"success","data":[
            {"id":"6a6922e6b617755de80cf24b","title_id":"685164d6702284f83417b6b1","lang":"en","name":"Chapter 81: The Floating Continent","number":81,"volume":0,"site":"mangakatana","status":"published","created_at":"2026-07-28T21:45:10.991000","updated_at":"2026-07-28T21:49:45.826000","storage":"cdn_1","chapter_number":81,"group":{"id":"68ca28898e848904a0004d61","_id":"68ca28898e848904a0004d61","name":"Tyranitar","slug":"tyranitar"},"group_name":"Tyranitar","group_id":"68ca28898e848904a0004d61"},
            {"id":"69c1a523e8ded0ca88fc7fa4","title_id":"685164d6702284f83417b6b1","lang":"en","name":"Chapter 75","number":75,"volume":0,"site":"mangadex","status":"published","created_at":"2026-03-23T20:40:03.420000","updated_at":"2026-03-23T21:04:04.713000","storage":"cdn_1","chapter_number":75,"group":{"id":"68c7d1dc8e848904a0004d56","_id":"68c7d1dc8e848904a0004d56","name":"Raikou","slug":"raikou"},"group_name":"Raikou","group_id":"68c7d1dc8e848904a0004d56"},
            {"id":"68cad9cdcc2d126b0513f760","title_id":"685164d6702284f83417b6b1","lang":"en","name":null,"number":69,"volume":13,"site":"mangadex","created_at":"2025-09-17T15:48:05","updated_at":"2026-03-11T22:02:45.197000","status":"published","storage":"cdn_1","chapter_number":69,"group":{"id":"68c7d1dc8e848904a0004d56","_id":"68c7d1dc8e848904a0004d56","name":"Raikou","slug":"raikou"},"group_name":"Raikou","group_id":"68c7d1dc8e848904a0004d56"},
            {"id":"68caec8f18bc45fda8be2805","title_id":"685164d6702284f83417b6b1","lang":"en","name":"Chapter 65.2: City Eater, the Monster","number":65.2,"volume":0,"site":"mangakatana","status":"published","created_at":"2025-09-17T17:14:55.269000","updated_at":"2026-03-05T20:00:47.532000","storage":"cdn_1","chapter_number":65.2,"group":{"id":"68ca28898e848904a0004d61","_id":"68ca28898e848904a0004d61","name":"Tyranitar","slug":"tyranitar"},"group_name":"Tyranitar","group_id":"68ca28898e848904a0004d61"},
            {"id":"6a945122c01e2cf095f81bea","title_id":"685164d6702284f83417b6b1","lang":"vi","name":"Chương 85.5 ②","number":85.5,"volume":0,"site":"daomeoden","status":"published","created_at":"2026-08-30T15:49:54.088000","updated_at":"2026-08-30T15:53:04.923000","storage":"cdn_2","chapter_number":85.5,"group":{"id":"68b1ca9bbe39944f6a139a3e","_id":"68b1ca9bbe39944f6a139a3e","name":"Rayquaza","slug":"rayquaza"},"group_name":"Rayquaza","group_id":"68b1ca9bbe39944f6a139a3e"},
            {"id":"6852d8286c8186610af2a49d","title_id":"685164d6702284f83417b6b1","lang":"vi","name":"Chap 0: Prologue","number":null,"volume":1,"created_at":"2023-08-08T09:03:22","updated_at":"2025-07-26T20:53:58.427000","site":"mangadex","status":"published","storage":"cdn_1","chapter_number":null,"group":{"id":"68c7d1dc8e848904a0004d56","_id":"68c7d1dc8e848904a0004d56","name":"Raikou","slug":"raikou"},"group_name":"Raikou","group_id":"68c7d1dc8e848904a0004d56"}
            ]}
            """

        const val PAGES =
            """
            {"status":"success","code":200,"data":{"chapter":{"id":"69bba5ac0fb428c9d4d1d4e6","title_id":"685164d6702284f83417b6b1","lang":"id","pages":[
            "https://chikorita.red-and-blue.net/storage/685164d6702284f83417b6b1/0/774/komiku/id/69bba5ac0fb428c9d4d1d4e6-001.webp",
            "https://chikorita.red-and-blue.net/storage/685164d6702284f83417b6b1/0/774/komiku/id/69bba5ac0fb428c9d4d1d4e6-002.webp",
            "https://chikorita.red-and-blue.net/storage/685164d6702284f83417b6b1/0/774/komiku/id/69bba5ac0fb428c9d4d1d4e6-003.webp"
            ]}}}
            """
    }
}
