package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.ArtworkEntity

@Dao
interface ArtworkDao {

    @Query("SELECT * FROM artwork WHERE manga_id = :mangaId")
    fun getArtworkForManga(mangaId: Long): Flow<List<ArtworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtwork(artwork: ArtworkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtworks(artworks: List<ArtworkEntity>)

    @Query("DELETE FROM artwork WHERE manga_id = :mangaId")
    suspend fun deleteArtworkForManga(mangaId: Long)
}
