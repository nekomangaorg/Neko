package org.nekomanga.domain.manga

import androidx.annotation.StringRes
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.ui.library.filter.FilterMangaType
import org.nekomanga.domain.category.CategoryItem

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
    val userCover: String?,
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
    val allCategories: List<CategoryItem> = emptyList(),
    val altTitles: List<String> = emptyList(),
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

    fun matches(searchQuery: String?): Boolean {
        return if (searchQuery == null) {
            true
        } else {
            displayManga.title.contains(searchQuery, true) ||
                this.altTitles.fastAny { altTitle -> altTitle.contains(searchQuery, true) } ||
                this.author.fastAny { author -> author.contains(searchQuery, true) } ||
                if (searchQuery.contains(",")) {
                    this.genre.fastAll { genre -> genre.contains(searchQuery) }
                    searchQuery.split(",").all { splitQuery ->
                        this.genre.fastAny { genre ->
                            if (splitQuery.startsWith("-")) {
                                !genre.contains(splitQuery.substringAfter("-"), true)
                            } else {
                                genre.contains(splitQuery, true)
                            }
                        }
                    }
                } else {
                    this.genre.fastAny { genre ->
                        if (searchQuery.startsWith("-")) {
                            !genre.contains(searchQuery.substringAfter("-"), true)
                        } else {
                            genre.contains(searchQuery, true)
                        }
                    }
                }
        }
    }
}

import eu.kanade.tachiyomi.data.database.models.Manga as DbManga

data class DisplayManga(
    val mangaId: Long,
    val inLibrary: Boolean,
    val currentArtwork: Artwork,
    val url: String,
    val title: String,
    val displayText: String = "",
    val isVisible: Boolean = true,
    @param:StringRes val displayTextRes: Int? = null,
) {
    fun toDbManga(): DbManga =
        DbManga.create(url, title).apply {
            id = mangaId
            favorite = inLibrary
            thumbnail_url = currentArtwork.url
        }
}

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
