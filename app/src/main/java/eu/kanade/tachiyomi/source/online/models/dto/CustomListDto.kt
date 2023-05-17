package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomListListDto(
    val data: List<CustomListDataDto>,
)

@Serializable
data class CustomListDto(
    val data: CustomListDataDto,
)

@Serializable
data class CustomListDataDto(
    val id: String,
    val attributes: ListAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class ListAttributesDto(
    val name: String,
    val visibility: String,
)
