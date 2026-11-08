package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.model.LibraryMangaRaw

@Dao
abstract class LibraryDao(private val database: RoomDatabase) {

    companion object {
        const val RAW_CHAPTER_SEPARATOR = " [.] "
        const val RAW_SCANLATOR_TYPE_SEPARATOR = " [;] "
        const val RAW_CHAPTER_COUNT_SEPARATOR = " [-] "
    }

    // A @Transaction @Query flow would run each read on Room's transaction executor, where
    // suspending writes queue too, so every write during a library update would wait for a whole
    // library read. Keep this table list in step with the query below.
    fun observeLibrary(): Flow<List<LibraryMangaRaw>> =
        database.invalidationTracker
            .createFlow("manga", "chapters", "manga_categories", "merge_manga")
            .conflate()
            .map { getLibraryList() }

    // A big library takes several cursor windows to read, and without the transaction a write that
    // drops a row between two of them makes the cursor read past the last row and throw.
    @Transaction
    @Query(
        """
        SELECT M.*, COALESCE(MC.category_id, 0) AS category, M.id IN (SELECT manga_id FROM merge_manga) AS is_merged
        FROM (
            SELECT manga.*,
                COALESCE(C.unread, '') AS unread,
                COALESCE(R.hasread, '') AS has_read,
                COALESCE(B.bookmarkCount, 0) AS bookmark_count,
                COALESCE(U.unavailableCount,0) as unavailable_count
            FROM manga
            LEFT JOIN (
                SELECT manga_id,
                    GROUP_CONCAT(
                        agg_scanlator || '${RAW_SCANLATOR_TYPE_SEPARATOR}' || agg_uploader || '${RAW_CHAPTER_COUNT_SEPARATOR}' || agg_count,
                        '${RAW_CHAPTER_SEPARATOR}'
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
            ON id = C.manga_id
            LEFT JOIN (
                SELECT manga_id,
                    GROUP_CONCAT(
                        agg_scanlator || '${RAW_SCANLATOR_TYPE_SEPARATOR}' || agg_uploader || '${RAW_CHAPTER_COUNT_SEPARATOR}' || agg_count,
                        '${RAW_CHAPTER_SEPARATOR}'
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
            ON id = R.manga_id
            LEFT JOIN (
                SELECT manga_id, COUNT(*) AS bookmarkCount
                FROM chapters
                WHERE bookmark = 1
                GROUP BY manga_id
            ) AS B
            ON id = B.manga_id
            LEFT JOIN (
                SELECT manga_id, COUNT(*) AS unavailableCount
                FROM chapters
                WHERE unavailable = 1 AND scanlator != 'Local'
                GROUP BY manga_id
            ) AS U
            ON id = U.manga_id
            WHERE favorite = 1
            GROUP BY id
            ORDER BY title
        ) AS M
        LEFT JOIN (
            SELECT * FROM manga_categories
        ) AS MC
        ON MC.manga_id = M.id
    """
    )
    abstract fun getLibraryList(): List<LibraryMangaRaw>
}
