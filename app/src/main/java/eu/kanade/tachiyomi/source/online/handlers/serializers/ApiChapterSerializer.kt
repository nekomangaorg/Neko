package eu.kanade.tachiyomi.source.online.handlers.serializers

import kotlinx.serialization.Serializable

@Serializable
data class ApiChapterSerializer(
    val data: ChapterPageSerializer
)

@Serializable
data class ChapterPageSerializer(
    val hash: String,
    val pages: List<String>,
    val server: String,
    val mangaId: Int
)