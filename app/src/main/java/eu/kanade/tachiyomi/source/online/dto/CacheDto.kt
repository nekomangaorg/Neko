package eu.kanade.tachiyomi.source.online.dto

import kotlinx.serialization.Serializable

@Serializable
data class CacheApiMangaSerializer(
    val result: String,
    val data: CacheApiData,
    val relationships: List<CacheApiRelationships>,
)

@Serializable
data class CacheApiRelationships(
    val id: String,
    val type: String,
)

@Serializable
data class CacheApiData(
    val id: String,
    val type: String,
    val attributes: CacheApiDataAttributes,
)

@Serializable
data class CacheApiDataAttributes(
    val title: Map<String, String>,
    val description: Map<String, String>,
    val links: Map<String, String>? = null,
    val originalLanguage: String? = null,
    val lastChapter: String? = null,
    val publicationDemographic: String? = null,
    val status: String? = null,
    val contentRating: String? = null,
    val tags: List<CacheApiTags>? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CacheApiTags(
    val id: String,
    val type: String,
    val attributes: CacheApiTagAttributes,
)

@Serializable
data class CacheApiTagAttributes(
    val name: Map<String, String>,
    val version: Int,
)
