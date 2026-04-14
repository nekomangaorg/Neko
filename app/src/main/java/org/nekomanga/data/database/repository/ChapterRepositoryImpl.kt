package org.nekomanga.data.database.repository

import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.dao.ChapterDao
import org.nekomanga.data.database.dao.HistoryDao
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.model.MangaChapter
import org.nekomanga.data.database.model.MangaChapterHistory

class ChapterRepositoryImpl(
    private val chapterDao: ChapterDao,
    private val historyDao: HistoryDao,
) {

    fun getChaptersForManga(mangaId: Long): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersForManga(mangaId)
    }

    suspend fun getChaptersForMangas(mangaIds: List<Long>): List<ChapterEntity> {
        return chapterDao.getChaptersForMangas(mangaIds)
    }

    suspend fun getChapterById(id: Long): ChapterEntity? {
        return chapterDao.getChapterById(id)
    }

    suspend fun getChapterByUrl(url: String): ChapterEntity? {
        return chapterDao.getChapterByUrl(url)
    }

    suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): ChapterEntity? {
        return chapterDao.getChapterByUrlAndMangaId(url, mangaId)
    }

    suspend fun insertChapter(chapter: ChapterEntity): Long {
        return chapterDao.insertChapter(chapter)
    }

    suspend fun insertChapters(chapters: List<ChapterEntity>) {
        chapterDao.insertChapters(chapters)
    }

    suspend fun updateChapters(chapters: List<ChapterEntity>) {
        chapterDao.updateChapters(chapters)
    }

    suspend fun deleteChapter(chapter: ChapterEntity) {
        chapterDao.deleteChapter(chapter)
    }

    suspend fun deleteChapters(chapters: List<ChapterEntity>) {
        chapterDao.deleteChapters(chapters)
    }

    suspend fun updateProgress(
        id: Long,
        read: Boolean,
        bookmark: Boolean,
        lastPage: Int,
        pagesLeft: Int,
    ) {
        chapterDao.updateProgress(id, read, bookmark, lastPage, pagesLeft)
    }

    suspend fun updateSourceOrder(chapterId: String, mangaId: Long, order: Int) {
        chapterDao.updateSourceOrder(chapterId, mangaId, order)
    }

    fun getRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): Flow<List<MangaChapter>> {
        return chapterDao.getRecentChapters(search, limit, offset, sortByFetched)
    }

    fun getRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>> {
        return historyDao.getRecentHistoryUngrouped(search, limit, offset)
    }

    fun getRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>> {
        return historyDao.getRecentMangaLimit(search, limit, offset)
    }

    fun getHistoryPerPeriod(startDate: Long, endDate: Long): Flow<List<MangaChapterHistory>> {
        return historyDao.getHistoryPerPeriod(startDate, endDate)
    }

    fun getAllRecentsTypes(
        search: String,
        includeRead: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>> {
        return historyDao.getAllRecentsTypes(search, includeRead, limit, offset)
    }

    suspend fun getHistoryByMangaId(mangaId: Long): List<HistoryEntity> {
        return historyDao.getHistoryByMangaId(mangaId)
    }

    suspend fun getHistoryByMangaIds(mangaIds: List<Long>): List<HistoryEntity> {
        return historyDao.getHistoryByMangaIds(mangaIds)
    }

    fun getChapterHistoryByMangaId(mangaId: Long): Flow<List<MangaChapterHistory>> {
        return historyDao.getChapterHistoryByMangaId(mangaId)
    }

    suspend fun getHistoryByChapterUrl(chapterUrl: String): HistoryEntity? {
        return historyDao.getHistoryByChapterUrl(chapterUrl)
    }

    suspend fun getTotalReadDuration(): Long {
        return historyDao.getTotalReadDuration()
    }

    suspend fun upsertHistory(history: HistoryEntity) {
        historyDao.upsertHistory(history)
    }

    suspend fun upsertHistoryList(historyList: List<HistoryEntity>) {
        historyDao.upsertHistoryList(historyList)
    }

    suspend fun deleteAllHistory() {
        historyDao.deleteAllHistory()
    }

    suspend fun deleteHistoryNoLastRead() {
        historyDao.deleteHistoryNoLastRead()
    }
}
