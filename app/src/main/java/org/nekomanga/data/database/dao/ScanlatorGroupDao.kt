package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.nekomanga.data.database.entity.ScanlatorGroupEntity

@Dao
interface ScanlatorGroupDao {
    @Query("SELECT * FROM scanlator_group WHERE name = :name")
    suspend fun getScanlatorGroupByName(name: String): ScanlatorGroupEntity?

    @Query("SELECT * FROM scanlator_group WHERE name IN (:names)")
    suspend fun getScanlatorGroupsByNamesSync(names: List<String>): List<ScanlatorGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanlatorGroup(group: ScanlatorGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanlatorGroups(groups: List<ScanlatorGroupEntity>)

    @Query("DELETE FROM scanlator_group WHERE name = :name")
    suspend fun deleteScanlatorGroup(name: String)
}
