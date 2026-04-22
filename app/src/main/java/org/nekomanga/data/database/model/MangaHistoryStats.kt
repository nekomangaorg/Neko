package org.nekomanga.data.database.model

import androidx.room.ColumnInfo

/**
 * Targeted Tuple to safely select and aggregate history stats for stats screen without fetching
 * heavy, unused columns into memory.
 */
data class MangaHistoryStats(
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "read_duration") val readDuration: Long,
    @ColumnInfo(name = "oldest_read_date") val oldestReadDate: Long?,
)
