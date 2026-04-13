package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.MangaSimilarEntity

@Dao
interface SimilarDao {
    @Query("SELECT * FROM manga_similar WHERE manga_id = :mangaId")
    fun getSimilar(mangaId: String): Flow<MangaSimilarEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimilar(similar: MangaSimilarEntity)

    @Query("DELETE FROM manga_similar")
    suspend fun deleteAllSimilar()
}
