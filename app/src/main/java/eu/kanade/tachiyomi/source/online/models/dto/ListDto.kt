package eu.kanade.tachiyomi.source.online.models.dto

@kotlinx.serialization.Serializable data class ListDto(val data: ListDataDto)

@kotlinx.serialization.Serializable
data class ListDataDto(val attributes: ListAttributesDto, val relationships: List<RelationshipDto>)

@kotlinx.serialization.Serializable data class ListAttributesDto(val name: String?)
