package org.nekomanga.data.database.model

import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.HistoryImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.database.models.MangaSimilarImpl
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.BrowseFilterEntity
import org.nekomanga.data.database.entity.CategoryEntity
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MangaSimilarEntity
import org.nekomanga.data.database.entity.MergeMangaEntity
import org.nekomanga.data.database.entity.ScanlatorGroupEntity
import org.nekomanga.data.database.entity.TrackEntity
import org.nekomanga.data.database.entity.UploaderEntity
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

fun Manga.toEntity(): MangaEntity {
    return MangaEntity(
        id = id,
        source = source,
        url = url,
        artist = artist,
        author = author,
        description = description,
        genre = genre,
        title = title,
        status = status,
        thumbnailUrl = thumbnail_url,
        favorite = favorite,
        lastUpdate = last_update,
        nextUpdate = next_update,
        initialized = initialized,
        viewerFlags = viewer_flags,
        chapterFlags = chapter_flags,
        dateAdded = date_added,
        langFlag = lang_flag,
        anilistId = anilist_id,
        kitsuId = kitsu_id,
        myAnimeListId = my_anime_list_id,
        mangaUpdatesId = manga_updates_id,
        animePlanetId = anime_planet_id,
        otherUrls = other_urls,
        filteredScanlators = filtered_scanlators,
        missingChapters = missing_chapters,
        rating = rating,
        users = users,
        mergeMangaUrl = merge_manga_url,
        mergeMangaImageUrl = merge_manga_image_url,
        lastVolumeNumber = last_volume_number,
        lastChapterNumber = last_chapter_number,
        altTitles = alt_titles,
        userCover = user_cover,
        userTitle = user_title,
        filteredLanguage = filtered_language,
        dynamicCover = dynamic_cover,
        repliesCount = replies_count,
        threadId = thread_id,
    )
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

fun Chapter.toEntity(): ChapterEntity {
    return ChapterEntity(
        id = id,
        mangaId = manga_id!!,
        url = url,
        name = name,
        chapterTxt = chapter_txt,
        chapterTitle = chapter_title,
        vol = vol,
        scanlator = scanlator,
        uploader = uploader,
        isUnavailable = isUnavailable,
        read = read,
        bookmark = bookmark,
        lastPageRead = last_page_read,
        pagesLeft = pages_left,
        chapterNumber = chapter_number,
        sourceOrder = source_order,
        smartOrder = smart_order,
        dateFetch = date_fetch,
        dateUpload = date_upload,
        mangadexChapterId = mangadex_chapter_id,
        oldMangadexId = oldMangadexId,
        language = language,
    )
}

fun SimpleChapter.toEntity(): ChapterEntity {
    return ChapterEntity(
        id = id,
        mangaId = mangaId,
        url = url,
        name = name,
        chapterTxt = chapterText,
        chapterTitle = chapterTitle,
        vol = volume,
        scanlator = scanlator,
        uploader = uploader,
        isUnavailable = isUnavailable,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        pagesLeft = pagesLeft,
        chapterNumber = chapterNumber,
        sourceOrder = sourceOrder,
        smartOrder = smartOrder,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        mangadexChapterId = mangaDexChapterId,
        oldMangaDexChapterId = oldMangaDexChapterId,
        language = language,
    )
}

fun CategoryEntity.toCategory(): CategoryImpl {
    return CategoryImpl().apply {
        id = this@toCategory.id
        name = this@toCategory.name
        order = this@toCategory.order
        flags = this@toCategory.flags
        mangaOrder = this@toCategory.mangaOrder.split("/").mapNotNull { it.toLongOrNull() }
    }
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        order = order,
        flags = flags,
        mangaOrder = mangaOrder.joinToString("/"),
    )
}

fun TrackEntity.toTrack(): TrackImpl {
    return TrackImpl().apply {
        id = this@toTrack.id
        manga_id = this@toTrack.mangaId
        sync_id = this@toTrack.syncId
        media_id = this@toTrack.mediaId
        library_id = this@toTrack.libraryId
        title = this@toTrack.title
        last_chapter_read = this@toTrack.lastChapterRead
        total_chapters = this@toTrack.totalChapters
        status = this@toTrack.status
        score = this@toTrack.score
        tracking_url = this@toTrack.trackingUrl
        started_reading_date = this@toTrack.startedReadingDate
        finished_reading_date = this@toTrack.finishedReadingDate
    }
}

fun Track.toEntity(): TrackEntity {
    return TrackEntity(
        id = id,
        mangaId = manga_id,
        syncId = sync_id,
        mediaId = media_id,
        libraryId = library_id,
        title = title,
        lastChapterRead = last_chapter_read,
        totalChapters = total_chapters,
        status = status,
        score = score,
        trackingUrl = tracking_url,
        startedReadingDate = started_reading_date,
        finishedReadingDate = finished_reading_date,
    )
}

fun MergeMangaEntity.toMergeManga(): MergeMangaImpl {
    return MergeMangaImpl(
        id = this@toMergeManga.id,
        mangaId = this@toMergeManga.mangaId,
        coverUrl = this@toMergeManga.coverUrl,
        title = this@toMergeManga.title,
        url = this@toMergeManga.url,
        mergeType = MergeType.getById(this@toMergeManga.mergeType),
    )
}

fun MergeMangaImpl.toEntity(): MergeMangaEntity {
    return MergeMangaEntity(
        id = id,
        mangaId = mangaId,
        coverUrl = coverUrl,
        title = title,
        url = url,
        mergeType = mergeType.id,
    )
}

fun HistoryEntity.toHistory(): HistoryImpl {
    return HistoryImpl().apply {
        id = this@toHistory.id
        chapter_id = this@toHistory.chapterId
        last_read = this@toHistory.lastRead
        time_read = this@toHistory.timeRead
    }
}

fun History.toEntity(): HistoryEntity {
    return HistoryEntity(
        id = id,
        chapterId = chapter_id,
        lastRead = last_read,
        timeRead = time_read,
    )
}

fun MangaCategoryEntity.toMangaCategory(): MangaCategory {
    return MangaCategory().apply {
        id = this@toMangaCategory.id?.toLong()
        manga_id = this@toMangaCategory.mangaId
        category_id = this@toMangaCategory.categoryId
    }
}

fun MangaCategory.toEntity(): MangaCategoryEntity {
    return MangaCategoryEntity(
        id = id?.toInt(),
        mangaId = manga_id,
        categoryId = category_id,
    )
}

fun ScanlatorGroupImpl.toEntity(): ScanlatorGroupEntity {
    return ScanlatorGroupEntity(
        id = id,
        name = name,
        uuid = uuid,
        description = description,
    )
}

fun UploaderImpl.toEntity(): UploaderEntity {
    return UploaderEntity(
        id = id,
        username = username,
        uuid = uuid,
    )
}

fun ArtworkImpl.toEntity(): ArtworkEntity {
    return ArtworkEntity(
        id = id,
        mangaId = mangaId,
        fileName = fileName,
        volume = volume,
        locale = locale,
        description = description,
    )
}

fun BrowseFilterEntity.toBrowseFilter(): BrowseFilterImpl {
    return BrowseFilterImpl(
        id = id,
        name = name,
        default = isDefault,
        dexFilters = dexFilters,
    )
}

fun BrowseFilterImpl.toEntity(): BrowseFilterEntity {
    return BrowseFilterEntity(
        id = id,
        name = name,
        isDefault = default,
        dexFilters = dexFilters,
    )
}

fun MangaSimilarEntity.toMangaSimilar(): MangaSimilarImpl {
    return MangaSimilarImpl().apply {
        id = this@toMangaSimilar.id
        manga_id = this@toMangaSimilar.mangaId
        data = this@toMangaSimilar.data
    }
}

fun MangaSimilar.toEntity(): MangaSimilarEntity {
    return MangaSimilarEntity(
        id = id,
        mangaId = manga_id,
        data = data,
    )
}

fun LibraryManga.toLegacyModel(): eu.kanade.tachiyomi.data.database.models.LibraryManga {
    val manga = this.manga.toManga()
    return eu.kanade.tachiyomi.data.database.models.LibraryManga().apply {
        id = manga.id
        source = manga.source
        url = manga.url
        artist = manga.artist
        author = manga.author
        description = manga.description
        genre = manga.genre
        title = manga.title
        status = manga.status
        thumbnail_url = manga.thumbnail_url
        favorite = manga.favorite
        last_update = manga.last_update
        next_update = manga.next_update
        initialized = manga.initialized
        viewer_flags = manga.viewer_flags
        chapter_flags = manga.chapter_flags
        date_added = manga.date_added
        lang_flag = manga.lang_flag
        anilist_id = manga.anilist_id
        kitsu_id = manga.kitsu_id
        my_anime_list_id = manga.my_anime_list_id
        manga_updates_id = manga.manga_updates_id
        anime_planet_id = manga.anime_planet_id
        other_urls = manga.other_urls
        filtered_scanlators = manga.filtered_scanlators
        missing_chapters = manga.missing_chapters
        rating = manga.rating
        users = manga.users
        merge_manga_url = manga.merge_manga_url
        merge_manga_image_url = manga.merge_manga_image_url
        last_volume_number = manga.last_volume_number
        last_chapter_number = manga.last_chapter_number
        alt_titles = manga.alt_titles
        user_cover = manga.user_cover
        user_title = manga.user_title
        filtered_language = manga.filtered_language
        dynamic_cover = manga.dynamic_cover
        replies_count = manga.replies_count
        thread_id = manga.thread_id

        unread = unreadCount
        read = readCount
        category = this@toLegacyModel.category
        bookmarkCount = this@toLegacyModel.bookmarkCount
        unavailableCount = this@toLegacyModel.unavailableCount
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
        mangaDexChapterId = mangadexChapterId ?: "" ?: "",
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
