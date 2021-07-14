package eu.kanade.tachiyomi.ui.reader.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.next

enum class ReadingModeType(val prefValue: Int, @StringRes val stringRes: Int, @DrawableRes val iconRes: Int) {
    DEFAULT(0, R.string.default_value, R.drawable.ic_reader_default_24dp),
    LEFT_TO_RIGHT(1, R.string.left_to_right_viewer, R.drawable.ic_reader_ltr_24dp),
    RIGHT_TO_LEFT(2, R.string.right_to_left_viewer, R.drawable.ic_reader_rtl_24dp),
    VERTICAL(3, R.string.vertical_viewer, R.drawable.ic_reader_vertical_24dp),
    WEBTOON(4, R.string.webtoon, R.drawable.ic_reader_webtoon_24dp),
    CONTINUOUS_VERTICAL(5, R.string.continuous_vertical, R.drawable.ic_reader_continuous_vertical_24dp),
    ;

    @Suppress("RemoveRedundantQualifierName")
    val flagValue = prefValue shl ReadingModeType.SHIFT

    companion object {
        fun fromPreference(preference: Int): ReadingModeType = values().find { it.flagValue == preference } ?: DEFAULT
        private const val SHIFT = 0x00000000
        const val MASK = 7 shl SHIFT

        fun getNextReadingMode(preference: Int): ReadingModeType {
            val current = fromPreference(preference)
            return current.next()
        }

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode == LEFT_TO_RIGHT || mode == RIGHT_TO_LEFT || mode == VERTICAL
        }

        fun isWebtoonType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode == WEBTOON || mode == CONTINUOUS_VERTICAL
        }

        fun fromSpinner(position: Int?) = values().find { value -> value.prefValue == position } ?: DEFAULT
    }
}
