package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.model.LibraryManga
import org.nekomanga.data.database.model.LibraryMangaRaw

@Dao
interface MangaDao {
    @Query("SELECT * FROM mangas") fun getMangaList(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM mangas") suspend fun getMangaListSync(): List<MangaEntity>

    @Query("SELECT * FROM mangas WHERE favorite = 1 ORDER BY title")
    fun getFavoriteMangaList(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM mangas WHERE favorite = 1 ORDER BY title")
    suspend fun getFavoriteMangaListSync(): List<MangaEntity>

    @Query("SELECT * FROM mangas WHERE _id IN (:ids)")
    suspend fun getMangasSync(ids: List<Long>): List<MangaEntity>

    @Query("SELECT * FROM mangas WHERE url = :url AND source = :sourceId")
    suspend fun getMangaByUrlAndSourceSync(url: String, sourceId: Long): MangaEntity?

    @Query("SELECT * FROM mangas WHERE url IN (:urls)")
    suspend fun getMangasByUrlSync(urls: List<String>): List<MangaEntity>

    @Query("SELECT * FROM mangas WHERE url = :url")
    suspend fun getMangaByUrlSync(url: String): MangaEntity?

    @Query("SELECT * FROM mangas WHERE _id = :id")
    suspend fun getMangaByIdSyncSync(id: Long): MangaEntity?

    @Query("SELECT * FROM mangas WHERE _id = :id") fun getMangaById(id: Long): Flow<MangaEntity?>

    // Replaces the complex libraryQuery from RawQueries.kt
    @Query(
        """
        SELECT mangas.*,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND read = 0) as unreadCount,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND read = 1) as readCount,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND bookmark = 1) as bookmarkCount,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND unavailable = 1) as unavailableCount,
        COALESCE(mangas_categories.category_id, 0) as category
        FROM mangas
        LEFT JOIN mangas_categories ON mangas._id = mangas_categories.manga_id
        WHERE favorite = 1
        ORDER BY title
    """
    )
    fun getLibraryMangaList(): Flow<List<LibraryManga>>

    // Replaces the complex libraryQuery from RawQueries.kt
    @Query(
        """
        SELECT mangas.*,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND read = 0) as unreadCount,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND read = 1) as readCount,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND bookmark = 1) as bookmarkCount,
        (SELECT COUNT(*) FROM chapters WHERE manga_id = mangas._id AND unavailable = 1) as unavailableCount,
        COALESCE(mangas_categories.category_id, 0) as category
        FROM mangas
        LEFT JOIN mangas_categories ON mangas._id = mangas_categories.manga_id
        WHERE favorite = 1
        ORDER BY title
    """
    )
    suspend fun getLibraryMangaListSync(): List<LibraryManga>

    @Query(
        """
    SELECT M.*, COALESCE(MC.category_id, 0) AS category
    FROM (
        SELECT mangas.*,
            COALESCE(C.unread, '') AS unread,
            COALESCE(R.hasread, '') AS hasRead,
            COALESCE(B.bookmarkCount, 0) AS bookmarkCount,
            COALESCE(U.unavailableCount,0) as unavailableCount
        FROM mangas
        LEFT JOIN (
            SELECT manga_id,
                GROUP_CONCAT(
                    agg_scanlator || ' [;] ' || agg_uploader || ' [-] ' || agg_count,
                    ' [.] '
                ) AS unread
            FROM (
                SELECT manga_id,
                       IFNULL(chapters.scanlator, 'N/A') AS agg_scanlator,
                       IFNULL(chapters.uploader, 'N/A') AS agg_uploader,
                       COUNT(*) AS agg_count
                FROM chapters
                WHERE read = 0
                GROUP BY manga_id, agg_scanlator, agg_uploader
            )
            GROUP BY manga_id
        ) AS C
        ON _id = C.manga_id
        LEFT JOIN (
            SELECT manga_id,
                GROUP_CONCAT(
                    agg_scanlator || ' [;] ' || agg_uploader || ' [-] ' || agg_count,
                    ' [.] '
                ) AS hasread
            FROM (
                SELECT manga_id,
                       IFNULL(chapters.scanlator, 'N/A') AS agg_scanlator,
                       IFNULL(chapters.uploader, 'N/A') AS agg_uploader,
                       COUNT(*) AS agg_count
                FROM chapters
                WHERE read = 1
                GROUP BY manga_id, agg_scanlator, agg_uploader
            )
            GROUP BY manga_id
        ) AS R
        ON _id = R.manga_id
        LEFT JOIN (
            SELECT manga_id, COUNT(*) AS bookmarkCount
            FROM chapters
            WHERE bookmark = 1
            GROUP BY manga_id
        ) AS B
        ON _id = B.manga_id
        LEFT JOIN (
            SELECT manga_id, COUNT(*) AS unavailableCount
            FROM chapters
            WHERE unavailable = 1 AND scanlator != 'Local'
            GROUP BY manga_id
        ) AS U
        ON _id = U.manga_id
        WHERE favorite = 1
        GROUP BY _id
        ORDER BY title
    ) AS M
    LEFT JOIN (
        SELECT * FROM mangas_categories) AS MC
        ON MC.manga_id = M._id
    """
    )
    fun getLibraryMangaRaw(): Flow<List<LibraryMangaRaw>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: MangaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangas(mangas: List<MangaEntity>)

    @Update suspend fun updateManga(manga: MangaEntity)

    @Delete suspend fun deleteManga(manga: MangaEntity)

    @Delete suspend fun deleteMangas(mangas: List<MangaEntity>)

    @Query("DELETE FROM mangas WHERE favorite = 0") suspend fun deleteAllNotInLibrary()

    @Query(
        """
        DELETE FROM mangas
        WHERE favorite = 0 AND _id NOT IN (
            SELECT manga_id FROM chapters WHERE read = 1 OR last_page_read != 0
        )
    """
    )
    suspend fun deleteAllNotInLibraryAndNotRead()

    @Query("DELETE FROM mangas") suspend fun deleteAllManga()

    @Query(
        """
        SELECT * FROM mangas
        WHERE favorite = 0 AND _id IN (
            SELECT manga_id FROM chapters WHERE read = 1 OR last_page_read != 0
        )
    """
    )
    fun getReadNotInLibraryMangas(): Flow<List<MangaEntity>>

    @Query(
        """
        SELECT * FROM mangas
        WHERE favorite = 0 AND _id IN (
            SELECT manga_id FROM chapters WHERE read = 1 OR last_page_read != 0
        )
    """
    )
    suspend fun getReadNotInLibraryMangasSync(): List<MangaEntity>

    @Query(
        """
        SELECT mangas.*, MAX(history.history_last_read) AS max
        FROM mangas
        JOIN chapters ON mangas._id = chapters.manga_id
        JOIN history ON chapters._id = history.history_chapter_id
        WHERE mangas.favorite = 1
        GROUP BY mangas._id
        ORDER BY max DESC
    """
    )
    fun getLastReadManga(): Flow<List<MangaEntity>>

    @Query(
        """
        SELECT mangas.*, MAX(chapters.date_fetch) AS max
        FROM mangas
        JOIN chapters ON mangas._id = chapters.manga_id
        WHERE mangas.favorite = 1
        GROUP BY mangas._id
        ORDER BY max DESC
    """
    )
    fun getLastFetchedManga(): Flow<List<MangaEntity>>

    @Query(
        """
        SELECT mangas.*
        FROM mangas
        JOIN chapters ON mangas._id = chapters.manga_id
        GROUP BY mangas._id
        ORDER BY COUNT(*)
    """
    )
    fun getTotalChapterManga(): Flow<List<MangaEntity>>

    @Query("UPDATE mangas SET favorite = :isFavorite WHERE _id = :mangaId")
    suspend fun updateFavorite(mangaId: Long, isFavorite: Boolean)

    @Query("UPDATE mangas SET date_added = :dateAdded WHERE _id = :mangaId")
    suspend fun updateDateAdded(mangaId: Long, dateAdded: Long)

    @Query("UPDATE mangas SET viewer = :flags WHERE _id = :mangaId")
    suspend fun updateViewerFlags(mangaId: Long, flags: Int)

    @Query("UPDATE mangas SET chapter_flags = :flags WHERE _id = :mangaId")
    suspend fun updateChapterFlags(mangaId: Long, flags: Int)

    @Query(
        """
    UPDATE mangas SET
    title = :title,
    genre = :genre,
    author = :author,
    artist = :artist,
    status = :status,
    description = :description
    WHERE _id = :mangaId
"""
    )
    suspend fun updateMangaInfo(
        mangaId: Long,
        title: String,
        genre: String?,
        author: String?,
        artist: String?,
        status: Int,
        description: String?,
    )

    @Query("UPDATE mangas SET last_update = :lastUpdate WHERE _id = :mangaId")
    suspend fun updateLastUpdated(mangaId: Long, lastUpdate: Long)

    @Query("UPDATE mangas SET next_update = :nextUpdate WHERE _id = :mangaId")
    suspend fun updateNextUpdated(mangaId: Long, nextUpdate: Long)

    @Query("UPDATE mangas SET scanlator_filter_flag = :filter WHERE _id = :mangaId")
    suspend fun updateScanlatorFilter(mangaId: Long, filter: String?)

    @Query("UPDATE mangas SET language_filter_flag = :filter WHERE _id = :mangaId")
    suspend fun updateLanguageFilter(mangaId: Long, filter: String?)
}
