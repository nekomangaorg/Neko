package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MangaChapter as LegacyMangaChapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.ChapterDao
import org.nekomanga.data.database.mapper.toChapter
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toManga

class ChapterRepositoryImpl(
    private val chapterDao: ChapterDao,
    // Injecting the dispatcher allows for easy testing, defaulting to IO
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChapterRepository {
    override fun observeChaptersForManga(mangaId: Long): Flow<List<Chapter>> {
        return chapterDao.observeChaptersForManga(mangaId).map { entities ->
            entities.map { it.toChapter() }
        }
    }

    override suspend fun getChaptersForManga(mangaId: Long): List<Chapter> {
        return chapterDao.getChaptersForManga(mangaId).map { it.toChapter() }
    }

    override suspend fun getChaptersForMangaIds(mangaIds: List<Long>): List<Chapter> {
        return chapterDao.getChaptersForMangaIds(mangaIds).map { it.toChapter() }
    }

    override suspend fun getChapterById(id: Long): Chapter? {
        return chapterDao.getChapterById(id)?.toChapter()
    }

    override suspend fun getChapterByUrl(url: String): Chapter? {
        return chapterDao.getChapterByUrl(url)?.toChapter()
    }

    override suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Chapter? {
        return chapterDao.getChapterByUrlAndMangaId(url, mangaId)?.toChapter()
    }

    override suspend fun insertChapter(chapter: Chapter): Long {
        return chapterDao.insertChapter(chapter.toEntity())
    }

    override suspend fun insertChapters(chapters: List<Chapter>): List<Long> {
        return chapterDao.insertChapters(chapters.map { it.toEntity() })
    }

    override suspend fun updateChapters(chapters: List<Chapter>) {
        chapterDao.updateChapters(chapters.map { it.toEntity() })
    }

    override suspend fun deleteChapter(chapter: Chapter) {
        chapterDao.deleteChapter(chapter.toEntity())
    }

    override suspend fun deleteChapters(chapters: List<Chapter>) {
        chapterDao.deleteChapters(chapters.map { it.toEntity() })
    }

    override suspend fun updateProgress(
        id: Long,
        read: Boolean,
        bookmark: Boolean,
        lastPage: Int,
        pagesLeft: Int,
    ) {
        chapterDao.updateProgress(id, read, bookmark, lastPage, pagesLeft)
    }

    override suspend fun updateSourceOrder(chapterId: String, mangaId: Long, order: Int) {
        chapterDao.updateSourceOrder(chapterId, mangaId, order)
    }

    // MangaChapter is already a Room Model, so we can return it directly without mapping
    override fun observeRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): Flow<List<LegacyMangaChapter>> {
        val sortByFetchedInt = if (sortByFetched) 1 else 0
        return chapterDao.observeRecentChapters(search, limit, offset, sortByFetchedInt).map {
            it.map { LegacyMangaChapter(it.manga.toManga(), it.chapter.toChapter()) }
        }
    }

    override suspend fun getRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): List<LegacyMangaChapter> {
        val sortByFetchedInt = if (sortByFetched) 1 else 0
        return chapterDao.getRecentChapters(search, limit, offset, sortByFetchedInt).map {
            LegacyMangaChapter(it.manga.toManga(), it.chapter.toChapter())
        }
    }

    // =========================================================================
    // LEGACY PUT RESOLVER BATCH OPERATIONS
    // =========================================================================

    override suspend fun updateChaptersBackup(chapters: List<Chapter>) {
        chapterDao.updateChaptersBackup(chapters.map { it.toEntity() })
    }

    override suspend fun updateKnownChaptersBackup(chapters: List<Chapter>) {
        chapterDao.updateKnownChaptersBackup(chapters.map { it.toEntity() })
    }

    override suspend fun updateChaptersProgress(chapters: List<Chapter>) {
        chapterDao.updateChaptersProgress(chapters.map { it.toEntity() })
    }

    override suspend fun fixChaptersSourceOrder(chapters: List<Chapter>) {
        chapterDao.fixChaptersSourceOrder(chapters.map { it.toEntity() })
    }
}
