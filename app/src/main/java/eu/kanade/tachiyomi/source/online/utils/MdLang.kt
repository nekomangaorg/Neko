package eu.kanade.tachiyomi.source.online.utils

enum class MdLang(val lang: String, val prettyPrint: String) {
    ENGLISH("en", "English"),
    JAPANESE("ja", "Japanese"),
    POLISH("pl", "Polish"),
    SERBO_CROATIAN("sh", "Serbo-Croatian"),
    DUTCH("nl", "Dutch"),
    ITALIAN("it", "Italian"),
    RUSSIAN("ru", "Russian"),
    GERMAN("de", "German"),
    HUNGARIAN("hu", "Hungarian"),
    FRENCH("fr", "French"),
    FINNISH("fi", "Finnish"),
    VIETNAMESE("vi", "Vietnamese"),
    GREEK("el", "Greek"),
    BULGARIAN("bg", "BULGARIN"),
    SPANISH_ES("es", "Spanish (Es)"),
    PORTUGUESE_BR("pt-br", "Portuguese (Br)"),
    PORTUGUESE("pt", "Portuguese (Pt)"),
    SWEDISH("sv", "Swedish"),
    ARABIC("ar", "Arabic"),
    DANISH("da", "Danish"),
    CHINESE_SIMPLIFIED("zh", "Chinese (Simp)"),
    BENGALI("bn", "Bengali"),
    ROMANIAN("ro", "Romanian"),
    CZECH("cs", "Czech"),
    MONGOLIAN("mn", "Mongolian"),
    TURKISH("tr", "Turkish"),
    INDONESIAN("id", "Indonesian"),
    KOREAN("ko", "Korean"),
    SPANISH_LATAM("es-la", "Spanish (LATAM)"),
    PERSIAN("fa", "Persian"),
    MALAY("ms", "Malay"),
    THAI("th", "Thai"),
    CATALAN("ca", "Catalan"),
    FILIPINO("tl", "Filipino"),
    CHINESE_TRAD("zh-hk", "Chinese (Trad)"),
    UKRAINIAN("uk", "Ukrainian"),
    BURMESE("my", "Burmese"),
    LINTHUANIAN("lt", "Lithuanian"),
    HEBREW("he", "Hebrew"),
    HINDI("hi", "Hindi"),
    NORWEGIAN("no", "Norwegian"),
    OTHER("NULL", "Other"),
    ;

    companion object {
        fun fromIsoCode(isoCode: String): MdLang? =
            values().firstOrNull {
                it.lang == isoCode
            }
    }
}
