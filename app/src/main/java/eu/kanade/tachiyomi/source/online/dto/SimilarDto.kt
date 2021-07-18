package eu.kanade.tachiyomi.source.online.dto

import kotlinx.serialization.Serializable

@Serializable
data class SimilarMangaDatabaseDto(
    val similarApi: SimilarMangaDto,
    val mangadexApi: MangaListDto,
)

@Serializable
data class SimilarMangaDto(
    val id: String,
    val title: Map<String, String>,
    val contentRating: String,
    val matches: List<SimilarMangaMatchListDto>,
    val updatedAt: String,
)

@Serializable
data class SimilarMangaMatchListDto(
    val id: String,
    val title: Map<String, String>,
    val contentRating: String,
    val score: Double,
)

@Serializable
data class MalMangaRecommendationsDto(
    val request_hash: String,
    val request_cached: Boolean,
    val request_cache_expiry: Long,
    val recommendations: List<MalMangaRecommendationDto>,
)

@Serializable
data class MalMangaRecommendationDto(
    val mal_id: Long,
    val url: String,
    val image_url: String,
    val recommendation_url: String,
    val title: String,
    val recommendation_count: Int,
)

