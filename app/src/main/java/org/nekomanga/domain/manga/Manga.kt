package org.nekomanga.domain.manga

import eu.kanade.tachiyomi.data.database.models.Manga

data class MangaItem(
    val id: Long,
    val inLibrary: Boolean,
    val title: String,
) {
    fun fromManga(manga: Manga): MangaItem {
        return MangaItem(
            id = manga.id!!,
            inLibrary = manga.favorite,
            title = manga.title,
        )
    }
}

data class MergeManga(val thumbnail: String, val url: String, val title: String)

data class Artwork(
    val url: String,
    val mangaId: Long,
    val inLibrary: Boolean = false,
    val originalArtwork: String = "",
    val description: String = "",
    val volume: String = "",
    val active: Boolean = false,
)

