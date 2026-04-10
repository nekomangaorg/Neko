package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MangaBakaSeriesState {
    @SerialName("active") ACTIVE,
    @SerialName("merged") MERGED,
    @SerialName("deleted") DELETED,
}

@Serializable
enum class MangaBakaPublicationStatus {
    @SerialName("cancelled") CANCELLED,
    @SerialName("completed") COMPLETED,
    @SerialName("hiatus") HIATUS,
    @SerialName("releasing") RELEASING,
    @SerialName("unknown") UNKNOWN,
    @SerialName("upcoming") UPCOMING,
}

@Serializable
enum class MangaBakaContentRating {
    @SerialName("safe") SAFE,
    @SerialName("suggestive") SUGGESTIVE,
    @SerialName("erotica") EROTICA,
    @SerialName("pornographic") PORNOGRAPHIC,
}

@Serializable
enum class MangaBakaMediaType {
    @SerialName("manga") MANGA,
    @SerialName("novel") NOVEL,
    @SerialName("manhwa") MANHWA,
    @SerialName("manhua") MANHUA,
    @SerialName("oel") OEL,
    @SerialName("other") OTHER,
}

@Serializable
enum class MangaBakaNewsType {
    @SerialName("default") DEFAULT,
    @SerialName("review") REVIEW,
    @SerialName("releases") RELEASES,
}
