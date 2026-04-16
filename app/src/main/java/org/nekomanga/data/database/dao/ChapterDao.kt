package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.model.MangaChapter

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE manga_id = :mangaId")
    fun getChaptersForManga(mangaId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE manga_id = :mangaId")
    suspend fun getChaptersForMangaSync(mangaId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE manga_id IN (:mangaIds)")
    suspend fun getChaptersForMangasSync(mangaIds: List<Long>): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE _id = :id")
    suspend fun getChapterByIdSync(id: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE url = :url")
    suspend fun getChapterByUrlSync(url: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE url = :url AND manga_id = :mangaId")
    suspend fun getChapterByUrlAndMangaIdSync(url: String, mangaId: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Update suspend fun updateChapters(chapters: List<ChapterEntity>)

    @Delete suspend fun deleteChapter(chapter: ChapterEntity)

    @Delete suspend fun deleteChapters(chapters: List<ChapterEntity>)

    @Query(
        """
        UPDATE chapters SET
        read = :read,
        bookmark = :bookmark,
        last_page_read = :lastPage,
        pages_left = :pagesLeft
        WHERE _id = :id
    """
    )
    suspend fun updateProgress(
        id: Long,
        read: Boolean,
        bookmark: Boolean,
        lastPage: Int,
        pagesLeft: Int,
    )

    @Query(
        "UPDATE chapters SET source_order = :order WHERE mangadex_chapter_id = :chapterId AND manga_id = :mangaId"
    )
    suspend fun updateSourceOrder(chapterId: String, mangaId: Long, order: Int)

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
               chapters.language AS ch_language
        FROM mangas JOIN chapters ON mangas._id = chapters.manga_id
        WHERE mangas.favorite = 1
        AND chapters.date_fetch > mangas.date_added
        AND LOWER(mangas.title) LIKE :search
        AND (
          chapters.unavailable = 0
          OR (chapters.unavailable = 1 AND chapters.scanlator = 'Local')
        )
        ORDER BY
            CASE WHEN :sortByFetched = 1 THEN chapters.date_fetch ELSE chapters.date_upload END DESC
        LIMIT :limit OFFSET :offset
    """
    )
    fun getRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): Flow<List<MangaChapter>>

    // =========================================================================
    // LEGACY PUT RESOLVER MIGRATIONS (Partial Column Updates)
    // =========================================================================

    // 1. Replicates ChapterBackupPutResolver
    @Query("UPDATE chapters SET read = :read, bookmark = :bookmark, last_page_read = :lastPageRead WHERE mangadex_chapter_id = :mangadexChapterId")
    suspend fun updateChapterBackupByMangadexId(mangadexChapterId: String, read: Boolean, bookmark: Boolean, lastPageRead: Int)

    @Transaction
    suspend fun updateChaptersBackup(chapters: List<ChapterEntity>) {
        chapters.forEach { chapter ->
            chapter.mangadexChapterId?.let {
                updateChapterBackupByMangadexId(it, chapter.read, chapter.bookmark, chapter.lastPageRead)
            }
        }
    }

    // 2. Replicates ChapterKnownBackupPutResolver
    @Query("UPDATE chapters SET read = :read, bookmark = :bookmark, last_page_read = :lastPageRead WHERE _id = :id")
    suspend fun updateKnownChapterBackupById(id: Long, read: Boolean, bookmark: Boolean, lastPageRead: Int)

    @Transaction
    suspend fun updateKnownChaptersBackup(chapters: List<ChapterEntity>) {
        chapters.forEach {
            updateKnownChapterBackupById(it.id, it.read, it.bookmark, it.lastPageRead)
        }
    }

    // 3. Replicates ChapterProgressPutResolver (Batch Version)
    @Transaction
    suspend fun updateChaptersProgress(chapters: List<ChapterEntity>) {
        chapters.forEach {
            updateProgress(it.id, it.read, it.bookmark, it.lastPageRead, it.pagesLeft)
        }
    }

    // 4. Replicates ChapterSourceOrderPutResolver (Batch Version)
    @Transaction
    suspend fun fixChaptersSourceOrder(chapters: List<ChapterEntity>) {
        chapters.forEach { chapter ->
            chapter.mangadexChapterId?.let {
                updateSourceOrder(it, chapter.mangaId, chapter.sourceOrder)
            }
        }
    }
}
