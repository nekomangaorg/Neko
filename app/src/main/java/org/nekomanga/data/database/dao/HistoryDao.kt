package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.model.MangaChapterHistory

@Dao
interface HistoryDao {
    @Query(
        """
        SELECT manga.*,
               chapters.id AS ch_id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id,
               chapters.language AS ch_language,
               history.id AS hi_id, history.chapter_id AS hi_chapter_id,
               history.last_read AS hi_last_read, history.time_read AS hi_time_read
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        WHERE history.last_read > 0
        AND LOWER(manga.title) LIKE :search
        ORDER BY history.last_read DESC
        LIMIT :limit OFFSET :offset
    """
    )
    fun getRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT manga.*,
               chapters.id AS ch_id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id,
               chapters.language AS ch_language,
               history.id AS hi_id, history.chapter_id AS hi_chapter_id,
               history.last_read AS hi_last_read, history.time_read AS hi_time_read
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        JOIN (
            SELECT chapters.manga_id, chapters.id as history_chapter_id, MAX(history.last_read) as history_last_read
            FROM chapters JOIN history ON chapters.id = history.chapter_id
            GROUP BY chapters.manga_id
        ) AS max_last_read
        ON chapters.manga_id = max_last_read.manga_id
        AND max_last_read.history_chapter_id = history.chapter_id
        AND max_last_read.history_last_read > 0
        AND LOWER(manga.title) LIKE :search
        ORDER BY max_last_read.history_last_read DESC
        LIMIT :limit OFFSET :offset
    """
    )
    fun getRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT manga.*,
               chapters.id AS ch_id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id,
               chapters.language AS ch_language,
               history.id AS hi_id, history.chapter_id AS hi_chapter_id,
               history.last_read AS hi_last_read, history.time_read AS hi_time_read
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        AND history.last_read >= :startDate
        AND history.last_read <= :endDate
        ORDER BY history.last_read DESC
    """
    )
    fun getHistoryPerPeriod(startDate: Long, endDate: Long): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT * FROM
        (SELECT manga.*, chapters.id AS ch_id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id,
               chapters.language AS ch_language,
               history.id AS hi_id, history.chapter_id AS hi_chapter_id,
               history.last_read AS hi_last_read, history.time_read AS hi_time_read
        FROM (
            SELECT manga.*
            FROM manga
            LEFT JOIN (
                SELECT manga_id, COUNT(*) AS unread
                FROM chapters
                WHERE read = 0
                GROUP BY manga_id
            ) AS C
            ON manga.id = C.manga_id
            WHERE (:includeRead = 1 OR C.unread > 0)
            GROUP BY manga.id
            ORDER BY manga.title
        ) AS manga
        JOIN chapters
        ON manga.id = chapters.manga_id
        JOIN history
        ON chapters.id = history.chapter_id
         JOIN (
            SELECT chapters.manga_id, chapters.id as history_chapter_id, MAX(history.last_read) as history_last_read
            FROM chapters JOIN history ON chapters.id = history.chapter_id
            GROUP BY chapters.manga_id) AS max_last_read
        ON chapters.manga_id = max_last_read.manga_id
        AND max_last_read.history_chapter_id = history.chapter_id
        AND max_last_read.history_last_read > 0
        AND LOWER(manga.title) LIKE :search)
        UNION
        SELECT * FROM
        (SELECT manga.*, chapters.id AS ch_id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id,
               chapters.language AS ch_language,
            Null as hi_id,
            Null as hi_chapter_id,
            chapters.date_fetch as hi_last_read,
            Null as hi_time_read
        FROM manga
        JOIN chapters
        ON manga.id = chapters.manga_id
        JOIN (
            SELECT chapters.manga_id, chapters.id as history_chapter_id, MAX(chapters.date_upload)
            FROM chapters JOIN manga
            ON manga.id = chapters.manga_id
            WHERE chapters.read = 0
            GROUP BY chapters.manga_id) AS newest_chapter
        ON chapters.manga_id = newest_chapter.manga_id
        WHERE manga.favorite = 1
        AND newest_chapter.history_chapter_id = chapters.id
        AND chapters.date_fetch > manga.date_added
        AND LOWER(manga.title) LIKE :search)
        UNION
        SELECT * FROM
        (SELECT manga.*,
            Null as ch_id,
            Null as ch_manga_id,
            Null as ch_url,
            Null as ch_name,
            Null as ch_chapter_txt,
            Null as ch_chapter_title,
            Null as ch_vol,
            Null as ch_scanlator,
            Null as ch_uploader,
            Null as ch_unavailable,
            Null as ch_read,
            Null as ch_bookmark,
            Null as ch_last_page_read,
            Null as ch_pages_left,
            Null as ch_chapter_number,
            Null as ch_source_order,
            Null as ch_smart_order,
            Null as ch_date_fetch,
            Null as ch_date_upload,
            Null as ch_mangadex_chapter_id,
            Null as ch_language,
            Null as hi_id,
            Null as hi_chapter_id,
            manga.date_added as hi_last_read,
            Null as hi_time_read
            FROM manga
        WHERE manga.favorite = 1
        AND LOWER(manga.title) LIKE :search)
        ORDER BY hi_last_read DESC
        LIMIT :limit OFFSET :offset
    """
    )
    fun observeAllRecentsTypes(
        search: String,
        includeRead: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.chapter_id = chapters.id
        WHERE chapters.manga_id = :mangaId
    """
    )
    suspend fun getHistoryByMangaId(mangaId: Long): List<HistoryEntity>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.chapter_id = chapters.id
        WHERE chapters.manga_id = :mangaId
    """
    )
    fun observeHistoryByMangaId(mangaId: Long): Flow<List<HistoryEntity>>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.chapter_id = chapters.id
        WHERE chapters.manga_id IN (:mangaIds)
    """
    )
    suspend fun getHistoryByMangaIds(mangaIds: List<Long>): List<HistoryEntity>

    @Query(
        """
        SELECT manga.*,
               chapters.id AS ch_id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id,
               chapters.language AS ch_language,
               history.id AS hi_id, history.chapter_id AS hi_chapter_id,
               history.last_read AS hi_last_read, history.time_read AS hi_time_read
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        AND history.last_read > 0
        WHERE manga.id = :mangaId
        ORDER BY history.last_read DESC
        LIMIT 25
    """
    )
    fun observeChapterHistoryByMangaId(mangaId: Long): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.chapter_id = chapters.id
        WHERE chapters.url = :chapterUrl
        LIMIT 1
    """
    )
    suspend fun getHistoryByChapterUrl(chapterUrl: String): HistoryEntity?

    @Query("SELECT SUM(time_read) FROM history") suspend fun getTotalReadDuration(): Long

    @Query("SELECT * FROM history WHERE chapter_id = :chapterId")
    suspend fun getHistoryByChapterId(chapterId: Long): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query(
        "UPDATE history SET last_read = :lastRead, time_read = :timeRead WHERE chapter_id = :chapterId"
    )
    suspend fun updateHistoryLastRead(chapterId: Long, lastRead: Long, timeRead: Long)

    @Transaction
    suspend fun upsertHistory(history: HistoryEntity) {
        val exists = getHistoryByChapterId(history.chapterId) != null
        if (exists) {
            updateHistoryLastRead(history.chapterId, history.lastRead, history.timeRead)
        } else {
            insertHistory(history)
        }
    }

    @Transaction
    suspend fun upsertHistoryList(historyList: List<HistoryEntity>) {
        historyList.forEach { upsertHistory(it) }
    }

    @Query("DELETE FROM history") suspend fun deleteAllHistory()

    @Query("DELETE FROM history WHERE last_read = 0") suspend fun deleteHistoryNoLastRead()
}
