package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.UploaderEntity

@Dao
interface UploaderDao {
    @Query("SELECT * FROM uploaders WHERE username = :name")
    suspend fun getUploaderByName(name: String): UploaderEntity?

    @Query("SELECT * FROM uploaders WHERE username IN (:names)")
    fun getUploadersByNames(names: List<String>): Flow<List<UploaderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploaders(uploaders: List<UploaderEntity>)

    @Query("DELETE FROM uploaders WHERE username = :name")
    suspend fun deleteUploader(name: String)
}
