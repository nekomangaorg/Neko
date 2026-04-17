package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.ArtworkDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toImpl

class ArtworkRepositoryImpl(private val artworkDao: ArtworkDao) : ArtworkRepository {

    override fun observeArtworkByMangaId(mangaId: Long): Flow<List<ArtworkImpl>> {
        return artworkDao.observeArtworkForManga(mangaId).map { entities ->
            entities.map { it.toImpl() }
        }
    }

    override suspend fun getArtworkByMangaId(mangaId: Long): List<ArtworkImpl> {
        return artworkDao.getArtworkForManga(mangaId).map { it.toImpl() }
    }

    override suspend fun insertArtworks(artworks: List<ArtworkImpl>) {
        val entities = artworks.map { it.toEntity() }
        artworkDao.insertArtworks(entities)
    }

    override suspend fun deleteArtworkByMangaId(mangaId: Long) {
        artworkDao.deleteArtworkForManga(mangaId)
    }
}
