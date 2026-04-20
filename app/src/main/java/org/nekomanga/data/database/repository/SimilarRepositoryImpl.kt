package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.SimilarDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toMangaSimilar

class SimilarRepositoryImpl(private val similarDao: SimilarDao) : SimilarRepository {

    override fun observeSimilar(mangaId: String): Flow<MangaSimilar?> {
        return similarDao.observeSimilar(mangaId).map { it?.toMangaSimilar() }
    }

    override suspend fun getSimilar(mangaId: String): MangaSimilar? {
        return similarDao.getSimilar(mangaId)?.toMangaSimilar()
    }

    override suspend fun insertSimilar(similar: MangaSimilar) {
        similarDao.insertSimilar(similar.toEntity())
    }

    override suspend fun deleteAllSimilar() {
        similarDao.deleteAllSimilar()
    }
}
