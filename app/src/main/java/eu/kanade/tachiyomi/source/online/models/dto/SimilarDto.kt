package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class RelationListDto(
    val response: String,
    val data: List<RelationDto>,
)

@Serializable
data class RelationDto(
    val attributes: RelationAttributesDto,
    val relationships: List<RelationMangaDto>,
)

@Serializable
data class RelationMangaDto(
    val id: String,
)

@Serializable
data class RelationAttributesDto(
    val relation: String,
)

@Serializable
data class SimilarMangaDatabaseDto(
    var similarApi: SimilarMangaDto? = null,
    var similarManga: List<RelatedMangaDto>? = null,
    var relatedManga: List<RelatedMangaDto>? = null,
    var aniListApi: AnilistMangaRecommendationsDto? = null,
    var aniListManga: List<RelatedMangaDto>? = null,
    var myAnimelistApi: MalMangaRecommendationsDto? = null,
    var myAnimeListManga: List<RelatedMangaDto>? = null,
    var mangaUpdatesApi: MUMangaDto? = null,
    var mangaUpdatesListManga: List<RelatedMangaDto>? = null,
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
data class RelatedMangaDto(
    val url: String,
    val title: String,
    val thumbnail: String,
    val relation: String,
)

@Serializable
data class SimilarMangaMatchListDto(
    val id: String,
    val title: Map<String, String>,
    val contentRating: String,
    val score: Double,
    val languages: List<String>,
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

@Serializable
data class MUMangaDto(
    val recommendations: List<MURecommendationDto>,
    val category_recommendations: List<MUCategoryRecommendationDto>,
)

@Serializable
data class MURecommendationDto(
    val series_name: String,
    val series_id: Long,
    val weight: Long,
)

@Serializable
data class MUCategoryRecommendationDto(
    val series_name: String,
    val series_id: Long,
    val weight: Long,
)

@Serializable
data class MUListsDto(
    val reading: Long,
    val wish: Long,
    val complete: Long,
    val unfinished: Long,
    val custom: Long,
)

@Serializable
data class MUPositionDto(
    val week: Long,
    val month: Long,
    val three_months: Long,
    val six_months: Long,
    val year: Long,
)
