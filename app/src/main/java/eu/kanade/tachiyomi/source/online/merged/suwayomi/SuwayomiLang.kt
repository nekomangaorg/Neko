package eu.kanade.tachiyomi.source.online.merged.suwayomi

enum class SuwayomiLang(val suwayomi: String, val mangadex: String) {
    ALL("all",""),
    CHINESE_SIMPLIFIED("zh-Hans", "zh"),
    CHINESE_TRADITIONAL("zh-Hant", "zh-hk"),
    FILIPINO("fil", "tl"),
    PORTUGUESE_BRAZIL("pt-BR", "pt-br"),
    SPANISH_LATAM("es-419", "es-la");

    companion object {
        fun fromSuwayomiLang(lang: String): String =
            entries.firstOrNull { it.suwayomi == lang }?.mangadex ?: lang
    }
}
