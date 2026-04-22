package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.TrackDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toTrack

class TrackRepositoryImpl(private val trackDao: TrackDao) : TrackRepository {

    override suspend fun getTrackById(id: Long): Track? {
        return trackDao.getTrackById(id)?.toTrack()
    }

    override fun observeTracksForManga(mangaId: Long): Flow<List<Track>> {
        return trackDao.observeTracksForManga(mangaId).map { entities ->
            entities.map { it.toTrack() }
        }
    }

    override suspend fun getTracksForManga(mangaId: Long): List<Track> {
        return trackDao.getTracksForManga(mangaId).map { it.toTrack() }
    }

    override suspend fun getTracksForMangaByIds(mangaIds: List<Long>): List<Track> {
        return mangaIds
            .chunked(500)
            .flatMap { trackDao.getTracksForMangaByIds(it) }
            .map { it.toTrack() }
    }

    override fun observeAllTracks(): Flow<List<Track>> {
        return trackDao.observeAllTracks().map { entities -> entities.map { it.toTrack() } }
    }

    override suspend fun getAllTracks(): List<Track> {
        return trackDao.getAllTracks().map { it.toTrack() }
    }

    override suspend fun getTrackByMangaIdAndTrackServiceId(
        mangaId: Long,
        trackServiceId: Int,
    ): Track? {
        return trackDao.getTrackByMangaIdAndTrackServiceId(mangaId, trackServiceId)?.toTrack()
    }

    override suspend fun insertTrack(track: Track): Long {
        return trackDao.insertTrack(track.toEntity())
    }

    override suspend fun insertTracks(tracks: List<Track>) {
        trackDao.insertTracks(tracks.map { it.toEntity() })
    }

    override suspend fun deleteTrack(track: Track) {
        trackDao.deleteTrack(track.toEntity())
    }

    override suspend fun deleteTrackByMangaIdAndTrackServiceId(mangaId: Long, trackServiceId: Int) {
        trackDao.deleteTrackByMangaIdAndTrackServiceId(mangaId, trackServiceId)
    }

    override suspend fun deleteAllTracks() {
        trackDao.deleteAllTracks()
    }
}
