package org.nekomanga.domain.manga

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.models.MergeType

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
    val unreadCount: Int = 0,
    val readCount: Int = 0,
    val category: Int = 0,
    val bookmarkCount: Int = 0,
    val unavailableCount: Int = 0,
    val downloadCount: Int = 0,
) {
    val totalChapterCount
        get() = readCount + unreadCount

    val availableCount
        get() = totalChapterCount - unavailableCount

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
