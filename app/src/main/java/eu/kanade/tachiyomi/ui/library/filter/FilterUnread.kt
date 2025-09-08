package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterUnread : LibraryFilterType {
    object Inactive : FilterUnread, BaseFilter(0)

    object Unread : FilterUnread, BaseFilter(1, R.string.unread)

    object Read : FilterUnread, BaseFilter(2, R.string.read)

    object NotStarted : FilterUnread, BaseFilter(3, R.string.not_started)

    object InProgress : FilterUnread, BaseFilter(4, R.string.in_progress)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Unread -> item.unreadCount > 0
            Read -> item.unreadCount == 0
            InProgress -> item.unreadCount > 0 && item.hasStarted
            NotStarted -> item.unreadCount > 0 && !item.hasStarted
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else FilterUnread.Inactive
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
