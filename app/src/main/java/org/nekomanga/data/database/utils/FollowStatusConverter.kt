package org.nekomanga.data.database.utils

import androidx.room.TypeConverter
import eu.kanade.tachiyomi.source.online.utils.FollowStatus

class FollowStatusConverter {
    @TypeConverter
    fun fromFollowStatus(value: FollowStatus): Int {
        return value.int
    }

    @TypeConverter
    fun toFollowStatus(value: Int): FollowStatus {
        return FollowStatus.fromInt(value)
    }
}
