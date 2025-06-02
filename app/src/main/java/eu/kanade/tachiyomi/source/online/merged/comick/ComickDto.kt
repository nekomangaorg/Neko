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
    val mucomics: Mucomics? = null,
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

@Serializable
data class Mucomics(
    @SerialName("mu_comics") val muComics: BaseMucomic? = null,
)

@Serializable
data class BaseMucomic(
    val titles: List<String>? = null,
    val summary: String? = null,
    val trivia: String? = null,
    val id: Int? = null,
    @SerialName("mu_id") val muId: Int? = null,
    valbayesianrating: Double? = null,
    val rating: Double? = null,
    val ratingvotes: Int? = null,
    val recommendations: String? = null,
    @SerialName("latest_chapter") val latestChapter: Int? = null,
    val year: Int? = null,
    val authors: List<String>? = null,
    val illustrators: List<String>? = null,
    val publishers: List<String>? = null,
    val moreQuery: String? = null,
    val demographic: String? = null,
    val tags: List<String>? = null,
    val categories: List<IdNameSlug>? = null,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val status: String? = null,
    @SerialName("licensed_in_english") val licensedInEnglish: Boolean? = null,
    val comments: List<String>? = null,
    val image: String? = null,
    val titlesalt: List<String>? = null,
    val type: String? = null,
    val rank: Int? = null,
    val users: Int? = null,
    val lists: Int? = null,
    val lastupdated: Int? = null,
    val chaptercount: Int? = null,
    val artists: List<IdNameSlug>? = null,
    val genres: List<IdNameSlug>? = null,
    val pictures: List<IdNameSlug>? = null,
)

@Serializable
data class IdNameSlug(
    val id: Int,
    val name: String,
    val slug: String,
)

@Serializable
data class Recommendation(
    val upCount: Int,
    val downCount: Int,
    @SerialName("relate_id") val relateId: Int,
    @SerialName("comic_id") val comicId: Int,
    val comic: ComickComic,
)

@Serializable
data class Artist(
    val name: String,
    val slug: String,
    val hid: String? = null,
)

@Serializable
data class ChapterList(
    val chapters: List<ComickChapter>,
    val total: Int,
    val limit: Int,
)

@Serializable
data class ComickChapter(
    val hid: String,
    val lang: String,
    val title: String? = "",
    val chapter: String? = "",
    val vol: String? = "",
    @SerialName("group_name") val groupName: List<String>? = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val mdChapter: MDChapter? = null,
    val upCount: Int? = null,
    val downCount: Int? = null,
    val commentCount: Int? = null,
    val mdid: String? = null,
    val mucomics: Mucomics? = null,
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

@Serializable
data class PageList(
    val chapter: ChapterPageData,
)

@Serializable
data class ChapterPageData(
    val hid: String,
    val mdid: String? = null,
    val images: List<Page>,
)

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
    val year: Int?,
    val rating: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val content_rating: String?,
    val last_chapter: String?,
    @SerialName("iso639_1") val lang: String?,
    val demographic: String?,
)

@Serializable
data class TopComics(
    val rank: List<ComickComic>,
    val recentRank: List<ComickComic>,
    val trending: Map<String, List<ComickComic>>,
    @SerialName("dUpdate") val dailyUpdates: List<ComickComic>,
    @SerialName("mUpdate") val monthlyUpdates: List<ComickComic>,
    @SerialName("yUpdate") val yearlyUpdates: List<ComickComic>,
    val news: List<ComickComic>,
    val completions: List<ComickComic>,
    @SerialName("topFollowComics") val topFollowComics: Map<String, List<ComickComic>>,
    @SerialName("topPopularComics") val topPopularComics: Map<String, List<ComickComic>>,
)

@Serializable
data class Genre(
    val name: String,
    val slug: String,
    val group: String,
    val id: Int,
    val hit: Int? = null,
    val comicCount: Int? = null,
)

@Serializable
data class Demographic(
    val name: String,
    val slug: String,
    val group: String,
    val id: Int,
    val hit: Int? = null,
    val comicCount: Int? = null,
)

@Serializable
data class Identity(
    val id: Int,
    val mdid: String?,
    val user: User,
    val roles: List<String>,
    val permissions: List<String>,
    val token: String,
    val hid: String,
    val slug: String,
    val title: String,
    val anilist: Anilist,
    val mal: Mal,
    val discord: String?,
    val isPublisher: Boolean,
)

@Serializable
data class User(
    val id: Int,
    val username: String,
    val mdid: String?,
    val hid: String,
    val slug: String,
    val email: String,
    val emailVerifiedAt: String?,
    val avatar: String?,
    val anilistId: Int?,
    val malId: String?,
    val discordId: String?,
    val isStaff: Boolean,
    val isPro: Boolean,
    val proTier: String?,
    val premiumUntil: String?,
    val chapterLanguages: List<String>,
    val restricted: Boolean,
    val anilist: Anilist,
    val mal: Mal,
    val discord: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class Anilist(
    val id: Int,
    val token: String,
    val refreshToken: String,
    val tokenType: String,
    val expires: Int,
    val avatar: String?,
    val name: String,
)

@Serializable
data class Mal(
    val id: String,
    val token: String,
    val refreshToken: String,
    val tokenType: String,
    val expires: Int,
    val avatar: String?,
    val name: String,
)

@Serializable
data class SLUG_QUERY(
    val slug: String,
    val hid: String,
)

@Serializable
data class HID_QUERY(
    val hid: String,
)

@Serializable
data class Me(
    val user: User,
    val anilist: Anilist?,
    val mal: Mal?,
    val discord: String?,
)

@Serializable
data class Bookmark(
    val hid: String,
    val chapter: BookmarkChapter,
    val comic: BookmarkComic,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BookmarkChapter(
    val hid: String,
    val chap: String,
    val vol: String?,
    val title: String?,
    val lang: String,
    val createdAt: String,
    val updatedAt: String,
    val id: Int,
    val upCount: Int,
    val downCount: Int,
    val groupName: List<String>?,
    val mdid: String?,
    val mucomics: Mucomics?,
    val serverCreatedAt: String,
    val groupNameVersion: Int,
    val bookmarked: Boolean,
    val userLastRead: String?,
    val isExternal: Boolean,
    val anilistId: Int?,
    val demographic: String?,
    val slug: String?,
    val matureContent: Boolean,
    val mdGroups: List<MDcovers>?,
)

@Serializable
data class BookmarkComic(
    val id: Int,
    val hid: String,
    val title: String,
    val slug: String,
    val status: Int,
    val year: Int?,
    val lang: String,
    val mdcovers: List<MDcovers>?,
)

@Serializable
data class ReadMarker(
    val hid: String,
    val guid: String?,
    val chap: String,
    val lang: String,
    val vol: String?,
    val title: String?,
    val comicHid: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ErrorMessage(
    val message: String,
    val status: Int,
    val name: String,
)

@Serializable
data class RequestResult(
    val message: String,
)
