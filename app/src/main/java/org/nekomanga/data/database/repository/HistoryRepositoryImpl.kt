package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory as LegacyMangaChapterHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.HistoryDao
import org.nekomanga.data.database.mapper.toChapter
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toHistory
import org.nekomanga.data.database.mapper.toManga
import org.nekomanga.data.database.model.MangaChapterHistory

class HistoryRepositoryImpl(private val historyDao: HistoryDao) : HistoryRepository {

    // --- Complex POJO Queries ---

    override fun observeRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<LegacyMangaChapterHistory>> {
        return historyDao.observeRecentHistoryUngrouped("%$search%", limit, offset).map {
            it.map {
                LegacyMangaChapterHistory(
                    it.manga.toManga(),
                    it.chapter.toChapter(),
                    it.history.toHistory(),
                )
            }
        }
    }

    override suspend fun getRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): List<LegacyMangaChapterHistory> {
        return historyDao.getRecentHistoryUngrouped("%$search%", limit, offset).map {
            LegacyMangaChapterHistory(
                it.manga.toManga(),
                it.chapter.toChapter(),
                it.history.toHistory(),
            )
        }
    }

    override fun observeRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<LegacyMangaChapterHistory>> {
        return historyDao.observeRecentMangaLimit("%$search%", limit, offset).map {
            it.map {
                LegacyMangaChapterHistory(
                    it.manga.toManga(),
                    it.chapter.toChapter(),
                    it.history.toHistory(),
                )
            }
        }
    }

    override suspend fun getRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): List<LegacyMangaChapterHistory> {
        return historyDao.getRecentMangaLimit("%$search%", limit, offset).map {
            LegacyMangaChapterHistory(
                it.manga.toManga(),
                it.chapter.toChapter(),
                it.history.toHistory(),
            )
        }
    }

    override fun getHistoryPerPeriod(
        startDate: Long,
        endDate: Long,
    ): Flow<List<MangaChapterHistory>> {
        return historyDao.observeHistoryPerPeriod(startDate, endDate)
    }

    override fun observeAllRecentsTypes(
        search: String,
        includeRead: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>> {
        return historyDao.observeAllRecentsTypes("%$search%", includeRead, limit, offset)
    }

    override fun observeChapterHistoryByMangaId(
        mangaId: Long
    ): Flow<List<LegacyMangaChapterHistory>> {
        return historyDao.observeChapterHistoryByMangaId(mangaId).map {
            it.map {
                LegacyMangaChapterHistory(
                    it.manga.toManga(),
                    it.chapter.toChapter(),
                    it.history.toHistory(),
                )
            }
        }
    }

    override suspend fun getChapterHistoryByMangaId(
        mangaId: Long
    ): List<LegacyMangaChapterHistory> {
        return historyDao.getChapterHistoryByMangaId(mangaId).map {
            LegacyMangaChapterHistory(
                it.manga.toManga(),
                it.chapter.toChapter(),
                it.history.toHistory(),
            )
        }
    }

    // --- Standard Domain Queries ---

    override suspend fun getHistoryByMangaId(mangaId: Long): List<History> {
        return historyDao.getHistoryByMangaId(mangaId).map { it.toHistory() }
    }

    override fun observeHistoryByMangaId(mangaId: Long): Flow<List<History>> {
        return historyDao.observeHistoryByMangaId(mangaId).map { entities ->
            entities.map { it.toHistory() }
        }
    }

    override suspend fun getHistoryByMangaIds(mangaIds: List<Long>): List<History> {
        return historyDao.getHistoryByMangaIds(mangaIds).map { it.toHistory() }
    }

    override suspend fun getHistoryByChapterUrl(chapterUrl: String): History? {
        return historyDao.getHistoryByChapterUrl(chapterUrl)?.toHistory()
    }

    override suspend fun getTotalReadDuration(): Long {
        return historyDao.getTotalReadDuration()
    }

    override suspend fun getHistoryByChapterId(chapterId: Long): History? {
        return historyDao.getHistoryByChapterId(chapterId)?.toHistory()
    }

    // --- Write Operations ---

    override suspend fun insertHistory(history: History): Long {
        return historyDao.insertHistory(history.toEntity())
    }

    override suspend fun updateHistoryLastRead(chapterId: Long, lastRead: Long, timeRead: Long) {
        historyDao.updateHistoryLastRead(chapterId, lastRead, timeRead)
    }

    override suspend fun upsertHistory(history: History) {
        historyDao.upsertHistory(history.toEntity())
    }

    override suspend fun upsertHistoryList(historyList: List<History>) {
        historyDao.upsertHistoryList(historyList.map { it.toEntity() })
    }

    override suspend fun deleteAllHistory() {
        historyDao.deleteAllHistory()
    }

    override suspend fun deleteHistoryNoLastRead() {
        historyDao.deleteHistoryNoLastRead()
    }
}
