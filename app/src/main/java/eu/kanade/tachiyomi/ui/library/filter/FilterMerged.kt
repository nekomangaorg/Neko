package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterMerged : LibraryFilterType {
    object Inactive : FilterMerged, BaseFilter(0)

    object Merged : FilterMerged, BaseFilter(1, R.string.merged)

    object NotMerged : FilterMerged, BaseFilter(2, R.string.not_merged)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Merged -> item.isMerged
            NotMerged -> !item.isMerged
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterMerged {
            return when (fromInt) {
                2 -> NotMerged
                1 -> Merged
                else -> Inactive
            }
        }
    }
}
