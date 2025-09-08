package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterMangaType : LibraryFilterType {
    object Inactive : FilterMangaType, BaseFilter(0)

    object Manga : FilterMangaType, BaseFilter(1, R.string.manga)

    object Manhwa : FilterMangaType, BaseFilter(2, R.string.manhwa)

    object Manhua : FilterMangaType, BaseFilter(3, R.string.manhua)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Manga,
            Manhua,
            Manhwa -> item.seriesType == this
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterMangaType {
            return when (fromInt) {
                3 -> Manhua
                2 -> Manhwa
                1 -> Manga
                else -> Inactive
            }
        }
    }
}
