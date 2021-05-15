package eu.kanade.tachiyomi.source.online.utils

enum class MdLang(val lang: String, val prettyPrint: String) {
    ENGLISH("en", "English"),
    JAPANESE("jp", "Japanese"),
    POLISH("pl", "Polish"),
    SERBO_CROATIAN("rs", "Serbo-Croatian"),
    DUTCH("nl", "Dutch"),
    ITALIAN("it", "IT"),
    RUSSIAN("ru", "Russian"),
    GERMAN("de", "German"),
    HUNGARIAN("hu", "Hungarian"),
    FRENCH("fr", "French"),
    FINNISH("fi", "Finnish"),
    VIETNAMESE("vn", "Vietnamese"),
    GREEK("gr", "Greek"),
    BULGARIAN("bg", "BULGARIN"),
    SPANISH_ES("es", "Spanish (Es)"),
    PORTUGUESE_BR("br", "Portuguese (Br)"),
    PORTUGUESE("pt", "Portuguese (Pt)"),
    SWEDISH("se", "Swedish"),
    ARABIC("sa", "Arabic"),
    DANISH("dk", "Danish"),
    CHINESE_SIMPLIFIED("cn", "Chinese (Simp)"),
    BENGALI("bd", "Bengali"),
    ROMANIAN("ro", "Romanian"),
    CZECH("cz", "Czech"),
    MONGOLIAN("mn", "Mongolian"),
    TURKISH("tr", "Turkish"),
    INDONESIAN("id", "Indonesian"),
    KOREAN("kr", "Korean"),
    SPANISH_LATAM("mx", "Spanish (LATAM)"),
    PERSIAN("ir", "Persian"),
    MALAY("my", "Malay"),
    THAI("th", "Thai"),
    CATALAN("ct", "Catalan"),
    FILIPINO("ph", "Filipino"),
    CHINESE_TRAD("hk", "Chinese (Trad)"),
    UKRAINIAN("ua", "Ukrainian"),
    BURMESE("mm", "Burmese"),
    LINTHUANIAN("lt", "Lithuanian"),
    HEBREW("il", "Hebrew"),
    HINDI("in", "Hindi"),
    NORWEGIAN("no", "Norwegian")
    ;

    companion object {
        fun fromIsoCode(isoCode: String): MdLang? =
            values().firstOrNull {
                it.lang == isoCode
            }
    }
}
