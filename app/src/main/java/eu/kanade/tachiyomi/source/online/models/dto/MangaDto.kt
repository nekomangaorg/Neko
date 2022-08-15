package eu.kanade.tachiyomi.source.online.models.dto

import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class MangaListDto(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val result: String,
    val data: List<MangaDataDto>,
)

@Serializable
data class MangaDto(
    val result: String,
    val data: MangaDataDto,
)

@Serializable
data class MangaDataDto(
    val id: String,
    val type: String,
    val attributes: MangaAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class MangaAttributesDto(
    val title: Map<String, String?>,
    val altTitles: List<JsonElement>?,
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
data class RelationshipDtoList(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val result: String,
    val data: List<RelationshipDto>,
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
    val description: String? = null,
    val locale: String? = null,
    val volume: String? = null,
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
)

@Serializable
data class CoverDataDto(
    val attributes: CoverAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class CoverAttributesDto(
    val fileName: String,
)

@Serializable
data class AggregateDto(
    val result: String,
    val volumes: JsonElement,
)

@Serializable
data class AggregateVolume(
    val volume: String,
    val count: String,
    val chapters: Map<String, AggregateChapter>,
)

@Serializable
data class AggregateChapter(
    val chapter: String,
    val count: String,
)

inline fun <reified T> JsonElement.asMdMap(): Map<String, T> {
    return runCatching {
        MdUtil.jsonParser.decodeFromJsonElement<Map<String, T>>(jsonObject)
    }.getOrElse { emptyMap() }
}
