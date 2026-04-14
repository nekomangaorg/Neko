package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaders")
data class UploaderEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "uuid") val uuid: String,
)
