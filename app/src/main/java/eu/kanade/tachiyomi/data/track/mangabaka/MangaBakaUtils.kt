package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.database.models.Track

fun Track.toApiStatus() =
    when (status) {
        MangaBaka.READING -> "reading"
        MangaBaka.COMPLETED -> "completed"
        MangaBaka.DROPPED -> "dropped"
        MangaBaka.PAUSED -> "paused"
        MangaBaka.PLAN_TO_READ -> "plan_to_read"
        MangaBaka.REREADING -> "rereading"
        MangaBaka.CONSIDERING -> "considering"
        else -> throw NotImplementedError("Unknown status: $status")
    }
