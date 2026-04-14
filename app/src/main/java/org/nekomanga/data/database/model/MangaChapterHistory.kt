package org.nekomanga.data.database.model

import androidx.room.Embedded
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaEntity

data class MangaChapterHistory(
    @Embedded val manga: MangaEntity,
    @Embedded(prefix = "ch_") val chapter: ChapterEntity,
    @Embedded(prefix = "hi_") val history: HistoryEntity,
)
