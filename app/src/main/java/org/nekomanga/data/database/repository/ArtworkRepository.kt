package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {

    /**
     * Observes artwork for a given manga ID, emitting updates whenever the underlying data changes.
     */
    fun observeArtworkByMangaId(mangaId: Long): Flow<List<ArtworkImpl>>

    /** One-shot fetch of artwork for a given manga ID. */
    suspend fun getArtworkByMangaId(mangaId: Long): List<ArtworkImpl>

    /** Inserts a list of artwork entries. Replaces existing entries on conflict. */
    suspend fun insertArtworks(artworks: List<ArtworkImpl>)

    /** Deletes all artwork associated with a specific manga ID. */
    suspend fun deleteArtworkByMangaId(mangaId: Long)
}
