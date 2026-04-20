package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.MergeMangaDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toMergeMangaImpl

class MergeMangaRepositoryImpl(private val mergeMangaDao: MergeMangaDao) : MergeMangaRepository {

    override suspend fun getAllMergeManga(): List<MergeMangaImpl> {
        return mergeMangaDao.getAllMergeManga().map { it.toMergeMangaImpl() }
    }

    override fun observeMergeMangaList(mangaId: Long): Flow<List<MergeMangaImpl>> {
        return mergeMangaDao.observeMergeMangaList(mangaId).map { entities ->
            entities.map { it.toMergeMangaImpl() }
        }
    }

    override suspend fun getMergeMangaList(mangaId: Long): List<MergeMangaImpl> {
        return mergeMangaDao.getMergeMangaList(mangaId).map { it.toMergeMangaImpl() }
    }

    override suspend fun getMergeMangaList(mangaIds: List<Long>): List<MergeMangaImpl> {
        return mergeMangaDao.getMergeMangaList(mangaIds).map { it.toMergeMangaImpl() }
    }

    override suspend fun insertMergeManga(mergeManga: MergeMangaImpl): Long {
        return mergeMangaDao.insertMergeManga(mergeManga.toEntity())
    }

    override suspend fun insertMergeMangaList(mergeMangaList: List<MergeMangaImpl>) {
        mergeMangaDao.insertMergeManga(mergeMangaList.map { it.toEntity() })
    }

    override suspend fun deleteMergeManga(mergeManga: MergeMangaImpl) {
        mergeMangaDao.deleteMergeManga(mergeManga.toEntity())
    }

    override suspend fun deleteMergeMangaByType(mangaId: Long, mergeType: MergeType) {
        mergeMangaDao.deleteMergeMangaByType(mangaId, mergeType.id)
    }

    override suspend fun deleteAllMergeMangaForManga(mangaId: Long) {
        mergeMangaDao.deleteAllMergeMangaForManga(mangaId)
    }
}
