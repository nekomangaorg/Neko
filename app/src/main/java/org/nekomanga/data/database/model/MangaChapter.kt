package org.nekomanga.data.database.model

import androidx.room.Embedded
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.MangaEntity

data class MangaChapter(
    @Embedded val manga: MangaEntity,
    @Embedded(prefix = "ch_") val chapter: ChapterEntity,
)
