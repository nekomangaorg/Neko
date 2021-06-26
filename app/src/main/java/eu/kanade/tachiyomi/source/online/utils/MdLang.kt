package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.R

enum class MdLang(val lang: String, val prettyPrint: String, val iconResId: Int = 0) {
    ENGLISH("en", "English", R.drawable.ic_flag_us),
    JAPANESE("ja", "Japanese", R.drawable.ic_flag_jp),
    POLISH("pl", "Polish", R.drawable.ic_flag_pl),
    SERBO_CROATIAN("sh", "Serbo-Croatian", R.drawable.ic_flag_rs),
    DUTCH("nl", "Dutch", R.drawable.ic_flag_nl),
    ITALIAN("it", "Italian", R.drawable.ic_flag_it),
    RUSSIAN("ru", "Russian", R.drawable.ic_flag_ru),
    GERMAN("de", "German", R.drawable.ic_flag_de),
    HUNGARIAN("hu", "Hungarian", R.drawable.ic_flag_hu),
    FRENCH("fr", "French", R.drawable.ic_flag_fr),
    FINNISH("fi", "Finnish", R.drawable.ic_flag_fi),
    VIETNAMESE("vi", "Vietnamese", R.drawable.ic_flag_vn),
    GREEK("el", "Greek", R.drawable.ic_flag_gr),
    BULGARIAN("bg", "Bulgarian", R.drawable.ic_flag_bg),
    SPANISH_ES("es", "Spanish (Es)", R.drawable.ic_flag_es),
    PORTUGUESE_BR("pt-br", "Portuguese (Br)", R.drawable.ic_flag_br),
    PORTUGUESE("pt", "Portuguese (Pt)", R.drawable.ic_flag_pt),
    SWEDISH("sv", "Swedish", R.drawable.ic_flag_se),
    ARABIC("ar", "Arabic", R.drawable.ic_flag_sa),
    DANISH("da", "Danish", R.drawable.ic_flag_dk),
    CHINESE_SIMPLIFIED("zh", "Chinese (Simp)", R.drawable.ic_flag_cn),
    BENGALI("bn", "Bengali", R.drawable.ic_flag_bd),
    ROMANIAN("ro", "Romanian", R.drawable.ic_flag_ro),
    CZECH("cs", "Czech", R.drawable.ic_flag_cz),
    MONGOLIAN("mn", "Mongolian", R.drawable.ic_flag_mn),
    TURKISH("tr", "Turkish", R.drawable.ic_flag_tr),
    INDONESIAN("id", "Indonesian", R.drawable.ic_flag_id),
    KOREAN("ko", "Korean", R.drawable.ic_flag_kr),
    SPANISH_LATAM("es-la", "Spanish (LATAM)", R.drawable.ic_flag_mx),
    PERSIAN("fa", "Persian", R.drawable.ic_flag_ir),
    MALAY("ms", "Malay", R.drawable.ic_flag_my),
    THAI("th", "Thai", R.drawable.ic_flag_th),
    CATALAN("ca", "Catalan"),
    FILIPINO("tl", "Filipino", R.drawable.ic_flag_ph),
    CHINESE_TRAD("zh-hk", "Chinese (Trad)", R.drawable.ic_flag_hk),
    UKRAINIAN("uk", "Ukrainian", R.drawable.ic_flag_ua),
    BURMESE("my", "Burmese", R.drawable.ic_flag_mm),
    LINTHUANIAN("lt", "Lithuanian", R.drawable.ic_flag_lt),
    HEBREW("he", "Hebrew", R.drawable.ic_flag_il),
    HINDI("hi", "Hindi", R.drawable.ic_flag_in),
    NORWEGIAN("no", "Norwegian", R.drawable.ic_flag_no),
    OTHER("NULL", "Other");

    companion object {
        fun fromIsoCode(isoCode: String): MdLang? =
            values().firstOrNull {
                it.lang == isoCode
            }
    }
}
