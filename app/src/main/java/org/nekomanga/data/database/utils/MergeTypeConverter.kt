package org.nekomanga.data.database.utils

import androidx.room.TypeConverter
import eu.kanade.tachiyomi.data.database.models.MergeType

class MergeTypeConverter {
    @TypeConverter
    fun fromMergeType(value: MergeType): Int {
        return value.id // Store the Int ID in SQLite
    }

    @TypeConverter
    fun toMergeType(value: Int): MergeType {
        return MergeType.getById(value) // Reconstruct the enum from the ID
    }
}
