package org.nekomanga.presentation.screens.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.domain.manga.MangaContentRating

sealed interface FilterContentRating : LibraryFilterType {
    object Inactive : FilterContentRating, BaseFilter(0)

    object Safe : FilterContentRating, BaseFilter(1, R.string.safe)

    object Suggestive : FilterContentRating, BaseFilter(2, R.string.suggestive)

    object Erotica : FilterContentRating, BaseFilter(3, R.string.erotica)

    object Pornographic : FilterContentRating, BaseFilter(4, R.string.pornographic)

    override fun matches(item: LibraryMangaItem): Boolean {
        val rating =
            when (this) {
                Safe -> MangaContentRating.Safe
                Suggestive -> MangaContentRating.Suggestive
                Erotica -> MangaContentRating.Erotica
                Pornographic -> MangaContentRating.Pornographic
                Inactive -> return true
            }
        // The item stores the rating capitalized ("Safe"), the enum key is lowercase
        return item.contentRating.any { it.equals(rating.key, ignoreCase = true) }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterContentRating {
            return when (fromInt) {
                4 -> Pornographic
                3 -> Erotica
                2 -> Suggestive
                1 -> Safe
                else -> Inactive
            }
        }
    }
}
