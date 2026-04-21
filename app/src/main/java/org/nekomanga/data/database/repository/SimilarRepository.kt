package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import kotlinx.coroutines.flow.Flow

interface SimilarRepository {
    fun observeSimilar(mangaId: String): Flow<MangaSimilar?>

    suspend fun getSimilar(mangaId: String): MangaSimilar?

    suspend fun insertSimilar(similar: MangaSimilar)

    suspend fun deleteAllSimilar()
}
