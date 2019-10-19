package eu.kanade.tachiyomi.source.online.handlers.serializers

import kotlinx.serialization.Serializable

@Serializable
data class FollowsPageResult(
        val result: List<Result>
)

@Serializable
data class Result(
        val chapter: String,
        val follow_type: Int,
        val manga_id: Int,
        val volume: String
)
