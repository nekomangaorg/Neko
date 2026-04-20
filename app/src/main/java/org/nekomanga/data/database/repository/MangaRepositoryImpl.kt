package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.LibraryDao
import org.nekomanga.data.database.dao.MangaDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toLibraryManga
import org.nekomanga.data.database.mapper.toManga
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.site.MangaDexPreferences

class MangaRepositoryImpl(
    private val libraryDao: LibraryDao,
    private val mangaDao: MangaDao,
    private val mangaDexPreferences: MangaDexPreferences,
    private val libraryPreferences: LibraryPreferences,
    // Injecting the dispatcher is best practice for testing, defaulting to IO
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MangaRepository {

    override fun observeLibrary(): Flow<List<LibraryManga>> {
        return combine(
                libraryDao.observeLibrary(),
                mangaDexPreferences.blockedGroups().changes(),
                mangaDexPreferences.blockedUploaders().changes(),
                libraryPreferences.chapterScanlatorFilterOption().changes(),
            ) { rawLibraryList, blockedGroups, blockedUploaders, scanlatorPref ->
                rawLibraryList.map { rawManga ->
                    rawManga.toLibraryManga(
                        blockedGroups = blockedGroups,
                        blockedUploaders = blockedUploaders,
                        scanlatorFilterOption = scanlatorPref,
                    )
                }
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getLibraryList(): List<LibraryManga> {
        // Fetch everything sequentially (or you could use async/await here,
        // but reading from preferences memory cache is instant anyway)
        val rawLibraryList = libraryDao.getLibraryList()
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()
        val scanlatorPref = libraryPreferences.chapterScanlatorFilterOption().get()

        return rawLibraryList.map { rawManga ->
            rawManga.toLibraryManga(
                blockedGroups = blockedGroups,
                blockedUploaders = blockedUploaders,
                scanlatorFilterOption = scanlatorPref,
            )
        }
    }

    override fun observeMangaList(): Flow<List<Manga>> =
        mangaDao.observeMangaList().map { it.map { m -> m.toManga() } }

    override suspend fun getMangaList(): List<Manga> = mangaDao.getMangaList().map { it.toManga() }

    override fun observeFavoriteMangaList(): Flow<List<Manga>> =
        mangaDao.observeFavoriteMangaList().map { it.map { m -> m.toManga() } }

    override suspend fun getFavoriteMangaList(): List<Manga> =
        mangaDao.getFavoriteMangaList().map { it.toManga() }

    override suspend fun getMangaByIds(ids: List<Long>): List<Manga> =
        mangaDao.getMangaByIds(ids).map { it.toManga() }

    override suspend fun getMangaByUrlAndSource(url: String, sourceId: Long): Manga? =
        mangaDao.getMangaByUrlAndSourceSync(url, sourceId)?.toManga()

    override suspend fun getMangaByUrls(urls: List<String>): List<Manga> =
        mangaDao.getMangaByUrls(urls).map { it.toManga() }

    override suspend fun getMangaByUrl(url: String): Manga? = mangaDao.getMangaByUrl(url)?.toManga()

    override suspend fun getMangaById(id: Long): Manga? = mangaDao.getMangaById(id)?.toManga()

    override fun observeMangaById(id: Long): Flow<Manga?> =
        mangaDao.observeMangaById(id).map { it?.toManga() }

    // --- Complex / Aggregate Queries ---

    override fun observeReadNotInLibraryManga(): Flow<List<Manga>> =
        mangaDao.observeReadNotInLibraryManga().map { it.map { m -> m.toManga() } }

    override suspend fun getReadNotInLibraryManga(): List<Manga> =
        mangaDao.getReadNotInLibraryManga().map { it.toManga() }

    override fun observeLastReadManga(): Flow<List<Manga>> =
        mangaDao.observeLastReadManga().map { it.map { m -> m.toManga() } }

    override fun observeLastFetchedManga(): Flow<List<Manga>> =
        mangaDao.observeLastFetchedManga().map { it.map { m -> m.toManga() } }

    override fun observeTotalChapterManga(): Flow<List<Manga>> =
        mangaDao.observeTotalChapterManga().map { it.map { m -> m.toManga() } }

    // --- Write Operations ---

    override suspend fun insertManga(manga: Manga): Long = mangaDao.insertManga(manga.toEntity())

    override suspend fun insertMangaList(mangas: List<Manga>): List<Long> =
        mangaDao.insertMangaList(mangas.map { it.toEntity() })

    override suspend fun updateManga(manga: Manga) = mangaDao.updateManga(manga.toEntity())

    override suspend fun deleteManga(manga: Manga) = mangaDao.deleteManga(manga.toEntity())

    override suspend fun deleteMangaList(mangas: List<Manga>) =
        mangaDao.deleteMangaList(mangas.map { it.toEntity() })

    override suspend fun deleteAllNotInLibrary() = mangaDao.deleteAllNotInLibrary()

    override suspend fun deleteAllNotInLibraryAndNotRead() =
        mangaDao.deleteAllNotInLibraryAndNotRead()

    override suspend fun deleteAllManga() = mangaDao.deleteAllManga()

    // --- Partial Updates (Legacy Put Resolvers) ---

    override suspend fun updateFavorite(mangaId: Long, isFavorite: Boolean) =
        mangaDao.updateFavorite(mangaId, isFavorite)

    override suspend fun updateDateAdded(mangaId: Long, dateAdded: Long) =
        mangaDao.updateDateAdded(mangaId, dateAdded)

    override suspend fun updateViewerFlags(manga: Manga) {
        manga.id?.let { mangaDao.updateViewerFlags(it, manga.viewer_flags) }
    }

    override suspend fun updateViewerFlags(mangas: List<Manga>) {
        mangas.forEach { updateViewerFlags(it) }
    }

    override suspend fun updateChapterFlags(manga: Manga) {
        manga.id?.let { mangaDao.updateChapterFlags(it, manga.chapter_flags) }
    }

    override suspend fun updateChapterFlags(mangas: List<Manga>) {
        mangas.forEach { updateChapterFlags(it) }
    }

    override suspend fun updateMangaInfo(
        mangaId: Long,
        title: String,
        genre: String?,
        author: String?,
        artist: String?,
        status: Int,
        description: String?,
    ) {
        mangaDao.updateMangaInfo(mangaId, title, genre, author, artist, status, description)
    }

    override suspend fun updateLastUpdated(mangaId: Long, lastUpdate: Long) =
        mangaDao.updateLastUpdated(mangaId, lastUpdate)

    override suspend fun updateNextUpdated(mangaId: Long, nextUpdate: Long) =
        mangaDao.updateNextUpdated(mangaId, nextUpdate)

    override suspend fun updateScanlatorFilter(manga: Manga) {
        manga.id?.let { mangaDao.updateScanlatorFilter(it, manga.filtered_scanlators) }
    }

    override suspend fun updateLanguageFilter(manga: Manga) {
        manga.id?.let { mangaDao.updateLanguageFilter(it, manga.filtered_language) }
    }
}
