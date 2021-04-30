package eu.kanade.tachiyomi.source.online.handlers.serializers

import kotlinx.serialization.Serializable

@Serializable
data class ChapterListResponse(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val results: List<ChapterResponse>
)

@Serializable
data class ChapterResponse(
    val result: String,
    val data: NetworkChapter,
)

@Serializable
data class NetworkChapter(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
)

@Serializable
data class ChapterAttributes(
    val title: String?,
    val volume: Int?,
    val chapter: String?,
    val translatedLanguage: String,
    val publishAt: String,
    val data: List<String>,
    val dataSaver: List<String>,
    val groups: List<String>,
    val hash: String,
)

@Serializable
data class AtHomeResponse(
    val baseUrl: String
)
