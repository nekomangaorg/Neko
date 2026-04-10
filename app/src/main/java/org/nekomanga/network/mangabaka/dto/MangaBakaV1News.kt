package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaV1News(
    val id: Double? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("source_name") val sourceName: String,
    val title: String,
    val url: String,
    val primary: Boolean,
    val type: MangaBakaNewsType,
    val author: String? = null,
    @SerialName("mentioned_series") val mentionedSeries: List<Double>? = null,
    val series: List<MangaBakaV1Series>? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
