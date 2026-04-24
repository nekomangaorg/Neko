package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.CategoryEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query(
        """
        SELECT categories.* FROM categories
        JOIN manga_categories ON categories.id = manga_categories.category_id
        WHERE manga_categories.manga_id = :mangaId
    """
    )
    fun observeCategoriesForManga(mangaId: Long): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT categories.* FROM categories
        JOIN manga_categories ON categories.id = manga_categories.category_id
        WHERE manga_categories.manga_id = :mangaId
    """
    )
    suspend fun getCategoriesForManga(mangaId: Long): List<CategoryEntity>

    @Upsert suspend fun insertCategory(category: CategoryEntity): Long

    @Upsert suspend fun insertCategories(categories: List<CategoryEntity>)

    @Delete suspend fun deleteCategory(category: CategoryEntity)

    @Delete suspend fun deleteCategories(categories: List<CategoryEntity>)

    // Join table operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaCategory(join: MangaCategoryEntity)

    @Query("DELETE FROM manga_categories WHERE manga_id = :mangaId")
    suspend fun deleteMangaFromAllCategories(mangaId: Long)
}
