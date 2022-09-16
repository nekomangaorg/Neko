package eu.kanade.tachiyomi.source.online.utils

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import java.util.Locale

enum class FollowStatus(val int: Int, @StringRes val stringRes: Int) {
    UNFOLLOWED(0, R.string.follows_unfollowed),
    READING(1, R.string.follows_reading),
    COMPLETED(2, R.string.follows_completed),
    ON_HOLD(3, R.string.follows_on_hold),
    PLAN_TO_READ(4, R.string.follows_plan_to_read),
    DROPPED(5, R.string.follows_dropped),
    RE_READING(6, R.string.follows_re_reading),
    ;

    fun toDex(): String = this.name.lowercase(Locale.US)

    companion object {
        fun fromDex(value: String?): FollowStatus =
            values().firstOrNull { it.name.lowercase(Locale.US) == value } ?: UNFOLLOWED

        fun fromInt(value: Int): FollowStatus =
            values().firstOrNull { it.int == value } ?: UNFOLLOWED

        fun isUnfollowed(value: Int): Boolean {
            return value == UNFOLLOWED.int
        }
    }
}
