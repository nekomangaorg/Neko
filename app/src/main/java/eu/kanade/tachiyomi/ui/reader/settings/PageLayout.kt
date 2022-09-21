package eu.kanade.tachiyomi.ui.reader.settings

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class PageLayout(
    val value: Int,
    val webtoonValue: Int,
    @StringRes val stringRes: Int,
    @StringRes private val _fullStringRes: Int? = null,
) {
    SINGLE_PAGE(0, 0, R.string.single_page),
    DOUBLE_PAGES(1, 2, R.string.double_pages),
    AUTOMATIC(2, 3, R.string.automatic, R.string.automatic_orientation),
    SPLIT_PAGES(3, 1, R.string.split_double_pages),

    ;

    @StringRes
    val fullStringRes = _fullStringRes ?: stringRes

    companion object {
        fun fromPreference(preference: Int): PageLayout =
            values().find { it.value == preference } ?: SINGLE_PAGE
    }
}
