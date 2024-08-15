package eu.kanade.tachiyomi.util.system

import androidx.annotation.StringRes
import org.nekomanga.R

enum class SideNavMode(val prefValue: Int, @StringRes val stringRes: Int) {
    DEFAULT(0, R.string.default_behavior),
    NEVER(1, R.string.never),
    ALWAYS(2, R.string.always),
    ;

    companion object {
        fun findByPrefValue(id: Int): SideNavMode {
            return values().firstOrNull { it.prefValue == id } ?: DEFAULT
        }
    }
}
