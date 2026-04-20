package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    suspend fun getTrackById(id: Long): Track?

    fun observeTracksForManga(mangaId: Long): Flow<List<Track>>

    suspend fun getTracksForManga(mangaId: Long): List<Track>

    suspend fun getTracksForMangaByIds(mangaIds: List<Long>): List<Track>

    fun observeAllTracks(): Flow<List<Track>>

    suspend fun getAllTracks(): List<Track>

    suspend fun getTrackByMangaIdAndTrackServiceId(mangaId: Long, trackServiceId: Int): Track?

    suspend fun insertTrack(track: Track): Long

    suspend fun insertTracks(tracks: List<Track>)

    suspend fun deleteTrack(track: Track)

    suspend fun deleteTrackByMangaIdAndTrackServiceId(mangaId: Long, trackServiceId: Int)

    suspend fun deleteAllTracks()
}
