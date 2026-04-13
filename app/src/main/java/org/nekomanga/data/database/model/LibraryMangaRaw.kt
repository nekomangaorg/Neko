package org.nekomanga.data.database.model

import androidx.room.Embedded
import org.nekomanga.data.database.entity.MangaEntity

/**
 * Data class to hold the raw results of the library join query.
 * Replaces the storage-side logic of LibraryManga.
 */
data class LibraryMangaRaw(
    @Embedded val manga: MangaEntity,
    val unread: String,           // Raw string of scanlator/uploader/count
    val hasRead: String,          // Raw string of read chapters
    val bookmarkCount: Int,
    val unavailableCount: Int,
    val category: Int             // From COALESCE(MC.category_id, 0)
)
