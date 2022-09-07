package org.nekomanga.domain.manga

data class SourceManga(val currentThumbnail: String, val url: String, val title: String, val displayText: String = "")

data class DisplayManga(val mangaId: Long, val inLibrary: Boolean, val currentArtwork: Artwork, val url: String, val title: String, val displayText: String = "")
