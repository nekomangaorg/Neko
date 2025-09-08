package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterCompleted : LibraryFilterType {
    object Inactive : FilterCompleted, BaseFilter(0)

    object Completed : FilterCompleted, BaseFilter(1, R.string.completed)

    object Ongoing : FilterCompleted, BaseFilter(2, R.string.ongoing)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Completed -> "Completed" in item.status
            Ongoing -> "Completed" !in item.status
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterCompleted {
            return when (fromInt) {
                2 -> Completed
                1 -> Ongoing
                else -> Inactive
            }
        }
    }
}
