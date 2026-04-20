package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import kotlinx.coroutines.flow.Flow

interface MangaAggregateRepository {
    fun observeMangaAggregate(mangaId: Long): Flow<MangaAggregate?>

    suspend fun getMangaAggregate(mangaId: Long): MangaAggregate?

    suspend fun insertMangaAggregate(aggregate: MangaAggregate)

    suspend fun deleteMangaAggregate(mangaId: Long)
}
