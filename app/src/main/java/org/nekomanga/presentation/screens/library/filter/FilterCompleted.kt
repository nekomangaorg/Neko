package org.nekomanga.presentation.screens.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterCompleted : LibraryFilterType {
    object Inactive : FilterCompleted, BaseFilter(0)

    object Completed : FilterCompleted, BaseFilter(1, R.string.completed)

    object Ongoing : FilterCompleted, BaseFilter(2, R.string.ongoing)

    object PublicationComplete : FilterCompleted, BaseFilter(3, R.string.publication_complete)

    object Hiatus : FilterCompleted, BaseFilter(4, R.string.hiatus)

    object Cancelled : FilterCompleted, BaseFilter(5, R.string.cancelled)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Completed -> "Completed" in item.status
            Ongoing -> "Ongoing" in item.status
            PublicationComplete -> "Publication Completed" in item.status
            Hiatus -> "Hiatus" in item.status
            Cancelled -> "Cancelled" in item.status
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterCompleted {
            return when (fromInt) {
                5 -> Cancelled
                4 -> Hiatus
                3 -> PublicationComplete
                2 -> Ongoing
                1 -> Completed
                else -> Inactive
            }
        }
    }
}
