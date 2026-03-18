package eu.kanade.tachiyomi.data.database.resolvers

import android.annotation.SuppressLint
import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.mappers.BaseMangaGetResolver
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.tables.MangaTable
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.injectLazy

class LibraryMangaGetResolver : DefaultGetResolver<LibraryManga>(), BaseMangaGetResolver {

    private val libraryPreferences: LibraryPreferences by injectLazy()

    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    private var blockedGroups: Set<String>? = null
    private var blockedUploaders: Set<String>? = null
    private var scanlatorFilterOption: Int? = null

    @SuppressLint("Range")
    override fun mapFromCursor(cursor: Cursor): LibraryManga {
        if (blockedGroups == null) {
            blockedGroups = mangaDexPreferences.blockedGroups().get()
            blockedUploaders = mangaDexPreferences.blockedUploaders().get()
            scanlatorFilterOption = libraryPreferences.chapterScanlatorFilterOption().get()
        }

        val manga = LibraryManga()

        mapBaseFromCursor(manga, cursor)

        manga.unread =
            cursor
                .getString(cursor.getColumnIndex(MangaTable.COL_UNREAD))
                .filterChaptersByScanlators(manga)

        manga.category = cursor.getInt(cursor.getColumnIndex(MangaTable.COL_CATEGORY))

        manga.read =
            cursor
                .getString(cursor.getColumnIndex(MangaTable.COL_HAS_READ))
                .filterChaptersByScanlators(manga)

        manga.bookmarkCount = cursor.getInt(cursor.getColumnIndex(MangaTable.COL_BOOKMARK_COUNT))

        manga.unavailableCount =
            cursor.getInt(cursor.getColumnIndex(MangaTable.COL_UNAVAILABLE_COUNT))

        val hasMergedUnread =
            cursor.getString(cursor.getColumnIndex(MangaTable.COL_UNREAD)).filterMerged()

        val hasMergedRead =
            cursor.getString(cursor.getColumnIndex(MangaTable.COL_HAS_READ)).filterMerged()

        manga.isMerged = hasMergedUnread || hasMergedRead

        return manga
    }

    private data class ChapterGroup(val scanlator: String, val uploader: String, val count: Int)

    private fun String.parseChapterGroup(startIndex: Int, endIndex: Int): ChapterGroup {
        val countSeparatorIndex = indexOf(Constants.RAW_CHAPTER_COUNT_SEPARATOR, startIndex)

        val (infoEndIndex, count) =
            if (countSeparatorIndex != -1 && countSeparatorIndex < endIndex) {
                val countStr =
                    substring(
                        countSeparatorIndex + Constants.RAW_CHAPTER_COUNT_SEPARATOR.length,
                        endIndex,
                    )
                countSeparatorIndex to (countStr.toIntOrNull() ?: 1)
            } else {
                endIndex to 1
            }

        val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)

        val (scanlator, uploader) =
            if (typeSeparatorIndex != -1 && typeSeparatorIndex < infoEndIndex) {
                substring(startIndex, typeSeparatorIndex) to
                    substring(
                        typeSeparatorIndex + Constants.RAW_SCANLATOR_TYPE_SEPARATOR.length,
                        infoEndIndex,
                    )
            } else {
                substring(startIndex, infoEndIndex) to ""
            }

        return ChapterGroup(scanlator, uploader, count)
    }

    private fun String.filterMerged(): Boolean {
        var startIndex = 0
        val length = this.length

        while (startIndex < length) {
            var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
            if (endIndex == -1) {
                endIndex = length
            }

            val group = parseChapterGroup(startIndex, endIndex)

            if (MergeType.containsMergeSourceName(group.scanlator)) {
                return true
            }

            startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
        }
        return false
    }

    private fun String.filterChaptersByScanlators(manga: LibraryManga): Int {
        if (isEmpty()) return 0

        // Fast path: No blocked scanlators/uploaders and no manga-specific filter
        if (
            blockedGroups.isNullOrEmpty() &&
                blockedUploaders.isNullOrEmpty() &&
                manga.filtered_scanlators == null
        ) {
            var totalCount = 0
            var startIndex = 0
            val length = this.length

            while (startIndex < length) {
                var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
                if (endIndex == -1) {
                    endIndex = length
                }

                val group = parseChapterGroup(startIndex, endIndex)
                totalCount += group.count

                startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
            }
            return totalCount
        }

        var validChapterCount = 0
        var startIndex = 0
        val length = this.length

        val filtered =
            if (manga.filtered_scanlators != null) {
                ChapterUtil.getScanlators(manga.filtered_scanlators).toSet()
            } else {
                null
            }
        val scanlatorMatchAll = scanlatorFilterOption == 0
        val sources =
            if (filtered != null) SourceManager.mergeSourceNames + MdConstants.name else emptyList()

        while (startIndex < length) {
            var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
            if (endIndex == -1) {
                endIndex = length
            }

            val group = parseChapterGroup(startIndex, endIndex)
            val scanlator = group.scanlator
            val uploader = group.uploader
            val currentGroupCount = group.count

            val scanlators = ChapterUtil.getScanlators(scanlator)

            val isBlocked =
                ChapterUtil.filterByScanlator(
                    scanlators,
                    uploader,
                    false,
                    blockedGroups ?: emptySet(),
                    blockedUploaders ?: emptySet(),
                )

            if (!isBlocked) {
                if (filtered == null) {
                    validChapterCount += currentGroupCount
                } else {
                    // Check if the chapter passes the manga-specific filter
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

                    if (!isFilteredOut) {
                        validChapterCount += currentGroupCount
                    }
                }
            }

            startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
        }

        return validChapterCount
    }
}
