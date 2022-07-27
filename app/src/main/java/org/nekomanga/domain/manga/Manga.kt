package org.nekomanga.domain.manga

import androidx.annotation.ColorInt

data class SimpleManga(
    val id: Long,
    val inLibrary: Boolean,
    val thumbnailUrl: String,
    val initialized: Boolean,
    val coverLastModified: Boolean,
)

data class MergeManga(val thumbnail: String, val url: String, val title: String)

data class Artwork(val url: String, val mangaId: Long, val inLibrary: Boolean, val originalArtwork: String, @ColorInt val vibrantColor: Int? = null)

