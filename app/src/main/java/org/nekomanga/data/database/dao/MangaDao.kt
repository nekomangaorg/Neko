package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.MangaEntity

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga") fun observeMangaList(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga") suspend fun getMangaList(): List<MangaEntity>

    @Query("SELECT * FROM manga WHERE favorite = 1 ORDER BY title")
    fun observeFavoriteMangaList(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE favorite = 1 ORDER BY title")
    suspend fun getFavoriteMangaList(): List<MangaEntity>

    @Query("SELECT * FROM manga WHERE id IN (:ids)")
    suspend fun getMangaByIds(ids: List<Long>): List<MangaEntity>

    @Query("SELECT * FROM manga WHERE url = :url AND source = :sourceId")
    suspend fun getMangaByUrlAndSourceSync(url: String, sourceId: Long): MangaEntity?

    @Query("SELECT * FROM manga WHERE url IN (:urls)")
    suspend fun getMangaByUrls(urls: List<String>): List<MangaEntity>

    @Query("SELECT * FROM manga WHERE url = :url")
    suspend fun getMangaByUrl(url: String): MangaEntity?

    @Query("SELECT * FROM manga WHERE id = :id") suspend fun getMangaById(id: Long): MangaEntity?

    @Query("SELECT * FROM manga WHERE id = :id") fun observeMangaById(id: Long): Flow<MangaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: MangaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaList(mangaList: List<MangaEntity>): List<Long>

    @Update suspend fun updateManga(manga: MangaEntity)

    @Delete suspend fun deleteManga(manga: MangaEntity)

    @Delete suspend fun deleteMangaList(mangaList: List<MangaEntity>)

    @Query("DELETE FROM manga WHERE favorite = 0") suspend fun deleteAllNotInLibrary()

    @Query(
        """
        DELETE FROM manga
        WHERE favorite = 0 AND id NOT IN (
            SELECT manga_id FROM chapters WHERE read = 1 OR last_page_read != 0
        )
    """
    )
    suspend fun deleteAllNotInLibraryAndNotRead()

    @Query("DELETE FROM manga") suspend fun deleteAllManga()

    @Query(
        """
        SELECT * FROM manga
        WHERE favorite = 0 AND id IN (
            SELECT manga_id FROM chapters WHERE read = 1 OR last_page_read != 0
        )
    """
    )
    fun observeReadNotInLibraryManga(): Flow<List<MangaEntity>>

    @Query(
        """
        SELECT * FROM manga
        WHERE favorite = 0 AND id IN (
            SELECT manga_id FROM chapters WHERE read = 1 OR last_page_read != 0
        )
    """
    )
    suspend fun getReadNotInLibraryManga(): List<MangaEntity>

    @Query(
        """
        SELECT manga.*, MAX(history.last_read) AS max
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        WHERE manga.favorite = 1
        GROUP BY manga.id
        ORDER BY max DESC
    """
    )
    fun observeLastReadManga(): Flow<List<MangaEntity>>

    @Query(
        """
        SELECT manga.*, MAX(chapters.date_fetch) AS max
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        WHERE manga.favorite = 1
        GROUP BY manga.id
        ORDER BY max DESC
    """
    )
    fun observeLastFetchedManga(): Flow<List<MangaEntity>>

    @Query(
        """
        SELECT manga.*
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        GROUP BY manga.id
        ORDER BY COUNT(*)
    """
    )
    fun observeTotalChapterManga(): Flow<List<MangaEntity>>

    @Query("UPDATE manga SET favorite = :isFavorite WHERE id = :mangaId")
    suspend fun updateFavorite(mangaId: Long, isFavorite: Boolean)

    @Query("UPDATE manga SET date_added = :dateAdded WHERE id = :mangaId")
    suspend fun updateDateAdded(mangaId: Long, dateAdded: Long)

    @Query("UPDATE manga SET viewer = :flags WHERE id = :mangaId")
    suspend fun updateViewerFlags(mangaId: Long, flags: Int)

    @Query("UPDATE manga SET chapter_flags = :flags WHERE id = :mangaId")
    suspend fun updateChapterFlags(mangaId: Long, flags: Int)

    @Query(
        """
    UPDATE manga SET
    title = :title,
    genre = :genre,
    author = :author,
    artist = :artist,
    status = :status,
    description = :description
    WHERE id = :mangaId
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

    @Query("UPDATE manga SET last_update = :lastUpdate WHERE id = :mangaId")
    suspend fun updateLastUpdated(mangaId: Long, lastUpdate: Long)

    @Query("UPDATE manga SET next_update = :nextUpdate WHERE id = :mangaId")
    suspend fun updateNextUpdated(mangaId: Long, nextUpdate: Long)

    @Query("UPDATE manga SET scanlator_filter_flag = :filter WHERE id = :mangaId")
    suspend fun updateScanlatorFilter(mangaId: Long, filter: String?)

    @Query("UPDATE manga SET language_filter_flag = :filter WHERE id = :mangaId")
    suspend fun updateLanguageFilter(mangaId: Long, filter: String?)
}
