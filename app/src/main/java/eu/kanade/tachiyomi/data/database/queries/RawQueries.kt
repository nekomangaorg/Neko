package eu.kanade.tachiyomi.data.database.queries

import eu.kanade.tachiyomi.data.database.tables.CategoryTable as Category
import eu.kanade.tachiyomi.data.database.tables.ChapterTable as Chapter
import eu.kanade.tachiyomi.data.database.tables.HistoryTable as History
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable as MangaCategory
import eu.kanade.tachiyomi.data.database.tables.MangaTable as Manga

/** Query to get the manga from the library, with their categories and unread count. */
val libraryQuery =
    """
    SELECT M.*, COALESCE(MC.${MangaCategory.COL_CATEGORY_ID}, 0) AS ${Manga.COL_CATEGORY}
    FROM (
        SELECT ${Manga.TABLE}.*, COALESCE(C.unread, '') AS ${Manga.COL_UNREAD}, COALESCE(R.hasread, '') AS ${Manga.COL_HAS_READ}, COALESCE(B.bookmarkCount, 0) AS ${Manga.COL_BOOKMARK_COUNT}, COALESCE(U.unavailableCount,0) as ${Manga.COL_UNAVAILABLE_COUNT}
        FROM ${Manga.TABLE}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, GROUP_CONCAT(IFNULL(${Chapter.TABLE}.${Chapter.COL_SCANLATOR}, 'N/A') || ' [;] ' || IFNULL(${Chapter.TABLE}.${Chapter.COL_UPLOADER}, 'N/A'), ' [.] ') AS unread
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_READ} = 0
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS C
        ON ${Manga.COL_ID} = C.${Chapter.COL_MANGA_ID}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, GROUP_CONCAT(IFNULL(${Chapter.TABLE}.${Chapter.COL_SCANLATOR}, 'N/A') || ' [;] ' || IFNULL(${Chapter.TABLE}.${Chapter.COL_UPLOADER}, 'N/A'), ' [.] ') AS hasread
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_READ} = 1
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS R
        ON ${Manga.COL_ID} = R.${Chapter.COL_MANGA_ID}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, COUNT(*) AS bookmarkCount
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_BOOKMARK} = 1
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS B
        ON ${Manga.COL_ID} = B.${Chapter.COL_MANGA_ID}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, COUNT(*) AS unavailableCount
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_UNAVAILABLE} = true
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS U
        ON ${Manga.COL_ID} = U.${Chapter.COL_MANGA_ID}
        WHERE ${Manga.COL_FAVORITE} = 1
        GROUP BY ${Manga.COL_ID}
        ORDER BY ${Manga.COL_TITLE}
    ) AS M
    LEFT JOIN (
        SELECT * FROM ${MangaCategory.TABLE}) AS MC
        ON MC.${MangaCategory.COL_MANGA_ID} = M.${Manga.COL_ID}
    """

/** Query to get the recent chapters of manga from the library up to a date. */
fun getRecentsQuery(search: String, offset: Int, limit: Int, sortByDateFetched: Boolean): String {
    val orderBy =
        when (sortByDateFetched) {
            true -> Chapter.COL_DATE_FETCH
            false -> Chapter.COL_DATE_UPLOAD
        }

    return """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, * FROM ${Manga.TABLE} JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    WHERE ${Manga.COL_FAVORITE} = 1
    AND ${Chapter.COL_DATE_FETCH} > ${Manga.COL_DATE_ADDED}
    AND lower(${Manga.COL_TITLE}) LIKE '%$search%'
    AND ${Chapter.COL_UNAVAILABLE} = 0
    ORDER BY $orderBy DESC
    LIMIT $limit
    OFFSET $offset
"""
}

fun limitAndOffset(endless: Boolean, isResuming: Boolean, offset: Int, limit: Int): String {
    return when {
        isResuming && endless && offset > 0 -> "LIMIT $offset"
        endless -> "LIMIT ${limit}\nOFFSET $offset"
        else -> "LIMIT $limit"
    }
}

/**
 * Query to get the recently read chapters of manga from the library up to a date. The max_last_read
 * table contains the most recent chapters grouped by manga The select statement returns all
 * information of chapters that have the same id as the chapter in max_last_read and are read after
 * the given time period
 */
fun getRecentHistoryUngrouped(
    search: String = "",
    offset: Int = 0,
    limit: Int = 25,
    isResuming: Boolean,
) =
    """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, ${Manga.TABLE}.*, ${Chapter.TABLE}.*, ${History.TABLE}.*
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    AND ${Chapter.TABLE}.${Chapter.COL_UNAVAILABLE} = 0
    JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    AND ${History.TABLE}.${History.COL_LAST_READ} > 0
    AND lower(${Manga.TABLE}.${Manga.COL_TITLE}) LIKE '%$search%'
    ORDER BY ${History.TABLE}.${History.COL_LAST_READ} DESC
    ${limitAndOffset(true, isResuming, offset,limit)}
"""

/**
 * Query to get the recently read chapters of manga from the library up to a date. The max_last_read
 * table contains the most recent chapters grouped by manga The select statement returns all
 * information of chapters that have the same id as the chapter in max_last_read and are read after
 * the given time period
 */
fun getAllChapterHistoryByMangaId(mangaId: Long) =
    """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, ${Manga.TABLE}.*, ${Chapter.TABLE}.*, ${History.TABLE}.*
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    AND ${Chapter.TABLE}.${Chapter.COL_UNAVAILABLE} = 0
    JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    AND ${History.TABLE}.${History.COL_LAST_READ} > 0
    WHERE ${Manga.TABLE}.${Manga.COL_ID} = ${mangaId}
    ORDER BY ${History.TABLE}.${History.COL_LAST_READ} DESC
    LIMIT 25
"""

/**
 * Query to get the recently read chapters of manga from the library up to a date. The max_last_read
 * table contains the most recent chapters grouped by manga The select statement returns all
 * information of chapters that have the same id as the chapter in max_last_read and are read after
 * the given time period
 */
fun getRecentMangasLimitQuery(
    search: String = "",
    offset: Int = 0,
    limit: Int = 25,
    isResuming: Boolean,
) =
    """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, ${Manga.TABLE}.*, ${Chapter.TABLE}.*, ${History.TABLE}.*
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    AND ${Chapter.TABLE}.${Chapter.COL_UNAVAILABLE} = 0
    JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    JOIN (
    SELECT ${Chapter.TABLE}.${Chapter.COL_MANGA_ID},${Chapter.TABLE}.${Chapter.COL_ID} as ${History.COL_CHAPTER_ID}, MAX(${History.TABLE}.${History.COL_LAST_READ}) as ${History.COL_LAST_READ}
    FROM ${Chapter.TABLE} JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    GROUP BY ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}) AS max_last_read
    ON ${Chapter.TABLE}.${Chapter.COL_MANGA_ID} = max_last_read.${Chapter.COL_MANGA_ID}
    AND max_last_read.${History.COL_CHAPTER_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    AND max_last_read.${History.COL_LAST_READ} > 0
    AND lower(${Manga.TABLE}.${Manga.COL_TITLE}) LIKE '%$search%'
    ORDER BY max_last_read.${History.COL_LAST_READ} DESC
    ${limitAndOffset(true, isResuming, offset, limit)}
"""

/**
 * Query to get the read chapters of manga from the library during the period. The max_last_read
 * table contains the most recent chapters grouped by manga The select statement returns all
 * information of chapters that have the same id as the chapter in max_last_read and are read after
 * the given time period
 */
fun getHistoryPerPeriodQuery(startDate: Long, endDate: Long) =
    """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, ${Manga.TABLE}.*, ${Chapter.TABLE}.*, ${History.TABLE}.*
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    AND ${History.TABLE}.${History.COL_LAST_READ} >= $startDate
    AND ${History.TABLE}.${History.COL_LAST_READ} <= $endDate
    ORDER BY ${History.TABLE}.${History.COL_LAST_READ} DESC
"""

/**
 * Query to get the recently read manga that has more chapters to read The first from checks that
 * there's an unread chapter The max_last_read table contains the most recent chapters grouped by
 * manga The select statement returns all information of chapters that have the same id as the
 * chapter in max_last_read and are read after the given time period The Second Union/Select gets
 * recents chapters Final Union gets newly added manga
 */
fun getAllRecentsType(
    search: String = "",
    includeRead: Boolean,
    endless: Boolean,
    offset: Int = 0,
    limit: Int = 25,
    isResuming: Boolean,
) =
    """
    SELECT * FROM
    (SELECT mangas.url as mangaUrl, mangas.*, chapters.*, history.*
    FROM (
        SELECT mangas.*
        FROM mangas
        LEFT JOIN (
            SELECT manga_id, COUNT(*) AS unread
            FROM chapters
            WHERE read = 0
            GROUP BY manga_id
        ) AS C
        ON _id = C.manga_id
        ${if (includeRead) "" else "WHERE C.unread > 0"}
        GROUP BY _id
        ORDER BY title
    ) AS mangas
    JOIN chapters
    ON mangas._id = chapters.manga_id
    JOIN history
    ON chapters._id = history.history_chapter_id
     JOIN (
        SELECT ${Chapter.TABLE}.${Chapter.COL_MANGA_ID},${Chapter.TABLE}.${Chapter.COL_ID} as ${History.COL_CHAPTER_ID}, MAX(${History.TABLE}.${History.COL_LAST_READ}) as ${History.COL_LAST_READ}
        FROM ${Chapter.TABLE} JOIN ${History.TABLE}
        ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
        GROUP BY ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}) AS max_last_read
    ON ${Chapter.TABLE}.${Chapter.COL_MANGA_ID} = max_last_read.${Chapter.COL_MANGA_ID}
    AND max_last_read.${History.COL_CHAPTER_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    AND max_last_read.${History.COL_LAST_READ} > 0
    AND lower(${Manga.COL_TITLE}) LIKE '%$search%')
    UNION
    SELECT * FROM
    (SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, ${Manga.TABLE}.*, ${Chapter.TABLE}.*,
        Null as history_id,
        Null as history_chapter_id,
        chapters.date_fetch as history_last_read,
        Null as history_time_read
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    JOIN (
        SELECT ${Chapter.TABLE}.${Chapter.COL_MANGA_ID},${Chapter.TABLE}.${Chapter.COL_ID} as ${History.COL_CHAPTER_ID},MAX(${Chapter.TABLE}.${Chapter.COL_DATE_UPLOAD})
        FROM ${Chapter.TABLE} JOIN ${Manga.TABLE}
        ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
        WHERE ${Chapter.COL_READ} = 0
        GROUP BY ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}) AS newest_chapter
    ON ${Chapter.TABLE}.${Chapter.COL_MANGA_ID} = newest_chapter.${Chapter.COL_MANGA_ID}
    WHERE ${Manga.COL_FAVORITE} = 1
    AND newest_chapter.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
    AND ${Chapter.COL_DATE_FETCH} > ${Manga.COL_DATE_ADDED}
    AND lower(${Manga.COL_TITLE}) LIKE '%$search%')
    UNION
    SELECT * FROM
    (SELECT mangas.url as mangaUrl,
        mangas.*,
        Null as _id,
        Null as manga_id,
        Null as url,
        Null as name,
        Null as read,
        Null as scanlator,
        Null as bookmark,
        Null as date_fetch,
        Null as date_upload,
        Null as last_page_read,
        Null as pages_left,
        Null as chapter_number,
        Null as source_order,
        Null as smart_order,
        Null as vol,
        Null as chapter_txt,
        Null as chapter_title,
        Null as mangadex_chapter_id,
        Null as old_mangadex_chapter_id,
        Null as language,
        Null as history_id,
        Null as history_chapter_id,
        ${Manga.TABLE}.${Manga.COL_DATE_ADDED} as history_last_read,
        Null as history_time_read
        FROM mangas
    WHERE ${Manga.COL_FAVORITE} = 1
    AND lower(${Manga.COL_TITLE}) LIKE '%$search%')
    ORDER BY history_last_read DESC
    ${limitAndOffset(endless, isResuming, offset, limit)}
"""

fun getHistoryByMangaId() =
    """
    SELECT ${History.TABLE}.*
    FROM ${History.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
    WHERE ${Chapter.TABLE}.${Chapter.COL_MANGA_ID} = ? AND ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
    """

fun getHistoryByChapterUrl() =
    """
    SELECT ${History.TABLE}.*
    FROM ${History.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
    WHERE ${Chapter.TABLE}.${Chapter.COL_URL} = ? AND ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
    """

fun getLastReadMangaQuery() =
    """
    SELECT ${Manga.TABLE}.*, MAX(${History.TABLE}.${History.COL_LAST_READ}) AS max
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    WHERE ${Manga.TABLE}.${Manga.COL_FAVORITE} = 1
    GROUP BY ${Manga.TABLE}.${Manga.COL_ID}
    ORDER BY max DESC
    """

fun getLastFetchedMangaQuery() =
    """
    SELECT ${Manga.TABLE}.*, MAX(${Chapter.TABLE}.${Chapter.COL_DATE_FETCH}) AS max
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    WHERE ${Manga.TABLE}.${Manga.COL_FAVORITE} = 1
    GROUP BY ${Manga.TABLE}.${Manga.COL_ID}
    ORDER BY max DESC
    """

fun getTotalChapterMangaQuery() =
    """
    SELECT ${Manga.TABLE}.*
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    GROUP BY ${Manga.TABLE}.${Manga.COL_ID}
    ORDER by COUNT(*)
    """

/** Query to get the categories for a manga. */
fun getCategoriesForMangaQuery() =
    """
    SELECT ${Category.TABLE}.* FROM ${Category.TABLE}
    JOIN ${MangaCategory.TABLE} ON ${Category.TABLE}.${Category.COL_ID} =
    ${MangaCategory.TABLE}.${MangaCategory.COL_CATEGORY_ID}
    WHERE ${MangaCategory.COL_MANGA_ID} = ?
    """

/** Query to get manga that are not in library, but have read chapters */
fun getReadMangaNotInLibraryQuery() =
    """
    SELECT ${Manga.TABLE}.*
    FROM ${Manga.TABLE}
    WHERE ${Manga.COL_FAVORITE} = 0 AND ${Manga.COL_ID} IN(
        SELECT ${Chapter.TABLE}.${Chapter.COL_MANGA_ID} FROM ${Chapter.TABLE} WHERE ${Chapter.COL_READ} = 1 OR ${Chapter.COL_LAST_PAGE_READ} != 0
    )
"""
