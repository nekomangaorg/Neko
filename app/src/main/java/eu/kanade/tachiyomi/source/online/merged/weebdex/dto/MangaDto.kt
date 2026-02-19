package eu.kanade.tachiyomi.source.online.merged.weebdex.dto

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.merged.weebdex.WeebDexHelper
import kotlinx.serialization.Serializable

@Serializable
class MangaListDto(
    val data: List<MangaDto> = emptyList(),
    val page: Int = 1,
    val limit: Int = 0,
    val total: Int = 0,
) {
    val hasNextPage: Boolean
        get() = page * limit < total

    fun toSMangaList(coverQuality: String): List<SManga> {
        val helper = WeebDexHelper()
        return data.map { it.toSManga(coverQuality, helper) }
    }
}

@Serializable
class MangaDto(
    val id: String,
    val title: String,
    val description: String = "",
    val status: String? = null,
    val relationships: RelationshipsDto? = null,
) {
    fun toSManga(coverQuality: String, helper: WeebDexHelper): SManga =
        SManga.create().apply {
            title = this@MangaDto.title
            description = this@MangaDto.description
            status = helper.parseStatus(this@MangaDto.status)
            thumbnail_url = helper.buildCoverUrl(id, relationships?.cover, coverQuality)
            url = "/manga/$id"
            relationships?.let { rel ->
                author = rel.authors.joinToString(", ") { it.name }
                artist = rel.artists.joinToString(", ") { it.name }
                genre = rel.tags.joinToString(", ") { it.name }
            }
        }
}

@Serializable
class RelationshipsDto(
    val authors: List<NamedEntity> = emptyList(),
    val artists: List<NamedEntity> = emptyList(),
    val tags: List<NamedEntity> = emptyList(),
    val cover: CoverDto? = null,
)

@Serializable class NamedEntity(val name: String)

@Serializable class CoverDto(val id: String, val ext: String = ".jpg")
