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
    @Query("SELECT * FROM track WHERE _id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT * FROM track WHERE track_manga_id = :mangaId")
    fun getTracksForManga(mangaId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track WHERE track_manga_id IN (:mangaIds)")
    suspend fun getTracksForMangas(mangaIds: List<Long>): List<TrackEntity>

    @Query("SELECT * FROM track")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track WHERE track_manga_id = :mangaId AND track_sync_id = :syncId")
    suspend fun getTrackByMangaIdAndSyncId(mangaId: Long, syncId: Int): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM track WHERE track_manga_id = :mangaId AND track_sync_id = :syncId")
    suspend fun deleteTrackByMangaIdAndSyncId(mangaId: Long, syncId: Int)

    @Query("DELETE FROM track")
    suspend fun deleteAllTracks()
}
