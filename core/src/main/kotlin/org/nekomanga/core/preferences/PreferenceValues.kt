package org.nekomanga.core.preferences

import org.nekomanga.core.R

// Library
const val MANGA_NON_COMPLETED = "manga_ongoing"
const val MANGA_HAS_UNREAD = "manga_fully_read"
const val MANGA_NON_READ = "manga_started"

// Device
const val DEVICE_ONLY_ON_WIFI = "wifi"
const val DEVICE_CHARGING = "ac"
const val DEVICE_BATTERY_NOT_LOW = "battery_not_low"

object PreferenceValues {

    enum class ReaderHideThreshold(val titleResId: Int, val threshold: Int) {
        HIGHEST(R.string.pref_highest, 5),
        HIGH(R.string.pref_high, 13),
        LOW(R.string.pref_low, 31),
        LOWEST(R.string.pref_lowest, 47),
    }
}
