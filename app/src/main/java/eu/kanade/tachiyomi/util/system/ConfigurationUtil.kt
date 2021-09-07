package eu.kanade.tachiyomi.util.system

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class SideNavMode(val prefValue: Int, @StringRes val stringRes: Int) {
    DEFAULT(0, R.string.default_behavior),
    NEVER(1, R.string.never),
    ALWAYS(2, R.string.always),
}
