package eu.kanade.tachiyomi.source.online.handlers.serializers

import kotlinx.serialization.Serializable

@Serializable
data class RelatedPageResult (
        val result: List<RelatedResult>
)

@Serializable
data class RelatedResult (
        val id: Int,
        val title: String,
        val matches: List<RelatedMatch>
)

@Serializable
data class RelatedMatch (
        val id: Int,
        val title: String,
        val score: Float
)
