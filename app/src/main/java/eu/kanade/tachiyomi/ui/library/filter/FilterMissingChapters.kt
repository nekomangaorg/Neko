package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterMissingChapters : LibraryFilterType {
    object Inactive : FilterMissingChapters, BaseFilter(0)

    object MissingChapter : FilterMissingChapters, BaseFilter(1, R.string.has_missing_chp)

    object NoMissingChapters : FilterMissingChapters, BaseFilter(2, R.string.no_missing_chp)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            MissingChapter -> item.hasMissingChapters
            NoMissingChapters -> !item.hasMissingChapters
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterMissingChapters {
            return when (fromInt) {
                2 -> NoMissingChapters
                1 -> MissingChapter
                else -> Inactive
            }
        }
    }
}
