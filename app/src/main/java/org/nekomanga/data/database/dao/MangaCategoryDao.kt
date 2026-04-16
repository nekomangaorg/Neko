package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.MangaCategoryEntity

@Dao
interface MangaCategoryDao {
    @Query("SELECT * FROM mangas_categories WHERE manga_id IN (:mangaIds)")
    fun getMangaCategories(mangaIds: List<Long>): Flow<List<MangaCategoryEntity>>

    @Query("SELECT * FROM mangas_categories WHERE manga_id IN (:mangaIds)")
    suspend fun getMangaCategoriesSync(mangaIds: List<Long>): List<MangaCategoryEntity>

    @Query("SELECT * FROM mangas_categories WHERE manga_id = :mangaId")
    fun getMangaCategoriesForManga(mangaId: Long): Flow<List<MangaCategoryEntity>>

    @Query("SELECT * FROM mangas_categories WHERE category_id = :categoryId")
    fun getMangaCategoriesForCategory(categoryId: Int): Flow<List<MangaCategoryEntity>>

    @Query("SELECT * FROM mangas_categories WHERE category_id = :categoryId")
    suspend fun getMangaCategoriesForCategorySync(categoryId: Int): List<MangaCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaCategory(mangaCategory: MangaCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaListCategories(mangaListCategories: List<MangaCategoryEntity>)

    @Query("DELETE FROM mangas_categories WHERE manga_id IN (:mangaIds)")
    suspend fun deleteOldMangaListCategories(mangaIds: List<Long>)

    @Transaction
    suspend fun setMangaCategories(
        mangaListCategories: List<MangaCategoryEntity>,
        mangaIds: List<Long>,
    ) {
        deleteOldMangaListCategories(mangaIds)
        insertMangaListCategories(mangaListCategories)
    }
}
