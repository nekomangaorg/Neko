package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.R

enum class MdLang(val lang: String, val prettyPrint: String, val iconResId: Int) {
    ARABIC("ar", "Arabic", R.drawable.ic_flag_sa),
    AZERBAIJANI("az", "Azerbaijani", R.drawable.ic_flag_az),
    BENGALI("bn", "Bengali", R.drawable.ic_flag_bd),
    BULGARIAN("bg", "Bulgarian", R.drawable.ic_flag_bg),
    BURMESE("my", "Burmese", R.drawable.ic_flag_mm),
    CATALAN("ca", "Catalan", R.drawable.ic_flag_ca),
    CHINESE_SIMPLIFIED("zh", "Chinese (Simp)", R.drawable.ic_flag_cn),
    CHINESE_TRAD("zh-hk", "Chinese (Trad)", R.drawable.ic_flag_hk),
    CZECH("cs", "Czech", R.drawable.ic_flag_cz),
    DANISH("da", "Danish", R.drawable.ic_flag_dk),
    DUTCH("nl", "Dutch", R.drawable.ic_flag_nl),
    ENGLISH("en", "English", R.drawable.ic_flag_us),
    FILIPINO("tl", "Filipino", R.drawable.ic_flag_ph),
    FINNISH("fi", "Finnish", R.drawable.ic_flag_fi),
    FRENCH("fr", "French", R.drawable.ic_flag_fr),
    GERMAN("de", "German", R.drawable.ic_flag_de),
    GREEK("el", "Greek", R.drawable.ic_flag_gr),
    HEBREW("he", "Hebrew", R.drawable.ic_flag_il),
    HINDI("hi", "Hindi", R.drawable.ic_flag_in),
    HUNGARIAN("hu", "Hungarian", R.drawable.ic_flag_hu),
    INDONESIAN("id", "Indonesian", R.drawable.ic_flag_id),
    ITALIAN("it", "Italian", R.drawable.ic_flag_it),
    JAPANESE("ja", "Japanese", R.drawable.ic_flag_jp),
    KOREAN("ko", "Korean", R.drawable.ic_flag_kr),
    LINTHUANIAN("lt", "Lithuanian", R.drawable.ic_flag_lt),
    MALAY("ms", "Malay", R.drawable.ic_flag_my),
    MONGOLIAN("mn", "Mongolian", R.drawable.ic_flag_mn),
    NORWEGIAN("no", "Norwegian", R.drawable.ic_flag_no),
    PERSIAN("fa", "Persian", R.drawable.ic_flag_ir),
    POLISH("pl", "Polish", R.drawable.ic_flag_pl),
    PORTUGUESE("pt", "Portuguese (Pt)", R.drawable.ic_flag_pt),
    PORTUGUESE_BR("pt-br", "Portuguese (Br)", R.drawable.ic_flag_br),
    ROMANIAN("ro", "Romanian", R.drawable.ic_flag_ro),
    RUSSIAN("ru", "Russian", R.drawable.ic_flag_ru),
    SERBO_CROATIAN("sh", "Serbo-Croatian", R.drawable.ic_flag_rs),
    SLOVAK("sk", "Slovak", R.drawable.ic_flag_sk),
    SPANISH_ES("es", "Spanish (Es)", R.drawable.ic_flag_es),
    SPANISH_LATAM("es-la", "Spanish (LATAM)", R.drawable.ic_flag_mx),
    SWEDISH("sv", "Swedish", R.drawable.ic_flag_se),
    THAI("th", "Thai", R.drawable.ic_flag_th),
    TURKISH("tr", "Turkish", R.drawable.ic_flag_tr),
    UKRAINIAN("uk", "Ukrainian", R.drawable.ic_flag_ua),
    VIETNAMESE("vi", "Vietnamese", R.drawable.ic_flag_vn),
    OTHER("NULL", "Other", R.drawable.ic_flag_other),
    ;

    companion object {
        fun fromIsoCode(isoCode: String): MdLang? =
            values().firstOrNull {
                it.lang == isoCode
            }
    }
}
