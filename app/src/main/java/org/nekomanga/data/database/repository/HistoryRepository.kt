package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory as LegacyMangaChapterHistory
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.model.MangaChapterHistory

interface HistoryRepository {

    // =========================================================================
    // COMPLEX / JOIN QUERIES (Returns MangaChapterHistory POJO)
    // =========================================================================

    fun observeRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<LegacyMangaChapterHistory>>

    suspend fun getRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): List<LegacyMangaChapterHistory>

    fun observeRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<LegacyMangaChapterHistory>>

    suspend fun getRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): List<LegacyMangaChapterHistory>

    fun getHistoryPerPeriod(startDate: Long, endDate: Long): Flow<List<MangaChapterHistory>>

    fun observeAllRecentsTypes(
        search: String,
        includeRead: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    fun observeChapterHistoryByMangaId(mangaId: Long): Flow<List<LegacyMangaChapterHistory>>

    suspend fun getChapterHistoryByMangaId(mangaId: Long): List<LegacyMangaChapterHistory>

    // =========================================================================
    // STANDARD HISTORY QUERIES (Returns Domain Model)
    // =========================================================================

    suspend fun getHistoryByMangaId(mangaId: Long): List<History>

    fun observeHistoryByMangaId(mangaId: Long): Flow<List<History>>

    suspend fun getHistoryByMangaIds(mangaIds: List<Long>): List<History>

    suspend fun getHistoryByChapterUrl(chapterUrl: String): History?

    suspend fun getTotalReadDuration(): Long

    suspend fun getHistoryByChapterId(chapterId: Long): History?

    // =========================================================================
    // WRITE OPERATIONS
    // =========================================================================

    suspend fun insertHistory(history: History): Long

    suspend fun updateHistoryLastRead(chapterId: Long, lastRead: Long, timeRead: Long)

    suspend fun upsertHistory(history: History)

    suspend fun upsertHistoryList(historyList: List<History>)

    suspend fun deleteAllHistory()

    suspend fun deleteHistoryNoLastRead()
}
