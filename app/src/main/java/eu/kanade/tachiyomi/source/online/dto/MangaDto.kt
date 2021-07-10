package eu.kanade.tachiyomi.source.online.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class MangaListDto(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val results: List<MangaDto>,
)

@Serializable
data class MangaDto(
    val result: String,
    val data: MangaDataDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class MangaDataDto(val id: String, val type: String, val attributes: MangaAttributesDto)

@Serializable
data class MangaAttributesDto(
    val title: Map<String, String>,
    val altTitles: List<JsonElement>,
    val description: JsonElement,
    val links: JsonElement?,
    val originalLanguage: String,
    val lastVolume: String?,
    val lastChapter: String?,
    val contentRating: String?,
    val publicationDemographic: String?,
    val status: String?,
    val year: Int?,
    val tags: List<TagDto>,
)

@Serializable
data class TagDto(
    val id: String,
)

@Serializable
data class RelationshipDto(
    val id: String,
    val type: String,
    val attributes: IncludesAttributesDto? = null,
)

@Serializable
data class IncludesAttributesDto(
    val name: String? = null,
    val fileName: String? = null,
)

@Serializable
data class AuthorListDto(
    val results: List<AuthorDto>,
)

@Serializable
data class AuthorDto(
    val result: String,
    val data: AuthorDataDto,
)

@Serializable
data class AuthorDataDto(
    val id: String,
    val attributes: AuthorAttributesDto,
)

@Serializable
data class AuthorAttributesDto(
    val name: String,
)

@Serializable
data class ReadingStatusDto(
    val status: String?,
)

@Serializable
data class ReadingStatusMapDto(
    val statuses: Map<String, String?>,
)

@Serializable
data class ReadChapterDto(
    val data: List<String>,
)

@Serializable
data class CoverListDto(
    val results: List<CoverDto>,
)

@Serializable
data class CoverDto(
    val data: CoverDataDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class CoverDataDto(
    val attributes: CoverAttributesDto,
)

@Serializable
data class CoverAttributesDto(
    val fileName: String,
)

fun JsonElement.asMdMap(): Map<String, String> {
    return runCatching {
        (this as JsonObject).map { it.key to (it.value.jsonPrimitive.contentOrNull ?: "") }.toMap()
    }.getOrElse { emptyMap() }
}
