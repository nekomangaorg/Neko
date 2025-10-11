package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil

data class MangaItem(
    val id: Long?,
    val source: Long,
    val url: String,
    val title: String,
    val artist: String,
    val author: String,
    val description: String,
    val genre: String,
    val status: Int,
    val thumbnail_url: String,
    val favorite: Boolean,
    val last_update: Long,
    val next_update: Long,
    val initialized: Boolean,
    val viewer_flags: Int,
    val chapter_flags: Int,
    val date_added: Long,
    val follow_status: FollowStatus?,
    val lang_flag: String,
    val anilist_id: String,
    val kitsu_id: String,
    val my_anime_list_id: String,
    val manga_updates_id: String,
    val anime_planet_id: String,
    val other_urls: String,
    val filtered_scanlators: String,
    val filtered_language: String,
    val missing_chapters: String,
    val rating: String,
    val users: String,
    val merge_manga_url: String,
    val merge_manga_image_url: String,
    val last_volume_number: Int?,
    val last_chapter_number: Int?,
    val alt_titles: String,
    val user_cover: String,
    val user_title: String,
    val replies_count: String,
    val thread_id: String,
    val ogTitle: String,
)

fun MangaItem.toManga(): Manga {
    val manga = MangaImpl()
    manga.id = this.id
    manga.source = this.source
    manga.url = this.url
    manga.title = this.title
    manga.artist = this.artist.takeIf { it.isNotBlank() }
    manga.author = this.author.takeIf { it.isNotBlank() }
    manga.description = this.description.takeIf { it.isNotBlank() }
    manga.genre = this.genre.takeIf { it.isNotBlank() }
    manga.status = this.status
    manga.thumbnail_url = this.thumbnail_url.takeIf { it.isNotBlank() }
    manga.favorite = this.favorite
    manga.last_update = this.last_update
    manga.next_update = this.next_update
    manga.initialized = this.initialized
    manga.viewer_flags = this.viewer_flags
    manga.chapter_flags = this.chapter_flags
    manga.date_added = this.date_added
    manga.follow_status = this.follow_status
    manga.lang_flag = this.lang_flag.takeIf { it.isNotBlank() }
    manga.anilist_id = this.anilist_id.takeIf { it.isNotBlank() }
    manga.kitsu_id = this.kitsu_id.takeIf { it.isNotBlank() }
    manga.my_anime_list_id = this.my_anime_list_id.takeIf { it.isNotBlank() }
    manga.manga_updates_id = this.manga_updates_id.takeIf { it.isNotBlank() }
    manga.anime_planet_id = this.anime_planet_id.takeIf { it.isNotBlank() }
    manga.other_urls = this.other_urls.takeIf { it.isNotBlank() }
    manga.filtered_scanlators = this.filtered_scanlators.takeIf { it.isNotBlank() }
    manga.filtered_language = this.filtered_language.takeIf { it.isNotBlank() }
    manga.missing_chapters = this.missing_chapters.takeIf { it.isNotBlank() }
    manga.rating = this.rating.takeIf { it.isNotBlank() }
    manga.users = this.users.takeIf { it.isNotBlank() }
    manga.merge_manga_url = this.merge_manga_url.takeIf { it.isNotBlank() }
    manga.merge_manga_image_url = this.merge_manga_image_url.takeIf { it.isNotBlank() }
    manga.last_volume_number = this.last_volume_number
    manga.last_chapter_number = this.last_chapter_number
    manga.alt_titles = this.alt_titles.takeIf { it.isNotBlank() }
    manga.user_cover = this.user_cover.takeIf { it.isNotBlank() }
    manga.user_title = this.user_title.takeIf { it.isNotBlank() }
    manga.replies_count = this.replies_count.takeIf { it.isNotBlank() }
    manga.thread_id = this.thread_id.takeIf { it.isNotBlank() }
    return manga
}

fun Manga.toMangaItem(): MangaItem {
    return MangaItem(
        id = this.id,
        source = this.source,
        url = this.url,
        title = this.title,
        artist = this.artist ?: "",
        author = this.author ?: "",
        description = this.description ?: "",
        genre = this.genre ?: "",
        status = this.status,
        thumbnail_url = this.thumbnail_url ?: "",
        favorite = this.favorite,
        last_update = this.last_update,
        next_update = this.next_update,
        initialized = this.initialized,
        viewer_flags = this.viewer_flags,
        chapter_flags = this.chapter_flags,
        date_added = this.date_added,
        follow_status = this.follow_status,
        lang_flag = this.lang_flag ?: "",
        anilist_id = this.anilist_id ?: "",
        kitsu_id = this.kitsu_id ?: "",
        my_anime_list_id = this.my_anime_list_id ?: "",
        manga_updates_id = this.manga_updates_id ?: "",
        anime_planet_id = this.anime_planet_id ?: "",
        other_urls = this.other_urls ?: "",
        filtered_scanlators = this.filtered_scanlators ?: "",
        filtered_language = this.filtered_language ?: "",
        missing_chapters = this.missing_chapters ?: "",
        rating = this.rating ?: "",
        users = this.users ?: "",
        merge_manga_url = this.merge_manga_url ?: "",
        merge_manga_image_url = this.merge_manga_image_url ?: "",
        last_volume_number = this.last_volume_number,
        last_chapter_number = this.last_chapter_number,
        alt_titles = this.alt_titles ?: "",
        user_cover = this.user_cover ?: "",
        user_title = this.user_title ?: "",
        replies_count = this.replies_count ?: "",
        thread_id = this.thread_id ?: "",
        ogTitle = this.originalTitle,
    )
}

fun MangaItem.uuid(): String {
    return MdUtil.getMangaUUID(this.url)
}
