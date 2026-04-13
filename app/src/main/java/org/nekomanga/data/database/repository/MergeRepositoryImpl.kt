package org.nekomanga.data.database.repository

import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.dao.MangaAggregateDao
import org.nekomanga.data.database.dao.MergeMangaDao
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.entity.MergeMangaEntity

class MergeRepositoryImpl(
    private val mergeMangaDao: MergeMangaDao,
    private val mangaAggregateDao: MangaAggregateDao
) {
    // Merge Manga methods
    fun getMergeMangaList(mangaId: Long): Flow<List<MergeMangaEntity>> = mergeMangaDao.getMergeMangaList(mangaId)

    suspend fun insertMergeManga(mergeManga: MergeMangaEntity): Long = mergeMangaDao.insertMergeManga(mergeManga)

    suspend fun insertMergeMangas(mergeMangas: List<MergeMangaEntity>) = mergeMangaDao.insertMergeMangas(mergeMangas)

    suspend fun deleteMergeManga(mergeManga: MergeMangaEntity) = mergeMangaDao.deleteMergeManga(mergeManga)

    suspend fun deleteMergeMangaByType(mangaId: Long, mergeType: Int) = mergeMangaDao.deleteMergeMangaByType(mangaId, mergeType)

    suspend fun deleteAllMergeMangaForManga(mangaId: Long) = mergeMangaDao.deleteAllMergeMangaForManga(mangaId)

    // Manga Aggregate methods
    fun getMangaAggregate(mangaId: Long): Flow<MangaAggregateEntity?> = mangaAggregateDao.getMangaAggregate(mangaId)

    suspend fun insertMangaAggregate(aggregate: MangaAggregateEntity) = mangaAggregateDao.insertMangaAggregate(aggregate)

    suspend fun deleteMangaAggregate(mangaId: Long) = mangaAggregateDao.deleteMangaAggregate(mangaId)
}
