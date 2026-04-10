package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaSecondaryTitle(
    val type: String,
    val title: String,
    val note: String? = null
)

@Serializable
data class MangaBakaTitleInfo(
    val language: String,
    val traits: List<String>,
    val title: String,
    val note: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean? = false
)

@Serializable
data class MangaBakaCover(
    val raw: MangaBakaCoverRaw,
    val x150: MangaBakaCoverScaled,
    val x250: MangaBakaCoverScaled,
    val x350: MangaBakaCoverScaled
)

@Serializable
data class MangaBakaCoverRaw(
    val url: String?,
    val size: Double? = null,
    val height: Double? = null,
    val width: Double? = null,
    val blurhash: String? = null,
    val thumbhash: String? = null,
    val format: String? = null
)

@Serializable
data class MangaBakaCoverScaled(
    val x1: String?,
    val x2: String?,
    val x3: String?
)
