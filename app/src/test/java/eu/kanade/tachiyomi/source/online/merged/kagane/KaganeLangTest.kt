package eu.kanade.tachiyomi.source.online.merged.kagane

import io.kotest.matchers.shouldBe
import org.junit.Test

class KaganeLangTest {

    @Test
    fun `chinese chapter languages search with the codes Kagane lists`() {
        KaganeLang.fromMangadexLang("zh") shouldBe "zh"
        KaganeLang.fromMangadexLang("zh-hk") shouldBe "zh-hk"
    }

    @Test
    fun `regional chapter languages still map to Kagane codes`() {
        KaganeLang.fromMangadexLang("es-la") shouldBe "es-419"
        KaganeLang.fromMangadexLang("pt-br") shouldBe "pt-BR"
        KaganeLang.fromMangadexLang("tl") shouldBe "fil"
    }
}
