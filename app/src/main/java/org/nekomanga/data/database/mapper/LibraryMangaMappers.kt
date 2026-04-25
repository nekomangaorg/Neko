package org.nekomanga.data.database.mapper

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.data.database.dao.LibraryDao
import org.nekomanga.data.database.model.LibraryMangaRaw

fun LibraryMangaRaw.toLibraryManga(
    blockedGroups: Set<String>,
    blockedUploaders: Set<String>,
    scanlatorFilterOption: Int,
): LibraryManga {
    val raw = this

    return LibraryManga().apply {
        // 1. Map the base Manga properties using our previous MangaEntity mapper
        val baseManga = raw.manga.toManga()
        this.id = baseManga.id
        this.source = baseManga.source
        this.url = baseManga.url
        this.title = baseManga.title
        this.artist = baseManga.artist
        this.author = baseManga.author
        this.description = baseManga.description
        this.genre = baseManga.genre
        this.status = baseManga.status
        this.thumbnail_url = baseManga.thumbnail_url
        this.favorite = baseManga.favorite
        this.last_update = baseManga.last_update
        this.next_update = baseManga.next_update
        this.initialized = baseManga.initialized
        this.viewer_flags = baseManga.viewer_flags
        this.chapter_flags = baseManga.chapter_flags
        this.date_added = baseManga.date_added
        this.follow_status = baseManga.follow_status
        this.lang_flag = baseManga.lang_flag
        this.anilist_id = baseManga.anilist_id
        this.kitsu_id = baseManga.kitsu_id
        this.my_anime_list_id = baseManga.my_anime_list_id
        this.manga_updates_id = baseManga.manga_updates_id
        this.anime_planet_id = baseManga.anime_planet_id
        this.other_urls = baseManga.other_urls
        this.filtered_scanlators = baseManga.filtered_scanlators
        this.missing_chapters = baseManga.missing_chapters
        this.rating = baseManga.rating
        this.users = baseManga.users
        this.thread_id = baseManga.thread_id
        this.replies_count = baseManga.replies_count
        this.merge_manga_url = baseManga.merge_manga_url
        this.last_volume_number = baseManga.last_volume_number
        this.last_chapter_number = baseManga.last_chapter_number
        this.merge_manga_image_url = baseManga.merge_manga_image_url
        this.alt_titles = baseManga.alt_titles
        this.user_cover = baseManga.user_cover
        this.user_title = baseManga.user_title
        this.filtered_language = baseManga.filtered_language
        this.dynamic_cover = baseManga.dynamic_cover

        // 2. Map the Library specific columns
        this.category = raw.category
        this.bookmarkCount = raw.bookmarkCount
        this.unavailableCount = raw.unavailableCount
        this.isMerged = raw.isMerged

        // 3. Parse the concatenated chapter strings
        this.unread =
            parseChapterCount(
                raw.unread,
                baseManga.filtered_scanlators,
                blockedGroups,
                blockedUploaders,
                scanlatorFilterOption,
            )

        this.read =
            parseChapterCount(
                raw.hasRead,
                baseManga.filtered_scanlators,
                blockedGroups,
                blockedUploaders,
                scanlatorFilterOption,
            )
    }
}

private fun parseChapterCount(
    countString: String?,
    filteredScanlatorsString: String?,
    blockedGroups: Set<String>,
    blockedUploaders: Set<String>,
    scanlatorFilterOption: Int,
): Int {
    if (countString.isNullOrBlank()) return 0

    // TODO switch this to enum
    // 0 is all 1 is any
    val scanlatorMatchAll = scanlatorFilterOption == 0
    var validChapterCount = 0
    var startIndex = 0

    val sources = SourceManager.mergeSourceNames + MdConstants.name
    val filtered = ChapterUtil.getScanlators(filteredScanlatorsString).toSet()

    while (startIndex < countString.length) {
        val endIndex = countString.indexOf(LibraryDao.RAW_CHAPTER_SEPARATOR, startIndex)
        val groupString =
            if (endIndex == -1) {
                countString.substring(startIndex)
            } else {
                countString.substring(startIndex, endIndex)
            }

        val parts = groupString.split(LibraryDao.RAW_SCANLATOR_TYPE_SEPARATOR)
        if (parts.size == 2) {
            val scanlator = parts[0]
            val extraParts = parts[1].split(LibraryDao.RAW_CHAPTER_COUNT_SEPARATOR)

            if (extraParts.size == 2) {
                val uploader = extraParts[0]
                val currentGroupCount = extraParts[1].toIntOrNull() ?: 0
                val scanlators = ChapterUtil.getScanlators(scanlator)

                var isFilteredOut = false

                // First check the sources
                for (source in sources) {
                    if (
                        ChapterUtil.filteredBySource(
                            source,
                            scanlators,
                            MergeType.containsMergeSourceName(scanlator),
                            scanlator == Constants.LOCAL_SOURCE,
                            filtered,
                        )
                    ) {
                        isFilteredOut = true
                        break
                    }
                }

                if (!isFilteredOut) {
                    if (
                        ChapterUtil.filterByScanlator(
                            scanlators,
                            uploader,
                            scanlatorMatchAll,
                            filtered,
                        )
                    ) {
                        isFilteredOut = true
                    }
                }

                // Add blocking logic from preferences
                if (!isFilteredOut) {
                    val groupBlocked =
                        blockedGroups.isNotEmpty() && scanlators.any { it in blockedGroups }
                    val uploaderBlocked =
                        blockedUploaders.isNotEmpty() && uploader in blockedUploaders
                    if (groupBlocked || uploaderBlocked) {
                        isFilteredOut = true
                    }
                }

                if (!isFilteredOut) {
                    validChapterCount += currentGroupCount
                }
            }
        }

        if (endIndex == -1) break
        startIndex = endIndex + LibraryDao.RAW_CHAPTER_SEPARATOR.length
    }

    return validChapterCount
}
