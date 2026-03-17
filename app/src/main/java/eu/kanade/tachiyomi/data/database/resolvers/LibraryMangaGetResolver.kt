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

    private fun String.filterMerged(): Boolean {
        var startIndex = 0
        val length = this.length

        while (startIndex < length) {
            var endIndex = indexOf(Constants.RAW_CHAPTER_SEPARATOR, startIndex)
            if (endIndex == -1) {
                endIndex = length
            }

            val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)

            val scanlator =
                if (typeSeparatorIndex != -1 && typeSeparatorIndex < endIndex) {
                    substring(startIndex, typeSeparatorIndex)
                } else {
                    substring(startIndex, endIndex)
                }

            if (MergeType.containsMergeSourceName(scanlator)) {
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
            var count = 1
            var index = indexOf(Constants.RAW_CHAPTER_SEPARATOR)
            while (index != -1) {
                count++
                index =
                    indexOf(
                        Constants.RAW_CHAPTER_SEPARATOR,
                        index + Constants.RAW_CHAPTER_SEPARATOR.length,
                    )
            }
            return count
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

            val typeSeparatorIndex = indexOf(Constants.RAW_SCANLATOR_TYPE_SEPARATOR, startIndex)

            val scanlator: String
            val uploader: String
            if (typeSeparatorIndex != -1 && typeSeparatorIndex < endIndex) {
                scanlator = substring(startIndex, typeSeparatorIndex)
                uploader =
                    substring(
                        typeSeparatorIndex + Constants.RAW_SCANLATOR_TYPE_SEPARATOR.length,
                        endIndex,
                    )
            } else {
                scanlator = substring(startIndex, endIndex)
                uploader = ""
            }

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
                    validChapterCount++
                } else {
                    // Check if the chapter passes the manga-specific filter
                    var isFilteredOut = false

                    // First check the sources
                    val replacedScanlator =
                        scanlator.replace(
                            Constants.RAW_CHAPTER_SEPARATOR,
                            Constants.SCANLATOR_SEPARATOR,
                        )
                    val replacedScanlatorList = ChapterUtil.getScanlators(replacedScanlator)
                    for (source in sources) {
                        if (
                            ChapterUtil.filteredBySource(
                                source,
                                replacedScanlatorList,
                                MergeType.containsMergeSourceName(replacedScanlator),
                                replacedScanlator == Constants.LOCAL_SOURCE,
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
                        validChapterCount++
                    }
                }
            }

            startIndex = endIndex + Constants.RAW_CHAPTER_SEPARATOR.length
        }

        return validChapterCount
    }
}
