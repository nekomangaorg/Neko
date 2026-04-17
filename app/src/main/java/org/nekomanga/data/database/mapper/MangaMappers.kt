package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import org.nekomanga.data.database.entity.MangaEntity

fun MangaEntity.toManga(): MangaImpl {
    return MangaImpl().apply {
        id = this@toManga.id
        source = this@toManga.source
        url = this@toManga.url
        artist = this@toManga.artist
        author = this@toManga.author
        description = this@toManga.description
        genre = this@toManga.genre
        title = this@toManga.title
        status = this@toManga.status
        thumbnail_url = this@toManga.thumbnailUrl
        favorite = this@toManga.favorite

        // Handle nullability differences between Entity and Legacy Impl
        last_update = this@toManga.lastUpdate ?: 0L
        next_update = this@toManga.nextUpdate ?: 0L
        date_added = this@toManga.dateAdded ?: 0L

        initialized = this@toManga.initialized
        viewer_flags = this@toManga.viewerFlags
        chapter_flags = this@toManga.chapterFlags
        lang_flag = this@toManga.langFlag
        follow_status = this@toManga.followStatus
        anilist_id = this@toManga.anilistId
        kitsu_id = this@toManga.kitsuId
        my_anime_list_id = this@toManga.myAnimeListId
        manga_updates_id = this@toManga.mangaUpdatesId
        anime_planet_id = this@toManga.animePlanetId
        other_urls = this@toManga.otherUrls
        filtered_scanlators = this@toManga.filteredScanlators
        missing_chapters = this@toManga.missingChapters
        rating = this@toManga.rating
        users = this@toManga.users
        thread_id = this@toManga.threadId
        replies_count = this@toManga.repliesCount
        merge_manga_url = this@toManga.mergeMangaUrl
        last_volume_number = this@toManga.lastVolumeNumber
        last_chapter_number = this@toManga.lastChapterNumber
        merge_manga_image_url = this@toManga.mergeMangaImageUrl
        alt_titles = this@toManga.altTitles
        user_cover = this@toManga.userCover
        user_title = this@toManga.userTitle
        filtered_language = this@toManga.filteredLanguage
        dynamic_cover = this@toManga.dynamicCover
    }
}

fun Manga.toEntity(): MangaEntity {
    return MangaEntity(
        id = this.id ?: 0L, // Safely handles auto-generation for new inserts
        source = this.source,
        url = this.url,
        artist = this.artist,
        author = this.author,
        description = this.description,
        genre = this.genre,
        title = this.title,
        status = this.status,
        thumbnailUrl = this.thumbnail_url,
        favorite = this.favorite,
        lastUpdate = this.last_update,
        nextUpdate = this.next_update,
        dateAdded = this.date_added,
        initialized = this.initialized,
        viewerFlags = this.viewer_flags,
        chapterFlags = this.chapter_flags,
        langFlag = this.lang_flag,

        // Entity requires non-null, Impl allows null. Fall back to un-followed.
        followStatus = this.follow_status ?: FollowStatus.UNFOLLOWED,
        anilistId = this.anilist_id,
        kitsuId = this.kitsu_id,
        myAnimeListId = this.my_anime_list_id,
        mangaUpdatesId = this.manga_updates_id,
        animePlanetId = this.anime_planet_id,
        otherUrls = this.other_urls,
        filteredScanlators = this.filtered_scanlators,
        missingChapters = this.missing_chapters,
        rating = this.rating,
        users = this.users,
        threadId = this.thread_id,
        repliesCount = this.replies_count,
        mergeMangaUrl = this.merge_manga_url,
        lastVolumeNumber = this.last_volume_number,
        lastChapterNumber = this.last_chapter_number,
        mergeMangaImageUrl = this.merge_manga_image_url,
        altTitles = this.alt_titles,
        userCover = this.user_cover,
        userTitle = this.user_title,
        filteredLanguage = this.filtered_language,
        dynamicCover = this.dynamic_cover,
    )
}
