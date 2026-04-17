package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.HistoryImpl
import org.nekomanga.data.database.entity.HistoryEntity

fun HistoryEntity.toHistory(): HistoryImpl {
    return HistoryImpl().apply {
        id = this@toHistory.id
        chapter_id = this@toHistory.chapterId
        last_read = this@toHistory.lastRead
        time_read = this@toHistory.timeRead
    }
}

fun History.toEntity(): HistoryEntity {
    return HistoryEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new history inserts
        chapterId = this.chapter_id,
        lastRead = this.last_read,
        timeRead = this.time_read,
    )
}

// Optional List extensions for cleaner Repository code
fun List<HistoryEntity>.toHistoryList(): List<HistoryImpl> {
    return this.map { it.toHistory() }
}

fun List<History>.toEntityList(): List<HistoryEntity> {
    return this.map { it.toEntity() }
}
