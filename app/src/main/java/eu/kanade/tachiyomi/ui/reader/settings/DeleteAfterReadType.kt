package eu.kanade.tachiyomi.ui.reader.settings

import androidx.annotation.StringRes
import org.nekomanga.R

/** Per manga override of the remove after read settings, stored in the manga viewer flags. */
enum class DeleteAfterReadType(
    val prefValue: Int,
    @param:StringRes val stringRes: Int,
    val flagValue: Int = prefValue shl DeleteAfterReadType.SHIFT,
) {
    DEFAULT(0, R.string.default_value),
    NEVER(1, R.string.never),
    ALWAYS(2, R.string.always);

    companion object {
        private const val SHIFT = 0x00000006
        const val MASK = 3 shl SHIFT

        fun fromPreference(preference: Int): DeleteAfterReadType =
            entries.find { it.flagValue == preference } ?: DEFAULT
    }
}
