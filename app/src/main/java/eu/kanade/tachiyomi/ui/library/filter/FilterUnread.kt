package eu.kanade.tachiyomi.ui.library.filter

sealed class FilterUnread() {
    object Enabled : FilterUnread()

    object Disabled : FilterUnread()

    object Inactive : FilterUnread()

    fun toInt(): Int {
        return when (this) {
            Inactive -> 0
            Enabled -> 1
            Disabled -> 2
        }
    }

    companion object {

        fun fromInt(fromInt: Int): FilterUnread {
            return when (fromInt) {
                2 -> Disabled
                1 -> Enabled
                else -> Inactive
            }
        }
    }
}
