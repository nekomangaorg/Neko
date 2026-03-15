package eu.kanade.tachiyomi.data.database.resolvers

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.compose.ui.util.fastAny
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
        val list = split(Constants.RAW_CHAPTER_SEPARATOR)
        return list.fastAny { scanlator -> MergeType.containsMergeSourceName(scanlator) }
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

        val list = split(Constants.RAW_CHAPTER_SEPARATOR)

        val chapterList =
            list.filterNot {
                val (scanlator, uploader) = it.split(Constants.RAW_SCANLATOR_TYPE_SEPARATOR)
                val scanlators = ChapterUtil.getScanlators(scanlator)

                ChapterUtil.filterByScanlator(
                    scanlators,
                    uploader,
                    false,
                    blockedGroups ?: emptySet(),
                    blockedUploaders ?: emptySet(),
                )
            }

        return when (manga.filtered_scanlators == null) {
            true -> chapterList.size
            false -> {
                // Filtered sources, groups and uploaders
                val filtered = ChapterUtil.getScanlators(manga.filtered_scanlators).toSet()
                val scanlatorMatchAll = scanlatorFilterOption == 0
                val sources = SourceManager.mergeSourceNames + MdConstants.name
                chapterList
                    .filterNot { scanlators ->
                        sources.any { source ->
                            var (scanlators, _) =
                                scanlators.split(Constants.RAW_SCANLATOR_TYPE_SEPARATOR)
                            scanlators =
                                scanlators.replace(
                                    Constants.RAW_CHAPTER_SEPARATOR,
                                    Constants.SCANLATOR_SEPARATOR,
                                )

                            val scanlatorList = ChapterUtil.getScanlators(scanlators)

                            ChapterUtil.filteredBySource(
                                source,
                                scanlatorList,
                                MergeType.containsMergeSourceName(scanlators),
                                scanlators == Constants.LOCAL_SOURCE,
                                filtered,
                            )
                        }
                    }
                    .filterNot { pairs ->
                        val (scanlator, uploader) =
                            pairs.split(Constants.RAW_SCANLATOR_TYPE_SEPARATOR)

                        val scanlatorList = ChapterUtil.getScanlators(scanlator)

                        ChapterUtil.filterByScanlator(
                            scanlatorList,
                            uploader,
                            scanlatorMatchAll,
                            filtered,
                        )
                    }
                    .size
            }
        }
    }
}
