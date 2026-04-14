package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.CategoryEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE _id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query(
        """
        SELECT categories.* FROM categories
        JOIN mangas_categories ON categories._id = mangas_categories.category_id
        WHERE mangas_categories.manga_id = :mangaId
    """
    )
    fun getCategoriesForManga(mangaId: Long): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Delete suspend fun deleteCategory(category: CategoryEntity)

    @Delete suspend fun deleteCategories(categories: List<CategoryEntity>)

    // Join table operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMangaCategory(join: MangaCategoryEntity)

    @Query("DELETE FROM mangas_categories WHERE manga_id = :mangaId")
    suspend fun deleteMangaFromAllCategories(mangaId: Long)
}
