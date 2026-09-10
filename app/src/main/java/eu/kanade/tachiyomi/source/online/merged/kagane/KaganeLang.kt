package eu.kanade.tachiyomi.source.online.merged.kagane

enum class KaganeLang(val kagane: String, val mangadex: String) {
    CHINESE_SIMPLIFIED("zh-Hans", "zh"),
    CHINESE_TRADITIONAL("zh-Hant", "zh-hk"),
    FILIPINO("fil", "tl"),
    PORTUGUESE_BRAZIL("pt-BR", "pt-br"),
    SPANISH_LATAM("es-419", "es-la");

    companion object {
        fun fromMangadexLang(lang: String): String =
            entries.firstOrNull { it.mangadex == lang }?.kagane ?: lang

        fun fromKaganeLang(lang: String): String =
            entries.firstOrNull { it.kagane == lang }?.mangadex ?: lang
    }
}
