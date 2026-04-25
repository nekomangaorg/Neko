package org.nekomanga.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import org.nekomanga.data.database.entity.MangaEntity

/**
 * Data class to hold the raw results of the library join query. Replaces the storage-side logic of
 * LibraryManga.
 */
data class LibraryMangaRaw(
    @Embedded val manga: MangaEntity,
    @ColumnInfo(name = "unread") val unread: String,
    @ColumnInfo(name = "has_read") val hasRead: String,
    @ColumnInfo(name = "bookmark_count") val bookmarkCount: Int,
    @ColumnInfo(name = "unavailable_count") val unavailableCount: Int,
    @ColumnInfo(name = "category") val category: Int,
    @ColumnInfo(name = "is_merged") val isMerged: Boolean = false,
)
