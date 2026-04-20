package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import kotlinx.coroutines.flow.Flow

interface MergeMangaRepository {
    suspend fun getAllMergeManga(): List<MergeMangaImpl>

    fun observeMergeMangaList(mangaId: Long): Flow<List<MergeMangaImpl>>

    suspend fun getMergeMangaList(mangaId: Long): List<MergeMangaImpl>

    suspend fun getMergeMangaList(mangaIds: List<Long>): List<MergeMangaImpl>

    suspend fun insertMergeManga(mergeManga: MergeMangaImpl): Long

    suspend fun insertMergeMangaList(mergeMangaList: List<MergeMangaImpl>)

    suspend fun deleteMergeManga(mergeManga: MergeMangaImpl)

    suspend fun deleteMergeMangaByType(mangaId: Long, mergeType: MergeType)

    suspend fun deleteAllMergeMangaForManga(mangaId: Long)
}
