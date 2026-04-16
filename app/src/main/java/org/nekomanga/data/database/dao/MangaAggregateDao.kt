package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.MangaAggregateEntity

@Dao
interface MangaAggregateDao {
    @Query("SELECT * FROM manga_aggregate WHERE manga_id = :mangaId")
    fun getMangaAggregate(mangaId: Long): Flow<MangaAggregateEntity?>

    @Query("SELECT * FROM manga_aggregate WHERE manga_id = :mangaId")
    suspend fun getMangaAggregateSync(mangaId: Long): MangaAggregateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaAggregate(aggregate: MangaAggregateEntity)

    @Query("DELETE FROM manga_aggregate WHERE manga_id = :mangaId")
    suspend fun deleteMangaAggregate(mangaId: Long)
}
