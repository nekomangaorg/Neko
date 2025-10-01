package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterTracked : LibraryFilterType {
    object Inactive : FilterTracked, BaseFilter(0)

    object Tracked : FilterTracked, BaseFilter(1, R.string.tracked)

    object NotTracked : FilterTracked, BaseFilter(2, R.string.not_tracked)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Tracked -> item.trackCount > 0
            NotTracked -> item.trackCount == 0
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else FilterTracked.Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterTracked {
            return when (fromInt) {
                2 -> NotTracked
                1 -> Tracked
                else -> Inactive
            }
        }
    }
}
