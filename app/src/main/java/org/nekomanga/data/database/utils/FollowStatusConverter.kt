package org.nekomanga.data.database.utils

import androidx.room.TypeConverter
import eu.kanade.tachiyomi.source.online.utils.FollowStatus

class FollowStatusConverter {
    @TypeConverter
    fun fromFollowStatus(value: FollowStatus?): Int {
        return value?.int ?: 0
    }

    @TypeConverter
    fun toFollowStatus(value: Int?): FollowStatus {
        return value?.let { FollowStatus.fromInt(value) } ?: FollowStatus.UNFOLLOWED
    }
}
