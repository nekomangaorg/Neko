package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class MangaBakaSeriesResult(val data: MangaBakaSeries)

@Serializable data class MangaBakaSeriesSearchResult(val data: List<MangaBakaSeries>)

@Serializable
data class MangaBakaSeries(
    val id: Double,
    val state: MangaBakaSeriesState,
    @SerialName("merged_with") val mergedWith: Double? = null,
    @Deprecated("Use the titles property instead") val title: String? = null,
    @Deprecated("Use the titles property instead")
    @SerialName("native_title")
    val nativeTitle: String? = null,
    @Deprecated("Use the titles property instead")
    @SerialName("romanized_title")
    val romanizedTitle: String? = null,
    @SerialName("secondary_titles")
    val secondaryTitles: Map<String, List<MangaBakaSecondaryTitle>>? = null,
    val cover: MangaBakaCover,
    val authors: List<String>? = null,
    val artists: List<String>? = null,
    val description: String? = null,
    @Deprecated("The year publication began for the series") val year: Double? = null,
    val published: MangaBakaPublished? = null,
    val status: MangaBakaPublicationStatus,
    @SerialName("is_licensed") val isLicensed: Boolean? = null,
    @SerialName("has_anime") val hasAnime: Boolean? = null,
    val anime: MangaBakaAnimeDetails? = null,
    @SerialName("content_rating") val contentRating: MangaBakaContentRating,
    val type: MangaBakaMediaType,
    val rating: Double? = null,
    @SerialName("final_volume") val finalVolume: String? = null,
    @SerialName("total_chapters") val totalChapters: String? = null,
    val links: List<String>? = emptyList(),
    val publishers: List<MangaBakaPublisher>? = null,
    val titles: List<MangaBakaTitleInfo>? = null,
    @SerialName("genres_v2") val genresV2: List<MangaBakaTagV2>? = null,
    @Deprecated("Use genres_v2 instead") val genres: List<String>? = emptyList(),
    @SerialName("tags_v2") val tagsV2: List<MangaBakaTagV2>? = null,
    @Deprecated("Use tags_v2 instead") val tags: List<String>? = null,
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
    val relationships: MangaBakaRelationships? = null,
    val source: MangaBakaSourceSpecificData,
) {
    fun parseTitle(): String {
        return titles?.firstOrNull { it.traits.contains("native") }?.title
            ?: nativeTitle
            ?: "NO TITLE"
    }
}
