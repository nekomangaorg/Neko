package org.nekomanga.core.preferences

import org.nekomanga.core.R

object PreferenceValues {

    enum class ReaderHideThreshold(val titleResId: Int, val threshold: Int) {
        HIGHEST(R.string.pref_highest, 5),
        HIGH(R.string.pref_high, 13),
        LOW(R.string.pref_low, 31),
        LOWEST(R.string.pref_lowest, 47),
    }
}
