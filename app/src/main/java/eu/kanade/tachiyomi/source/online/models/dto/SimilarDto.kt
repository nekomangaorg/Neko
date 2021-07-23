package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SimilarMangaDatabaseDto(
    var similarApi: SimilarMangaDto? = null,
    var similarMdexApi: MangaListDto? = null,
    var anilistApi: AnilistMangaRecommendationsDto? = null,
    var anilistMdexApi: MangaListDto? = null,
    var myanimelistApi: MalMangaRecommendationsDto? = null,
    var myanimelistMdexApi: MangaListDto? = null,
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
data class AnilistMangaRecommendationsDto(
    val data: AnilistMangaSimilarMedia,
)

@Serializable
data class AnilistMangaSimilarMedia(
    val Media: AnilistMangaSimilarRecommendations,
)

@Serializable
data class AnilistMangaSimilarRecommendations(
    val recommendations: AnilistMangaSimilarEdges,
)

@Serializable
data class AnilistMangaSimilarEdges(
    val edges: List<AnilistMangaSimilarEdge>,
)

@Serializable
data class AnilistMangaSimilarEdge(
    val node: AnilistMangaSimilarNode,
)

@Serializable
data class AnilistMangaSimilarNode(
    val mediaRecommendation: AnilistMangaSimilarMediaRecommend,
    val rating: Int,
)

@Serializable
data class AnilistMangaSimilarMediaRecommend(
    val id: Long,
    val format: String,
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

