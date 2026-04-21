package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.MergeMangaEntity

@Dao
interface MergeMangaDao {
    @Query("SELECT * FROM merge_manga") suspend fun getAllMergeManga(): List<MergeMangaEntity>

    @Query("SELECT * FROM merge_manga WHERE manga_id = :mangaId")
    fun observeMergeMangaList(mangaId: Long): Flow<List<MergeMangaEntity>>

    @Query("SELECT * FROM merge_manga WHERE manga_id = :mangaId")
    suspend fun getMergeMangaList(mangaId: Long): List<MergeMangaEntity>

    @Query("SELECT * FROM merge_manga WHERE manga_id IN (:mangaIds)")
    suspend fun getMergeMangaList(mangaIds: List<Long>): List<MergeMangaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMergeManga(mergeManga: MergeMangaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMergeManga(mergeMangaList: List<MergeMangaEntity>)

    @Delete suspend fun deleteMergeManga(mergeManga: MergeMangaEntity)

    @Query("DELETE FROM merge_manga WHERE manga_id = :mangaId AND merge_type = :mergeType")
    suspend fun deleteMergeMangaByType(mangaId: Long, mergeType: Int)

    @Query("DELETE FROM merge_manga WHERE manga_id = :mangaId")
    suspend fun deleteAllMergeMangaForManga(mangaId: Long)
}
