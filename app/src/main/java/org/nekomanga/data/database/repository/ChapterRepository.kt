package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MangaChapter as LegacyMangaChapter
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {

    fun observeChaptersForManga(mangaId: Long): Flow<List<Chapter>>

    suspend fun getChaptersForManga(mangaId: Long): List<Chapter>

    suspend fun getChaptersForMangaIds(mangaIds: List<Long>): List<Chapter>

    suspend fun getChapterById(id: Long): Chapter?

    suspend fun getChapterByUrl(url: String): Chapter?

    suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Chapter?

    suspend fun insertChapter(chapter: Chapter): Long

    suspend fun insertChapters(chapters: List<Chapter>): List<Long>

    suspend fun updateChapters(chapters: List<Chapter>)

    suspend fun deleteChapter(chapter: Chapter)

    suspend fun deleteChapters(chapters: List<Chapter>)

    suspend fun updateProgress(
        id: Long,
        read: Boolean,
        bookmark: Boolean,
        lastPage: Int,
        pagesLeft: Int,
    )

    suspend fun updateSourceOrder(chapterId: String, mangaId: Long, order: Int)

    fun observeRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): Flow<List<LegacyMangaChapter>>

    suspend fun getRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): List<LegacyMangaChapter>

    // =========================================================================
    // LEGACY PUT RESOLVER BATCH OPERATIONS
    // =========================================================================

    suspend fun updateChaptersBackup(chapters: List<Chapter>)

    suspend fun updateKnownChaptersBackup(chapters: List<Chapter>)

    suspend fun updateChaptersProgress(chapters: List<Chapter>)

    suspend fun fixChaptersSourceOrder(chapters: List<Chapter>)
}
