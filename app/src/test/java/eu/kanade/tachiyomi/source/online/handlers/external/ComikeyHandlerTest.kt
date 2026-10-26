package eu.kanade.tachiyomi.source.online.handlers.external

import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler.ComikeyAlternatePage
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler.ComikeyEpisodeManifest
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler.ComikeyEpisodeManifestMetadata
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler.ComikeyPage
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comikey serves each chapter as a manifest whose page links resolve against the manifest URL. A
 * page link without a query takes the manifest's. Page requests carry the App Check token in the
 * `act` query parameter unless their query already has one, and a blank token is left out.
 */
class ComikeyHandlerTest {

    @Test
    fun `page link keeps its own signed query`() {
        val urls = imageUrls(page("pages/0001.webp?Expires=2&Signature=page1"))

        assertEquals(listOf("$EPISODE/pages/0001.webp?Expires=2&Signature=page1&act=$ACT"), urls)
    }

    @Test
    fun `page link that already carries act gets no second one`() {
        val urls = imageUrls(page("pages/0001.webp?act=server&Signature=page1"))

        assertEquals(listOf("$EPISODE/pages/0001.webp?act=server&Signature=page1"), urls)
    }

    @Test
    fun `page link without a query takes the manifest query`() {
        val urls = imageUrls(page("pages/0001.webp"))

        assertEquals(listOf("$EPISODE/pages/0001.webp?$MANIFEST_QUERY&act=$ACT"), urls)
    }

    @Test
    fun `manifest query that already carries act gets no second one`() {
        val urls =
            imageUrls(
                page("pages/0001.webp"),
                manifestUrl = "$EPISODE/manifest?act=server&Signature=manifest",
            )

        assertEquals(listOf("$EPISODE/pages/0001.webp?act=server&Signature=manifest"), urls)
    }

    @Test
    fun `blank act is not added`() {
        val urls = imageUrls(page("pages/0001.webp?Signature=page1"), act = "")

        assertEquals(listOf("$EPISODE/pages/0001.webp?Signature=page1"), urls)
    }

    @Test
    fun `absolute page link is kept`() {
        val urls = imageUrls(page("https://cdn.comikey.com/p/0001.webp?Signature=page1"))

        assertEquals(listOf("https://cdn.comikey.com/p/0001.webp?Signature=page1&act=$ACT"), urls)
    }

    @Test
    fun `largest webp variant is picked`() {
        val page =
            page(
                "pages/0001.jpg",
                type = "image/jpeg",
                alternate =
                    listOf(
                        ComikeyAlternatePage("pages/0001-m.webp", "image/webp", 1536, 1050),
                        ComikeyAlternatePage("pages/0001-l.webp", "image/webp", 2048, 1400),
                    ),
            )

        assertEquals(
            listOf("$EPISODE/pages/0001-l.webp?$MANIFEST_QUERY&act=$ACT"),
            imageUrls(page),
        )
    }

    @Test
    fun `webtoon picks the widest webp variant`() {
        val page =
            page(
                "pages/0001.jpg",
                type = "image/jpeg",
                alternate =
                    listOf(
                        ComikeyAlternatePage("pages/0001-tall.webp", "image/webp", 4000, 720),
                        ComikeyAlternatePage("pages/0001-wide.webp", "image/webp", 2600, 1080),
                    ),
            )

        assertEquals(
            listOf("$EPISODE/pages/0001-wide.webp?$MANIFEST_QUERY&act=$ACT"),
            imageUrls(page, progression = "ttb"),
        )
    }

    @Test
    fun `page without a webp variant keeps its own image`() {
        val urls = imageUrls(page("pages/0001.jpg", type = "image/jpeg"))

        assertEquals(listOf("$EPISODE/pages/0001.jpg?$MANIFEST_QUERY&act=$ACT"), urls)
    }

    @Test
    fun `manifest page without alternates is read`() {
        val manifest =
            json.decodeFromString<ComikeyEpisodeManifest>(
                """
                    {
                        "metadata": {"readingProgression": "ltr"},
                        "readingOrder": [
                            {"href": "pages/0001.webp", "type": "image/webp", "height": 2048, "width": 1400}
                        ]
                    }
                    """
            )

        assertEquals(
            listOf("$EPISODE/pages/0001.webp?$MANIFEST_QUERY&act=$ACT"),
            imageUrls(manifest),
        )
    }

    @Test
    fun `manifest without pages gives no pages`() {
        val manifest = ComikeyEpisodeManifest(ComikeyEpisodeManifestMetadata("ltr"), emptyList())

        assertEquals(emptyList<String?>(), imageUrls(manifest))
    }

    @Test
    fun `manifest request on any relay host is intercepted`() {
        assertTrue(ComikeyHandler.isRelayManifest("relay-us.epub.rocks", "/v1/ep1/manifest"))
        assertTrue(ComikeyHandler.isRelayManifest("relay-eu.comikey.com", "/v1/ep1/manifest"))
    }

    @Test
    fun `relay host is matched in any letter case`() {
        assertTrue(ComikeyHandler.isRelayManifest("RELAY-US.EPUB.ROCKS", "/v1/ep1/manifest"))
        assertTrue(ComikeyHandler.isRelayManifest("Relay-Eu.Comikey.Com", "/v1/ep1/manifest"))
    }

    @Test
    fun `other requests are not intercepted`() {
        assertFalse(ComikeyHandler.isRelayManifest("comikey.com", "/v1/ep1/manifest"))
        assertFalse(
            ComikeyHandler.isRelayManifest("relay-us.epub.rocks", "/v1/ep1/pages/0001.webp")
        )
        assertFalse(ComikeyHandler.isRelayManifest("relay-us.epub.rocks.example", "/manifest"))
        assertFalse(ComikeyHandler.isRelayManifest(null, "/manifest"))
    }

    private fun imageUrls(
        vararg pages: ComikeyPage,
        progression: String = "ltr",
        manifestUrl: String = MANIFEST_URL,
        act: String = ACT,
    ) =
        imageUrls(
            ComikeyEpisodeManifest(ComikeyEpisodeManifestMetadata(progression), pages.toList()),
            manifestUrl,
            act,
        )

    private fun imageUrls(
        manifest: ComikeyEpisodeManifest,
        manifestUrl: String = MANIFEST_URL,
        act: String = ACT,
    ): List<String?> =
        ComikeyHandler.pageListFromManifest(manifest, manifestUrl.toHttpUrl(), act).map {
            it.imageUrl
        }

    private fun page(
        href: String,
        type: String = "image/webp",
        alternate: List<ComikeyAlternatePage> = emptyList(),
    ) = ComikeyPage(href, type, height = 2048, width = 1400, alternate = alternate)

    private companion object {
        const val EPISODE = "https://relay-us.epub.rocks/v1/ep1"
        const val MANIFEST_QUERY = "Expires=1&Signature=manifest"
        const val MANIFEST_URL = "$EPISODE/manifest?$MANIFEST_QUERY"
        const val ACT = "token"

        val json = Json { ignoreUnknownKeys = true }
    }
}
