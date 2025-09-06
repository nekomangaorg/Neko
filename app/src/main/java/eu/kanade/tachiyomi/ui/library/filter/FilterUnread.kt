package eu.kanade.tachiyomi.ui.library.filter

sealed class FilterUnread() {
    object Unread : FilterUnread()

    object Read : FilterUnread()

    object NotStarted : FilterUnread()

    object InProgress : FilterUnread()

    object Inactive : FilterUnread()

    fun toInt(): Int {
        return when (this) {
            Inactive -> 0
            Unread -> 1
            Read -> 2
            NotStarted -> 3
            InProgress -> 4
        }
    }

    companion object {

        fun fromInt(fromInt: Int): FilterUnread {
            return when (fromInt) {
                4 -> InProgress
                3 -> NotStarted
                2 -> Read
                1 -> Unread
                else -> Inactive
            }
        }
    }
}
