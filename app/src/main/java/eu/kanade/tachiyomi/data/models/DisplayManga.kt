package eu.kanade.tachiyomi.data.models

import org.nekomanga.domain.manga.Artwork

data class SourceManga(val currentThumbnail: String, val url: String, val title: String, val displayText: String = "")

data class DisplayManga(val mangaId: Long, val inLibrary: Boolean, val currentArtwork: Artwork, val url: String, val title: String, val displayText: String = "")
