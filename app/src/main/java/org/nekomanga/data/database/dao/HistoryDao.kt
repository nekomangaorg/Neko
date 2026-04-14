package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.model.MangaChapterHistory

@Dao
interface HistoryDao {
    @Query(
        """
        SELECT mangas.*,
               chapters._id AS ch__id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id, chapters.old_mangadex_chapter_id AS ch_old_mangadex_id,
               chapters.language AS ch_language,
               history._id AS hi__id, history.history_chapter_id AS hi_history_chapter_id,
               history.history_last_read AS hi_history_last_read, history.history_time_read AS hi_history_time_read
        FROM mangas
        JOIN chapters ON mangas._id = chapters.manga_id
        JOIN history ON chapters._id = history.history_chapter_id
        WHERE history.history_last_read > 0
        AND LOWER(mangas.title) LIKE :search
        ORDER BY history.history_last_read DESC
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
        SELECT mangas.*,
               chapters._id AS ch__id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id, chapters.old_mangadex_chapter_id AS ch_old_mangadex_id,
               chapters.language AS ch_language,
               history._id AS hi__id, history.history_chapter_id AS hi_history_chapter_id,
               history.history_last_read AS hi_history_last_read, history.history_time_read AS hi_history_time_read
        FROM mangas
        JOIN chapters ON mangas._id = chapters.manga_id
        JOIN history ON chapters._id = history.history_chapter_id
        JOIN (
            SELECT chapters.manga_id, chapters._id as history_chapter_id, MAX(history.history_last_read) as history_last_read
            FROM chapters JOIN history ON chapters._id = history.history_chapter_id
            GROUP BY chapters.manga_id
        ) AS max_last_read
        ON chapters.manga_id = max_last_read.manga_id
        AND max_last_read.history_chapter_id = history.history_chapter_id
        AND max_last_read.history_last_read > 0
        AND LOWER(mangas.title) LIKE :search
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
        SELECT mangas.*,
               chapters._id AS ch__id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id, chapters.old_mangadex_chapter_id AS ch_old_mangadex_id,
               chapters.language AS ch_language,
               history._id AS hi__id, history.history_chapter_id AS hi_history_chapter_id,
               history.history_last_read AS hi_history_last_read, history.history_time_read AS hi_history_time_read
        FROM mangas
        JOIN chapters ON mangas._id = chapters.manga_id
        JOIN history ON chapters._id = history.history_chapter_id
        AND history.history_last_read >= :startDate
        AND history.history_last_read <= :endDate
        ORDER BY history.history_last_read DESC
    """
    )
    fun getHistoryPerPeriod(startDate: Long, endDate: Long): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT * FROM
        (SELECT mangas.*, chapters._id AS ch__id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id, chapters.old_mangadex_chapter_id AS ch_old_mangadex_id,
               chapters.language AS ch_language,
               history._id AS hi__id, history.history_chapter_id AS hi_history_chapter_id,
               history.history_last_read AS hi_history_last_read, history.history_time_read AS hi_history_time_read
        FROM (
            SELECT mangas.*
            FROM mangas
            LEFT JOIN (
                SELECT manga_id, COUNT(*) AS unread
                FROM chapters
                WHERE read = 0
                GROUP BY manga_id
            ) AS C
            ON mangas._id = C.manga_id
            WHERE (:includeRead = 1 OR C.unread > 0)
            GROUP BY mangas._id
            ORDER BY mangas.title
        ) AS mangas
        JOIN chapters
        ON mangas._id = chapters.manga_id
        JOIN history
        ON chapters._id = history.history_chapter_id
         JOIN (
            SELECT chapters.manga_id, chapters._id as history_chapter_id, MAX(history.history_last_read) as history_last_read
            FROM chapters JOIN history ON chapters._id = history.history_chapter_id
            GROUP BY chapters.manga_id) AS max_last_read
        ON chapters.manga_id = max_last_read.manga_id
        AND max_last_read.history_chapter_id = history.history_chapter_id
        AND max_last_read.history_last_read > 0
        AND LOWER(mangas.title) LIKE :search)
        UNION
        SELECT * FROM
        (SELECT mangas.*, chapters._id AS ch__id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id, chapters.old_mangadex_chapter_id AS ch_old_mangadex_id,
               chapters.language AS ch_language,
            Null as hi__id,
            Null as hi_history_chapter_id,
            chapters.date_fetch as hi_history_last_read,
            Null as hi_history_time_read
        FROM mangas
        JOIN chapters
        ON mangas._id = chapters.manga_id
        JOIN (
            SELECT chapters.manga_id, chapters._id as history_chapter_id, MAX(chapters.date_upload)
            FROM chapters JOIN mangas
            ON mangas._id = chapters.manga_id
            WHERE chapters.read = 0
            GROUP BY chapters.manga_id) AS newest_chapter
        ON chapters.manga_id = newest_chapter.manga_id
        WHERE mangas.favorite = 1
        AND newest_chapter.history_chapter_id = chapters._id
        AND chapters.date_fetch > mangas.date_added
        AND LOWER(mangas.title) LIKE :search)
        UNION
        SELECT * FROM
        (SELECT mangas.*,
            Null as ch__id,
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
            Null as ch_old_mangadex_id,
            Null as ch_language,
            Null as hi__id,
            Null as hi_history_chapter_id,
            mangas.date_added as hi_history_last_read,
            Null as hi_history_time_read
            FROM mangas
        WHERE mangas.favorite = 1
        AND LOWER(mangas.title) LIKE :search)
        ORDER BY hi_history_last_read DESC
        LIMIT :limit OFFSET :offset
    """
    )
    fun getAllRecentsTypes(
        search: String,
        includeRead: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.history_chapter_id = chapters._id
        WHERE chapters.manga_id = :mangaId
    """
    )
    suspend fun getHistoryByMangaId(mangaId: Long): List<HistoryEntity>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.history_chapter_id = chapters._id
        WHERE chapters.manga_id IN (:mangaIds)
    """
    )
    suspend fun getHistoryByMangaIds(mangaIds: List<Long>): List<HistoryEntity>

    @Query(
        """
        SELECT mangas.*,
               chapters._id AS ch__id, chapters.manga_id AS ch_manga_id, chapters.url AS ch_url, chapters.name AS ch_name,
               chapters.chapter_txt AS ch_chapter_txt, chapters.chapter_title AS ch_chapter_title, chapters.vol AS ch_vol,
               chapters.scanlator AS ch_scanlator, chapters.uploader AS ch_uploader, chapters.unavailable AS ch_unavailable,
               chapters.read AS ch_read, chapters.bookmark AS ch_bookmark, chapters.last_page_read AS ch_last_page_read,
               chapters.pages_left AS ch_pages_left, chapters.chapter_number AS ch_chapter_number, chapters.source_order AS ch_source_order,
               chapters.smart_order AS ch_smart_order, chapters.date_fetch AS ch_date_fetch, chapters.date_upload AS ch_date_upload,
               chapters.mangadex_chapter_id AS ch_mangadex_chapter_id, chapters.old_mangadex_chapter_id AS ch_old_mangadex_id,
               chapters.language AS ch_language,
               history._id AS hi__id, history.history_chapter_id AS hi_history_chapter_id,
               history.history_last_read AS hi_history_last_read, history.history_time_read AS hi_history_time_read
        FROM mangas
        JOIN chapters ON mangas._id = chapters.manga_id
        JOIN history ON chapters._id = history.history_chapter_id
        AND history.history_last_read > 0
        WHERE mangas._id = :mangaId
        ORDER BY history.history_last_read DESC
        LIMIT 25
    """
    )
    fun getChapterHistoryByMangaId(mangaId: Long): Flow<List<MangaChapterHistory>>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.history_chapter_id = chapters._id
        WHERE chapters.url = :chapterUrl
        LIMIT 1
    """
    )
    suspend fun getHistoryByChapterUrl(chapterUrl: String): HistoryEntity?

    @Query("SELECT SUM(history_time_read) FROM history") suspend fun getTotalReadDuration(): Long

    @Upsert suspend fun upsertHistory(history: HistoryEntity)

    @Upsert suspend fun upsertHistoryList(historyList: List<HistoryEntity>)

    @Query("DELETE FROM history") suspend fun deleteAllHistory()

    @Query("DELETE FROM history WHERE history_last_read = 0") suspend fun deleteHistoryNoLastRead()
}
