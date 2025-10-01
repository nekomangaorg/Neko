package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.R
import org.nekomanga.domain.manga.LibraryMangaItem

sealed interface FilterDownloaded : LibraryFilterType {
    object Inactive : FilterDownloaded, BaseFilter(0)

    object Downloaded : FilterDownloaded, BaseFilter(1, R.string.downloaded)

    object NotDownloaded : FilterDownloaded, BaseFilter(2, R.string.not_downloaded)

    override fun matches(item: LibraryMangaItem): Boolean {
        return when (this) {
            Downloaded -> item.downloadCount > 0
            NotDownloaded -> item.downloadCount == 0
            Inactive -> true
        }
    }

    override fun toggle(enabling: Boolean): LibraryFilterType {
        return if (enabling) this else Inactive
    }

    companion object {
        fun fromInt(fromInt: Int): FilterDownloaded {
            return when (fromInt) {
                2 -> NotDownloaded
                1 -> Downloaded
                else -> Inactive
            }
        }
    }
}
