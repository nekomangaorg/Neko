package eu.kanade.tachiyomi.source.online.utils

import java.util.Locale

enum class FollowStatus(val int: Int) {
    UNFOLLOWED(0),
    READING(1),
    COMPLETED(2),
    ON_HOLD(3),
    PLAN_TO_READ(4),
    DROPPED(5),
    RE_READING(6);

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
