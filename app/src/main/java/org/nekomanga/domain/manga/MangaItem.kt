package org.nekomanga.domain.manga

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.manga.MangaUtil
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

data class MangaItem(
    val id: Long = 0L,
    val source: Long = 0L,
    val url: String = "",
    val title: String = "",
    val artist: String = "",
    val author: String = "",
    val description: String = "",
    val contentRating: String = "",
    val genre: PersistentList<String> = persistentListOf(),
    val status: Int = 0,
    val coverUrl: String = "",
    val favorite: Boolean = false,
    val lastUpdate: Long = 0L,
    val nextUpdate: Long = 0L,
    val initialized: Boolean = false,
    val viewerFlags: Int = 0,
    val chapterFlags: Int = 0,
    val dateAdded: Long = 0L,
    val followStatus: FollowStatus? = null,
    val langFlag: String = "",
    val anilistId: String = "",
    val kitsuId: String = "",
    val myAnimeListId: String = "",
    val mangaUpdatesId: String = "",
    val animePlanetId: String = "",
    val externalLinks: PersistentList<ExternalLink> = persistentListOf(),
    val filteredScanlators: PersistentList<String> = persistentListOf(),
    val filteredLanguage: PersistentList<String> = persistentListOf(),
    val missingChapters: String = "",
    val rating: String = "",
    val users: String = "",
    val lastVolumeNumber: Int? = null,
    val lastChapterNumber: Int? = null,
    val altTitles: PersistentList<String> = persistentListOf(),
    val userCover: String = "",
    val dynamicCover: String = "",
    val userTitle: String = "",
    val repliesCount: String = "",
    val threadId: String = "",
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
    manga.genre = MangaUtil.genresToString(genre, this.contentRating)
    manga.status = this.status
    manga.thumbnail_url = this.coverUrl.takeIf { it.isNotBlank() }
    manga.favorite = this.favorite
    manga.last_update = this.lastUpdate
    manga.next_update = this.nextUpdate
    manga.initialized = this.initialized
    manga.viewer_flags = this.viewerFlags
    manga.chapter_flags = this.chapterFlags
    manga.date_added = this.dateAdded
    manga.follow_status = this.followStatus
    manga.lang_flag = this.langFlag.takeIf { it.isNotBlank() }
    manga.anilist_id = this.anilistId.takeIf { it.isNotBlank() }
    manga.kitsu_id = this.kitsuId.takeIf { it.isNotBlank() }
    manga.my_anime_list_id = this.myAnimeListId.takeIf { it.isNotBlank() }
    manga.manga_updates_id = this.mangaUpdatesId.takeIf { it.isNotBlank() }
    manga.anime_planet_id = this.animePlanetId.takeIf { it.isNotBlank() }
    manga.other_urls = MangaUtil.externalLinksToOtherString(this.externalLinks)
    manga.filtered_scanlators = ChapterUtil.getScanlatorString(this.filteredScanlators.toSet())
    manga.filtered_language = ChapterUtil.getLanguageString(this.filteredLanguage.toSet())
    manga.missing_chapters = this.missingChapters.takeIf { it.isNotBlank() }
    manga.rating = this.rating.takeIf { it.isNotBlank() }
    manga.users = this.users.takeIf { it.isNotBlank() }
    manga.last_volume_number = this.lastVolumeNumber
    manga.last_chapter_number = this.lastChapterNumber
    manga.alt_titles = MangaUtil.altTitlesToString(this.altTitles)
    manga.dynamic_cover = this.dynamicCover.takeIf { it.isNotBlank() }
    manga.user_cover = this.userCover.takeIf { it.isNotBlank() }
    manga.user_title = this.userTitle.takeIf { it.isNotBlank() }
    manga.replies_count = this.repliesCount.takeIf { it.isNotBlank() }
    manga.thread_id = this.threadId.takeIf { it.isNotBlank() }
    return manga
}

fun Manga.toMangaItem(): MangaItem {
    return MangaItem(
        id = this.id!!,
        source = this.source,
        url = this.url,
        title = this.title,
        artist = this.artist ?: "",
        author = this.author ?: "",
        description = this.description ?: "",
        contentRating = MangaUtil.getContentRating(MangaUtil.getGenres(this.genre)),
        genre = MangaUtil.getGenres(this.genre, true).toPersistentList(),
        status = this.status,
        coverUrl = this.thumbnail_url ?: "",
        favorite = this.favorite,
        lastUpdate = this.last_update,
        nextUpdate = this.next_update,
        initialized = this.initialized,
        viewerFlags = this.viewer_flags,
        chapterFlags = this.chapter_flags,
        dateAdded = this.date_added,
        followStatus = this.follow_status,
        langFlag = this.lang_flag ?: "",
        anilistId = this.anilist_id ?: "",
        kitsuId = this.kitsu_id ?: "",
        myAnimeListId = this.my_anime_list_id ?: "",
        mangaUpdatesId = this.manga_updates_id ?: "",
        animePlanetId = this.anime_planet_id ?: "",
        externalLinks = this.getExternalLinks().toPersistentList(),
        filteredScanlators = ChapterUtil.getScanlators(this.filtered_scanlators).toPersistentList(),
        filteredLanguage = ChapterUtil.getLanguages(this.filtered_language).toPersistentList(),
        missingChapters = this.missing_chapters ?: "",
        rating = this.rating ?: "",
        users = this.users ?: "",
        lastVolumeNumber = this.last_volume_number,
        lastChapterNumber = this.last_chapter_number,
        altTitles = this.getAltTitles().toPersistentList(),
        dynamicCover = this.dynamic_cover ?: "",
        userCover = this.user_cover ?: "",
        userTitle = this.user_title ?: "",
        repliesCount = this.replies_count ?: "",
        threadId = this.thread_id ?: "",
    )
}

fun MangaItem.uuid(): String {
    return MdUtil.getMangaUUID(this.url)
}

fun MangaItem.getDescription(): String {
    return when {
        description.isNotEmpty() -> description
        !initialized -> ""
        else -> "No description"
    }
}
