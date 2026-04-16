package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanlator_group")
data class ScanlatorGroupEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "description") val description: String?,
)
