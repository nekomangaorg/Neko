package eu.kanade.tachiyomi.source.online.merged.comick

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComickComic(
    val hid: String,
    val title: String,
    val slug: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    val genres: List<String> = emptyList(),
    val demographic: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    val last_chapter: String? = null,
    val mainCover: String? = null,
    val lang: String? = null,
    val year: Int? = null,
    val bayesianRating: String? = null,
    val rating: String? = null,
    val userFollowCount: Int? = null,
    val followRank: Int? = null,
    val commentCount: Int? = null,
    val followCount: Int? = null,
    val desc: String? = null,
    val status: Int? = null,
    val links: Map<String, String>? = null,
    val mdcovers: List<MDcovers>? = null,
    @SerialName("iso639_1") val iso6391: String? = null,
    @SerialName("translation_completed") val translationCompleted: Boolean? = null,
    val chapterCount: Int? = null,
    val contentRating: String? = null,
    val recommendations: List<Recommendation>? = null,
    val authors: List<Artist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    @SerialName("matureContent") val matureContent: Boolean? = null,
)

@Serializable
data class MDcovers(
    val b2key: String? = null,
    val vol: String? = null,
    val w: Int? = null,
    val h: Int? = null,
)

@Serializable data class IdNameSlug(val id: Int, val name: String, val slug: String)

@Serializable
data class Recommendation(
    val upCount: Int,
    val downCount: Int,
    @SerialName("relate_id") val relateId: Int,
    @SerialName("comic_id") val comicId: Int,
    val comic: ComickComic,
)

@Serializable data class Artist(val name: String, val slug: String, val hid: String? = null)

@Serializable
data class ChapterList(val chapters: List<ComickChapter>, val total: Int, val limit: Int)

@Serializable
data class ComickChapter(
    val hid: String,
    val lang: String,
    val title: String? = "",
    val chapter: String? = "",
    val vol: String? = "",
    @SerialName("group_name") val groupName: List<String>? = emptyList(),
    @SerialName("md_chapters_groups") val mdGroupName: List<MDGroups>? = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val mdChapter: MDChapter? = null,
    val upCount: Int? = null,
    val downCount: Int? = null,
    val commentCount: Int? = null,
    val mdid: String? = null,
    @SerialName("server_created_at") val serverCreatedAt: String? = null,
    val id: Int? = null,
    val chap: String? = null,
    @SerialName("group_name_version") val groupNameVersion: Int? = null,
    val bookmarked: Boolean? = null,
    val userLastRead: String? = null,
    val isExternal: Boolean? = null,
    val anilistId: Int? = null,
    val demographic: String? = null,
    val slug: String? = null,
    val matureContent: Boolean? = null,
    val mdGroups: List<MDcovers>? = null,
)

@Serializable data class MDGroups(@SerialName("md_groups") val mdGroup: MDGroup)
@Serializable data class MDGroup(val title: String)

@Serializable
data class MDChapter(
    val title: String? = null,
    val hid: String? = null,
    val chapter: String? = null,
    val vol: String? = null,
    val lang: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val id: Int? = null,
    val group_name: List<String>? = null,
    val server_created_at: String? = null,
    val server_updated_at: String? = null,
    val group_name_version: Int? = null,
    val chapter_id: Int? = null,
    val anilist_id: Int? = null,
    val bookmarked: Boolean? = null,
    val user_last_read: String? = null,
    val is_external: Boolean? = null,
    val anilist_id_v2: Int? = null,
    val demographic: String? = null,
    val slug: String? = null,
    val matureContent: Boolean? = null,
    val md_groups: List<MDcovers>? = null,
)

@Serializable data class PageList(val chapter: ChapterPageData)

@Serializable
data class ChapterPageData(val hid: String, val mdid: String? = null, val images: List<Page>)

@Serializable
data class Page(
    val url: String? = null,
    val h: Int? = null,
    val w: Int? = null,
    val b2key: String? = null,
    val name: String? = null,
    val s: Int? = null,
)

@Serializable
data class SearchResponse(
    val hid: String,
    val slug: String,
    val title: String,
    @SerialName("md_covers") val mdCovers: List<MDcovers> = emptyList(),
)
