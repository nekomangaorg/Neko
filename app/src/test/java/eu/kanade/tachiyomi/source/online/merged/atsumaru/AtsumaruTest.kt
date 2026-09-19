package eu.kanade.tachiyomi.source.online.merged.atsumaru

import eu.kanade.tachiyomi.source.online.merged.atsumaru.Atsumaru.Companion.pageImageUrl
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
}
