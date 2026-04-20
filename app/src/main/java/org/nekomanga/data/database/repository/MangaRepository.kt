package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {

    /**
     * Observes the user's library, applying user preferences for blocked scanlators and uploaders
     * on the fly.
     */
    fun observeLibrary(): Flow<List<LibraryManga>>

    suspend fun getLibraryList(): List<LibraryManga>

    /**
     * val libraryFlow = mangaRepository.getLibraryAsFlow() .stateIn( scope = viewModelScope,
     * started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList() )
     */

    // --- Standard Queries ---
    fun observeMangaList(): Flow<List<Manga>>

    suspend fun getMangaList(): List<Manga>

    fun observeFavoriteMangaList(): Flow<List<Manga>>

    suspend fun getFavoriteMangaList(): List<Manga>

    suspend fun getMangaByIds(ids: List<Long>): List<Manga>

    suspend fun getMangaByUrlAndSource(url: String, sourceId: Long): Manga?

    suspend fun getMangaByUrls(urls: List<String>): List<Manga>

    suspend fun getMangaByUrl(url: String): Manga?

    suspend fun getMangaById(id: Long): Manga?

    fun observeMangaById(id: Long): Flow<Manga?>

    // --- Complex / Aggregate Queries ---
    fun observeReadNotInLibraryManga(): Flow<List<Manga>>

    suspend fun getReadNotInLibraryManga(): List<Manga>

    fun observeLastReadManga(): Flow<List<Manga>>

    fun observeLastFetchedManga(): Flow<List<Manga>>

    fun observeTotalChapterManga(): Flow<List<Manga>>

    // --- Write Operations ---
    suspend fun insertManga(manga: Manga): Long

    suspend fun insertMangaList(mangas: List<Manga>): List<Long>

    suspend fun updateManga(manga: Manga)

    suspend fun deleteManga(manga: Manga)

    suspend fun deleteMangaList(mangas: List<Manga>)

    suspend fun deleteAllNotInLibrary()

    suspend fun deleteAllNotInLibraryAndNotRead()

    suspend fun deleteAllManga()

    // =========================================================================
    // LEGACY PUT RESOLVER MIGRATIONS (Partial Updates)
    // =========================================================================

    suspend fun updateFavorite(mangaId: Long, isFavorite: Boolean)

    suspend fun updateDateAdded(mangaId: Long, dateAdded: Long)

    suspend fun updateViewerFlags(manga: Manga)

    suspend fun updateViewerFlags(mangas: List<Manga>)

    suspend fun updateChapterFlags(manga: Manga)

    suspend fun updateChapterFlags(mangas: List<Manga>)

    suspend fun updateMangaInfo(
        mangaId: Long,
        title: String,
        genre: String?,
        author: String?,
        artist: String?,
        status: Int,
        description: String?,
    )

    suspend fun updateLastUpdated(mangaId: Long, lastUpdate: Long)

    suspend fun updateNextUpdated(mangaId: Long, nextUpdate: Long)

    suspend fun updateScanlatorFilter(manga: Manga)

    suspend fun updateLanguageFilter(manga: Manga)
}
