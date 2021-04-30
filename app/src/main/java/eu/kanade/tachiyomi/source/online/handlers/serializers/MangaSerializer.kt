package eu.kanade.tachiyomi.source.online.handlers.serializers

import kotlinx.serialization.Serializable

@Serializable
data class MangaListResponse(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val results: List<MangaResponse>
)

@Serializable
data class MangaResponse(
    val result: String,
    val data: NetworkManga,
    val relationships: List<MangaRelationships>
)

@Serializable
data class NetworkManga(val id: String, val type: String, val attributes: NetworkMangaAttributes)

@Serializable
data class NetworkMangaAttributes(
    val title: Map<String, String>,
    val altTitles: List<Map<String, String>>,
    val description: Map<String, String>,
    val links: Map<String, String>?,
    val originalLanguage: String,
    val lastVolume: Int?,
    val lastChapter: String,
    val contentRating: String?,
    val publicationDemographic: String?,
    val status: String?,
    val year: Int?,
    val tags: List<TagsSerializer>,
    val readingStatus: String? = null,
)

@Serializable
data class TagsSerializer(
    val id: String
)

@Serializable
data class MangaRelationships(
    val id: String,
    val type: String,
)

@Serializable
data class AuthorResponse(
    val result: String,
    val data: NetworkAuthor,
)

@Serializable
data class NetworkAuthor(
    val id: String,
    val attributes: AuthorAttributes,
)

@Serializable
data class AuthorAttributes(
    val name: String,
)