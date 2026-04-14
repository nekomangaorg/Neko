package org.nekomanga.data.database.model

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.domain.chapter.SimpleChapter

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
        last_update = this@toManga.lastUpdate
        next_update = this@toManga.nextUpdate
        initialized = this@toManga.initialized
        viewer_flags = this@toManga.viewerFlags
        chapter_flags = this@toManga.chapterFlags
        date_added = this@toManga.dateAdded
        lang_flag = this@toManga.langFlag
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
        merge_manga_url = this@toManga.mergeMangaUrl
        merge_manga_image_url = this@toManga.mergeMangaImageUrl
        last_volume_number = this@toManga.lastVolumeNumber
        last_chapter_number = this@toManga.lastChapterNumber
        alt_titles = this@toManga.altTitles
        user_cover = this@toManga.userCover
        user_title = this@toManga.userTitle
        filtered_language = this@toManga.filteredLanguage
        dynamic_cover = this@toManga.dynamicCover
        replies_count = this@toManga.repliesCount
        thread_id = this@toManga.threadId
    }
}

fun ChapterEntity.toChapter(): ChapterImpl {
    return ChapterImpl().apply {
        id = this@toChapter.id
        manga_id = this@toChapter.mangaId
        url = this@toChapter.url
        name = this@toChapter.name
        chapter_txt = this@toChapter.chapterTxt
        chapter_title = this@toChapter.chapterTitle
        vol = this@toChapter.vol
        scanlator = this@toChapter.scanlator
        uploader = this@toChapter.uploader
        isUnavailable = this@toChapter.isUnavailable
        read = this@toChapter.read
        bookmark = this@toChapter.bookmark
        last_page_read = this@toChapter.lastPageRead
        pages_left = this@toChapter.pagesLeft
        chapter_number = this@toChapter.chapterNumber
        source_order = this@toChapter.sourceOrder
        smart_order = this@toChapter.smartOrder
        date_fetch = this@toChapter.dateFetch
        date_upload = this@toChapter.dateUpload
        mangadex_chapter_id = this@toChapter.mangadexChapterId ?: ""
        old_mangadex_id = this@toChapter.oldMangadexId
        language = this@toChapter.language
    }
}

fun ChapterEntity.toSimpleChapter(lastRead: Long = 0L): SimpleChapter {
    return SimpleChapter(
        id = id!!,
        mangaId = mangaId,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        pagesLeft = pagesLeft,
        dateFetch = dateFetch,
        sourceOrder = sourceOrder,
        smartOrder = smartOrder,
        url = url,
        name = name,
        dateUpload = dateUpload,
        chapterNumber = chapterNumber,
        scanlator = scanlator ?: "",
        uploader = uploader ?: "",
        volume = vol,
        chapterTitle = chapterTitle,
        chapterText = chapterTxt,
        mangaDexChapterId = mangadexChapterId ?: "",
        oldMangaDexChapterId = oldMangadexId,
        language = language ?: "",
        lastRead = lastRead,
        isUnavailable = isUnavailable,
    )
}

fun MangaChapter.toSimpleChapter(lastRead: Long = 0L): SimpleChapter {
    return chapter.toSimpleChapter(lastRead)
}

fun MangaChapter.toManga(): MangaImpl {
    return manga.toManga()
}

fun MangaChapter.toChapter(): ChapterImpl {
    return chapter.toChapter()
}

fun MangaChapterHistory.toSimpleChapter(): SimpleChapter {
    return chapter.toSimpleChapter(history.lastRead)
}

fun MangaChapterHistory.toManga(): MangaImpl {
    return manga.toManga()
}

fun MangaChapterHistory.toChapter(): ChapterImpl {
    return chapter.toChapter()
}
