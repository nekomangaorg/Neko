package eu.kanade.tachiyomi.ui.reader.settings

import androidx.annotation.StringRes
import org.nekomanga.R

enum class ReaderSliderPosition(
    val prefValue: Int,
    @param:StringRes val stringRes: Int,
) {
    LEFT(0, R.string.left_align),
    HORIZONTAL(1, R.string.horizontal),
    RIGHT(2, R.string.right_align);

    companion object {
        val DEFAULT = RIGHT

        fun fromPreference(preference: Int?): ReaderSliderPosition =
            entries.find { it.prefValue == preference } ?: DEFAULT

        fun fromSpinner(position: Int?): ReaderSliderPosition =
            entries.find { it.prefValue == position } ?: DEFAULT
    }
}
