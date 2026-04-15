package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.constants.Constants
import org.nekomanga.data.database.dao.ArtworkDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.dao.SimilarDao
import org.nekomanga.data.database.dao.UploaderDao
import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MangaSimilarEntity
import org.nekomanga.data.database.entity.UploaderEntity
import org.nekomanga.data.database.model.LibraryManga
import org.nekomanga.data.database.model.LibraryMangaRaw
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.site.MangaDexPreferences

class MangaRepositoryImpl(
    private val mangaDao: MangaDao,
    private val artworkDao: ArtworkDao,
    private val similarDao: SimilarDao,
    private val uploaderDao: UploaderDao,
    private val libraryPreferences: LibraryPreferences,
    private val mangaDexPreferences: MangaDexPreferences,
) {

    /**
     * Replaces LibraryMangaGetResolver by mapping the Room Flow and applying the scanlator/uploader
     * filters.
     */
    fun getLibraryManga(): Flow<List<LibraryManga>> {
        return mangaDao.getLibraryMangaRaw().map { rawList ->
            // Fetch latest preferences once per emission
            val blockedGroups = mangaDexPreferences.blockedGroups().get()
            val blockedUploaders = mangaDexPreferences.blockedUploaders().get()
            val filterOption = libraryPreferences.chapterScanlatorFilterOption().get()

            rawList.map { rawItem ->
                val filteredUnread =
                    calculateValidCount(
                        rawItem.unread,
                        blockedGroups,
                        blockedUploaders,
                        filterOption,
                        rawItem.manga,
                    )

                val filteredRead =
                    calculateValidCount(
                        rawItem.hasRead,
                        blockedGroups,
                        blockedUploaders,
                        filterOption,
                        rawItem.manga,
                    )

                // Map back to the domain LibraryManga model
                rawItem.toDomainModel(filteredUnread, filteredRead)
            }
        }
    }

    /** Ported logic from LibraryMangaGetResolver.calculateValidChapterCount */
    private fun calculateValidCount(
        rawString: String,
        blockedGroups: Set<String>,
        blockedUploaders: Set<String>,
        filterOption: Int,
        manga: MangaEntity,
    ): Int {
        if (rawString.isBlank()) return 0
        var total = 0
        var startIndex = 0

        val mangaFilteredGroups =
            if (!manga.filteredScanlators.isNullOrBlank()) {
                ChapterUtil.getScanlators(manga.filteredScanlators).toSet()
            } else {
                null
            }

        // Iterates through the concatenated aggregate string
        while (startIndex < rawString.length) {
            val endIndex =
                rawString.indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex).let {
                    if (it == -1) rawString.length else it
                }

            val segment = rawString.substring(startIndex, endIndex)
            val parts = segment.split(Constants.RAW_SCANLATOR_TYPE_SEPARATOR)

            if (parts.size >= 2) {
                val scanlator = parts[0]
                val uploader = if (parts.size >= 3) parts[1] else ""
                val countPart = parts.last()
                val count =
                    countPart.split(Constants.RAW_CHAPTER_COUNT_SEPARATOR).last().toIntOrNull() ?: 0

                val scanlators = ChapterUtil.getScanlators(scanlator)

                val isBlocked =
                    ChapterUtil.filterByScanlator(
                        scanlators = scanlators,
                        uploader = uploader,
                        all = false,
                        filteredGroups = blockedGroups,
                        filteredUploaders = blockedUploaders,
                    )

                if (!isBlocked) {
                    if (mangaFilteredGroups == null) {
                        total += count
                    } else {
                        val isFiltered =
                            ChapterUtil.filterByScanlator(
                                scanlators = scanlators,
                                uploader = uploader,
                                all = filterOption == 0,
                                filteredGroups = mangaFilteredGroups,
                            )
                        if (!isFiltered) {
                            total += count
                        }
                    }
                }
            }
            startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
        }
        return total
    }

    // MangaDao wrappers
    fun getMangaList(): Flow<List<MangaEntity>> = mangaDao.getMangaList()

    suspend fun getMangaListSync(): List<MangaEntity> = mangaDao.getMangaListSync()

    fun getFavoriteMangaList(): Flow<List<MangaEntity>> = mangaDao.getFavoriteMangaList()

    suspend fun getFavoriteMangaListSync(): List<MangaEntity> = mangaDao.getFavoriteMangaListSync()

    suspend fun getMangas(ids: List<Long>): List<MangaEntity> = mangaDao.getMangas(ids)

    suspend fun getMangaByUrlAndSource(url: String, sourceId: Long): MangaEntity? =
        mangaDao.getMangaByUrlAndSource(url, sourceId)

    suspend fun getMangasByUrl(urls: List<String>): List<MangaEntity> =
        mangaDao.getMangasByUrl(urls)

    suspend fun getMangaByUrl(url: String): MangaEntity? = mangaDao.getMangaByUrl(url)

    suspend fun getMangaByIdSync(id: Long): MangaEntity? = mangaDao.getMangaByIdSync(id)

    fun getMangaById(id: Long): Flow<MangaEntity?> = mangaDao.getMangaById(id)

    fun getLibraryMangaList(): Flow<List<LibraryManga>> = mangaDao.getLibraryMangaList()

    suspend fun getLibraryMangaListSync(): List<LibraryManga> = mangaDao.getLibraryMangaListSync()

    suspend fun insertManga(manga: MangaEntity): Long = mangaDao.insertManga(manga)

    suspend fun insertMangas(mangas: List<MangaEntity>) = mangaDao.insertMangas(mangas)

    suspend fun updateManga(manga: MangaEntity) = mangaDao.updateManga(manga)

    suspend fun deleteManga(manga: MangaEntity) = mangaDao.deleteManga(manga)

    suspend fun deleteMangas(mangas: List<MangaEntity>) = mangaDao.deleteMangas(mangas)

    suspend fun deleteAllNotInLibrary() = mangaDao.deleteAllNotInLibrary()

    suspend fun deleteAllNotInLibraryAndNotRead() = mangaDao.deleteAllNotInLibraryAndNotRead()

    suspend fun deleteAllManga() = mangaDao.deleteAllManga()

    fun getReadNotInLibraryMangas(): Flow<List<MangaEntity>> = mangaDao.getReadNotInLibraryMangas()

    suspend fun getReadNotInLibraryMangasSync(): List<MangaEntity> =
        mangaDao.getReadNotInLibraryMangasSync()

    fun getLastReadManga(): Flow<List<MangaEntity>> = mangaDao.getLastReadManga()

    fun getLastFetchedManga(): Flow<List<MangaEntity>> = mangaDao.getLastFetchedManga()

    fun getTotalChapterManga(): Flow<List<MangaEntity>> = mangaDao.getTotalChapterManga()

    suspend fun updateFavorite(mangaId: Long, isFavorite: Boolean) =
        mangaDao.updateFavorite(mangaId, isFavorite)

    suspend fun updateDateAdded(mangaId: Long, dateAdded: Long) =
        mangaDao.updateDateAdded(mangaId, dateAdded)

    suspend fun updateViewerFlags(mangaId: Long, flags: Int) =
        mangaDao.updateViewerFlags(mangaId, flags)

    suspend fun updateChapterFlags(mangaId: Long, flags: Int) =
        mangaDao.updateChapterFlags(mangaId, flags)

    suspend fun updateMangaInfo(
        mangaId: Long,
        title: String,
        genre: String?,
        author: String?,
        artist: String?,
        status: Int,
        description: String?,
    ) = mangaDao.updateMangaInfo(mangaId, title, genre, author, artist, status, description)

    suspend fun updateLastUpdated(mangaId: Long, lastUpdate: Long) =
        mangaDao.updateLastUpdated(mangaId, lastUpdate)

    suspend fun updateNextUpdated(mangaId: Long, nextUpdate: Long) =
        mangaDao.updateNextUpdated(mangaId, nextUpdate)

    suspend fun updateScanlatorFilter(mangaId: Long, filter: String?) =
        mangaDao.updateScanlatorFilter(mangaId, filter)

    suspend fun updateLanguageFilter(mangaId: Long, filter: String?) =
        mangaDao.updateLanguageFilter(mangaId, filter)

    // ArtworkDao wrappers
    fun getArtworkForManga(mangaId: Long): Flow<List<ArtworkEntity>> =
        artworkDao.getArtworkForManga(mangaId)

    suspend fun insertArtwork(artwork: ArtworkEntity): Long = artworkDao.insertArtwork(artwork)

    suspend fun insertArtworks(artworks: List<ArtworkEntity>) = artworkDao.insertArtworks(artworks)

    suspend fun deleteArtworkForManga(mangaId: Long) = artworkDao.deleteArtworkForManga(mangaId)

    // SimilarDao wrappers
    fun getSimilar(mangaId: String): Flow<MangaSimilarEntity?> = similarDao.getSimilar(mangaId)

    suspend fun getSimilarSync(mangaId: String): MangaSimilarEntity? =
        similarDao.getSimilarSync(mangaId)

    suspend fun insertSimilar(similar: MangaSimilarEntity) = similarDao.insertSimilar(similar)

    suspend fun deleteAllSimilar() = similarDao.deleteAllSimilar()

    // UploaderDao wrappers
    suspend fun insertUploader(uploaders: List<UploaderEntity>) =
        uploaderDao.insertUploaders(uploaders)
}

fun LibraryMangaRaw.toDomainModel(filteredUnread: Int, filteredRead: Int): LibraryManga {
    return LibraryManga(
        manga = this.manga,
        unreadCount = filteredUnread,
        readCount = filteredRead,
        bookmarkCount = this.bookmarkCount,
        unavailableCount = this.unavailableCount,
        category = this.category,
    )
}
