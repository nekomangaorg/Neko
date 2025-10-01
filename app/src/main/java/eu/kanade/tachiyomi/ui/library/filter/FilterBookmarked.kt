package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterBookmarked : LibraryFilterType {
    object Inactive : FilterBookmarked, BaseFilter(0)

    object Bookmarked : FilterBookmarked, BaseFilter(1, R.string.bookmarked)

    object NotBookmarked : FilterBookmarked, BaseFilter(2, R.string.not_bookmarked)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Bookmarked -> item.bookmarkCount > 0
            NotBookmarked -> item.bookmarkCount == 0
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterBookmarked {
            return when (fromInt) {
                2 -> NotBookmarked
                1 -> Bookmarked
                else -> Inactive
            }
        }
    }
}
