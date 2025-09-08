package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterUnavailable : LibraryFilterType {
    object Inactive : FilterUnavailable, BaseFilter(0)

    object Unavailable : FilterUnavailable, BaseFilter(1, R.string.unavailable)

    object NoUnavailable : FilterUnavailable, BaseFilter(2, R.string.no_unavailable)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Unavailable -> item.unavailableCount > 0
            NoUnavailable -> item.unavailableCount == 0
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterUnavailable {
            return when (fromInt) {
                2 -> NoUnavailable
                1 -> Unavailable
                else -> Inactive
            }
        }
    }
}
