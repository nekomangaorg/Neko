package eu.kanade.tachiyomi.source.online.handlers.serializers

import kotlinx.serialization.Serializable

@Serializable
data class CoversResult(
    val covers: List<String> = emptyList(),
    val status: String
)

