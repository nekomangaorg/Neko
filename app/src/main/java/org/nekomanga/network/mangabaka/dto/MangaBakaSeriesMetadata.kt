package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaPublished(
    @SerialName("start_date") val startDate: String?,
    @SerialName("end_date") val endDate: String?,
    @SerialName("start_date_is_estimated") val startDateIsEstimated: Boolean?,
    @SerialName("end_date_is_estimated") val endDateIsEstimated: Boolean?
)

@Serializable
data class MangaBakaAnimeDetails(
    val start: String?,
    val end: String?
)

@Serializable
data class MangaBakaPublisher(
    val name: String?,
    val type: String?,
    val note: String?
)

@Serializable
data class MangaBakaTagV2(
    val id: Int,
    @SerialName("merged_with") val mergedWith: Double? = null,
    @SerialName("is_spoiler") val isSpoiler: Boolean?,
    @SerialName("is_genre") val isGenre: Boolean = false,
    @SerialName("is_explicit") val isExplicit: Boolean = false,
    @SerialName("implied_by_tag_ids") val impliedByTagIds: List<Int> = emptyList(),
    @SerialName("parent_id") val parentId: Int?,
    val name: String,
    val level: Double,
    @SerialName("name_path") val namePath: String,
    @SerialName("series_count") val seriesCount: Double,
    val description: String?,
    @SerialName("content_rating") val contentRating: MangaBakaContentRating
)

@Serializable
data class MangaBakaRelationships(
    @SerialName("main_story") val mainStory: List<Double>? = null,
    val adaptation: List<Double>? = null,
    val prequel: List<Double>? = null,
    val sequel: List<Double>? = null,
    @SerialName("side_story") val sideStory: List<Double>? = null,
    @SerialName("spin_off") val spinOff: List<Double>? = null,
    val alternative: List<Double>? = null,
    val other: List<Double>? = null
)

@Serializable
data class MangaBakaSourceSpecificData(
    val anilist: MangaBakaSourceItemNum,
    @SerialName("anime_planet") val animePlanet: MangaBakaSourceItemStr,
    @SerialName("anime_news_network") val animeNewsNetwork: MangaBakaSourceItemNum,
    val kitsu: MangaBakaSourceItemNum,
    @SerialName("manga_updates") val mangaUpdates: MangaBakaSourceItemStr,
    @SerialName("my_anime_list") val myAnimeList: MangaBakaSourceItemNum,
    val shikimori: MangaBakaSourceItemNum
)

@Serializable
data class MangaBakaSourceItemNum(
    val id: Double?,
    val rating: Double?,
    @SerialName("rating_normalized") val ratingNormalized: Double? = null
)

@Serializable
data class MangaBakaSourceItemStr(
    val id: String?,
    val rating: Double?,
    @SerialName("rating_normalized") val ratingNormalized: Double? = null
)
