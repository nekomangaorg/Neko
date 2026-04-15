package org.nekomanga.data.database.repository

import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.dao.TrackDao
import org.nekomanga.data.database.entity.TrackEntity

class TrackRepositoryImpl(private val trackDao: TrackDao) {

    fun getTracksForManga(mangaId: Long): Flow<List<TrackEntity>> {
        return trackDao.getTracksForManga(mangaId)
    }

    suspend fun getTracksForMangaSync(mangaId: Long): List<TrackEntity> {
        return trackDao.getTracksForMangaSync(mangaId)
    }

    suspend fun getTracksForMangas(mangaIds: List<Long>): List<TrackEntity> {
        return trackDao.getTracksForMangas(mangaIds)
    }

    fun getAllTracks(): Flow<List<TrackEntity>> {
        return trackDao.getAllTracks()
    }

    suspend fun getAllTracksSync(): List<TrackEntity> {
        return trackDao.getAllTracksSync()
    }

    suspend fun getTrackById(id: Long): TrackEntity? {
        return trackDao.getTrackById(id)
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

    suspend fun deleteTrackByMangaIdAndSyncId(mangaId: Long, syncId: Int) {
        trackDao.deleteTrackByMangaIdAndSyncId(mangaId, syncId)
    }

    suspend fun deleteAllTracks() {
        trackDao.deleteAllTracks()
    }
}
