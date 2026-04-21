package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "uploader", indices = [Index(value = ["uuid"], unique = true)])
data class UploaderEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "uuid") val uuid: String,
)
