package org.nekomanga.data.database.repository

import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.dao.TrackDao
import org.nekomanga.data.database.entity.TrackEntity

class TrackRepositoryImpl(
    private val trackDao: TrackDao
) {

    fun getTracksForManga(mangaId: Long): Flow<List<TrackEntity>> {
        return trackDao.getTracksForManga(mangaId)
    }

    suspend fun insertTrack(track: TrackEntity): Long {
        return trackDao.insertTrack(track)
    }

    suspend fun insertTracks(tracks: List<TrackEntity>) {
        trackDao.insertTracks(tracks)
    }

    suspend fun deleteTrack(track: TrackEntity) {
        trackDao.deleteTrack(track)
    }
}
