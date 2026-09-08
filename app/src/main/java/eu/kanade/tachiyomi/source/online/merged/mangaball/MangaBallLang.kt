package eu.kanade.tachiyomi.source.online.merged.mangaball

/** Maps Neko (MangaDex) language codes onto MangaBall site language codes. */
enum class MangaBallLang(val mangadex: String, val mangaball: List<String>) {
    ENGLISH("en", listOf("en")),
    JAPANESE("ja", listOf("jp")),
    KOREAN("ko", listOf("kr")),
    ARABIC("ar", listOf("ar")),
    BULGARIAN("bg", listOf("bg")),
    BENGALI("bn", listOf("bn")),
    CATALAN("ca", listOf("ca", "ca-ad", "ca-es", "ca-fr", "ca-it", "ca-pt")),
    CZECH("cs", listOf("cs")),
    DANISH("da", listOf("da")),
    GERMAN("de", listOf("de")),
    GREEK("el", listOf("el")),
    SPANISH("es", listOf("es", "es-ar", "es-mx", "es-es", "es-la", "es-419")),
    PERSIAN("fa", listOf("fa")),
    FINNISH("fi", listOf("fi")),
    FRENCH("fr", listOf("fr")),
    HEBREW("he", listOf("he")),
    HINDI("hi", listOf("hi")),
    HUNGARIAN("hu", listOf("hu")),
    INDONESIAN("id", listOf("id")),
    ITALIAN("it", listOf("it", "it-it")),
    MALAY("ms", listOf("ms")),
    NEPALI("ne", listOf("ne")),
    DUTCH("nl", listOf("nl", "nl-be")),
    NORWEGIAN("no", listOf("no")),
    POLISH("pl", listOf("pl")),
    PORTUGUESE_BRAZIL("pt-br", listOf("pt-br", "pt-pt")),
    PORTUGUESE("pt", listOf("pt")),
    ROMANIAN("ro", listOf("ro")),
    RUSSIAN("ru", listOf("ru")),
    SLOVAK("sk", listOf("sk")),
    SERBIAN("sr", listOf("sr", "sr-cyrl")),
    SWEDISH("sv", listOf("sv")),
    TAMIL("ta", listOf("ta")),
    THAI("th", listOf("th", "th-hk", "th-kh", "th-la", "th-my", "th-sg")),
    TURKISH("tr", listOf("tr")),
    UKRAINIAN("uk", listOf("uk")),
    VIETNAMESE("vi", listOf("vi")),
    CHINESE_SIMPLIFIED("zh", listOf("zh", "zh-cn", "zh-hk", "zh-mo", "zh-sg", "zh-tw"));

    companion object {
        fun fromMangadexLang(mangadexLang: String): List<String> =
            entries.firstOrNull { it.mangadex == mangadexLang }?.mangaball.orEmpty()

        fun fromMangaBallLang(mangaballLang: String): String? =
            entries.firstOrNull { mangaballLang in it.mangaball }?.mangadex
    }
}
