package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.BrowseFilterEntity

@Dao
interface BrowseFilterDao {

    @Query("SELECT * FROM browse_filter")
    fun getBrowseFilters(): Flow<List<BrowseFilterEntity>>

    @Query("SELECT * FROM browse_filter WHERE is_default = 1")
    suspend fun getDefaultFilter(): List<BrowseFilterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowseFilter(filter: BrowseFilterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowseFilters(filters: List<BrowseFilterEntity>)

    @Query("DELETE FROM browse_filter WHERE name = :name")
    suspend fun deleteBrowseFilterByName(name: String)

    @Query("DELETE FROM browse_filter")
    suspend fun deleteAllBrowseFilters()
}
