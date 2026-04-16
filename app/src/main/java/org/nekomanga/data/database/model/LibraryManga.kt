package org.nekomanga.data.database.model

import androidx.room.Embedded
import org.nekomanga.data.database.entity.MangaEntity

data class LibraryManga(
    @Embedded val manga: MangaEntity,
    val unreadCount: Int,
    val readCount: Int,
    val bookmarkCount: Int,
    val unavailableCount: Int,
    val category: Int,
)
