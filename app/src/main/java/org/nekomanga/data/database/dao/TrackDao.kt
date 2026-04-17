package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.TrackEntity

@Dao
interface TrackDao {
    @Query("SELECT * FROM track WHERE id = :id") suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT * FROM track WHERE manga_id = :mangaId")
    fun observeTracksForManga(mangaId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track WHERE manga_id = :mangaId")
    suspend fun getTracksForManga(mangaId: Long): List<TrackEntity>

    @Query("SELECT * FROM track WHERE manga_id IN (:mangaIds)")
    suspend fun getTracksForMangaByIds(mangaIds: List<Long>): List<TrackEntity>

    @Query("SELECT * FROM track") fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track") suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM track WHERE manga_id = :mangaId AND track_service_id = :trackServiceId")
    suspend fun getTrackByMangaIdAndTrackServiceId(mangaId: Long, trackServiceId: Int): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Delete suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM track WHERE manga_id = :mangaId AND track_service_id = :trackServiceId")
    suspend fun deleteTrackByMangaIdAndTrackServiceId(mangaId: Long, trackServiceId: Int)

    @Query("DELETE FROM track") suspend fun deleteAllTracks()
}
