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

data class LibraryManga(
    val mangaId: Long,
    val currentArtwork: Artwork,
    val title: String,
    val unreadCount: Int = 0,
    val downloadCount: Int = 0,
)

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
