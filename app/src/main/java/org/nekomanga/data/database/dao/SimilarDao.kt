package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.MangaSimilarEntity

@Dao
interface SimilarDao {
    @Query("SELECT * FROM manga_similar WHERE manga_id = :mangaDexUuid")
    fun observeSimilar(mangaDexUuid: String): Flow<MangaSimilarEntity?>

    @Query("SELECT * FROM manga_similar WHERE manga_id = :mangaDexUuid")
    suspend fun getSimilar(mangaDexUuid: String): MangaSimilarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimilar(similar: MangaSimilarEntity)

    @Query("DELETE FROM manga_similar") suspend fun deleteAllSimilar()
}
