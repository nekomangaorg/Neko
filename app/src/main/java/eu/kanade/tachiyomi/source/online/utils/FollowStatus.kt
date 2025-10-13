package eu.kanade.tachiyomi.source.online.utils

import androidx.annotation.StringRes
import java.util.Locale
import org.nekomanga.constants.R

enum class FollowStatus(val int: Int, @param:StringRes val stringRes: Int) {
    UNFOLLOWED(0, R.string.follows_unfollowed),
    READING(1, R.string.follows_reading),
    COMPLETED(2, R.string.follows_completed),
    ON_HOLD(3, R.string.follows_on_hold),
    PLAN_TO_READ(4, R.string.follows_plan_to_read),
    DROPPED(5, R.string.follows_dropped),
    RE_READING(6, R.string.follows_re_reading);

    fun toDex(): String = this.name.lowercase(Locale.US)

    companion object {
        fun fromDex(value: String?): FollowStatus =
            entries.firstOrNull { it.name.lowercase(Locale.US) == value } ?: UNFOLLOWED

        fun fromInt(value: Int): FollowStatus =
            entries.firstOrNull { it.int == value } ?: UNFOLLOWED

        fun isUnfollowed(value: Int): Boolean {
            return value == UNFOLLOWED.int
        }

        fun fromStringRes(value: Int?): FollowStatus =
            entries.firstOrNull { it.stringRes == value } ?: UNFOLLOWED
    }
}
