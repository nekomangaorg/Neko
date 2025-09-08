package org.nekomanga.domain.manga

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.ui.library.filter.FilterMangaType

data class SimpleManga(val title: String, val id: Long)

data class SourceManga(
    val currentThumbnail: String,
    val url: String,
    val title: String,
    val displayText: String = "",
    @param:StringRes val displayTextRes: Int? = null,
)

data class LibraryMangaItem(
    val displayManga: DisplayManga,
    val addedToLibraryDate: Long = 0L,
    val latestChapterDate: Long = 0L,
    val unreadCount: Int = 0,
    val readCount: Int = 0,
    val category: Int = 0,
    val bookmarkCount: Int = 0,
    val unavailableCount: Int = 0,
    val downloadCount: Int = 0,
    val trackCount: Int = 0,
    val isMerged: Boolean = false,
    val hasMissingChapters: Boolean = false,
    val genre: List<String> = emptyList(),
    val author: List<String> = emptyList(),
    val contentRating: List<String> = emptyList(),
    val language: List<String> = emptyList(),
    val status: List<String> = emptyList(),
    val seriesType: FilterMangaType = FilterMangaType.Manga,
    val rating: Double = (-1).toDouble(),
) {
    val totalChapterCount
        get() = readCount + unreadCount

    val hasStarted
        get() = readCount > 0
}

data class DisplayManga(
    val mangaId: Long,
    val inLibrary: Boolean,
    val currentArtwork: Artwork,
    val url: String,
    val title: String,
    val displayText: String = "",
    val isVisible: Boolean = true,
    @param:StringRes val displayTextRes: Int? = null,
)

data class MergeArtwork(val url: String, val mergeType: MergeType)

data class Artwork(
    val url: String = "",
    val mangaId: Long,
    val inLibrary: Boolean = false,
    val originalArtwork: String = "",
    val description: String = "",
    val volume: String = "",
    val active: Boolean = false,
)
