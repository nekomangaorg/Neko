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

    companion object {
        val INSTANCE = LibraryMangaGetResolver()
    }

    @SuppressLint("Range")
    override fun mapFromCursor(cursor: Cursor): LibraryManga {
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

        return manga
    }

    private fun String.filterChaptersByScanlators(manga: LibraryManga): Int {
        if (isEmpty()) return 0
        val list = split(" [.] ")

        val blockedScanlators = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        val chapterList =
            list.filterNot {
                val (scanlator, uploader) = it.split(" [;] ")
                ChapterUtil.filterByScanlator(
                    scanlator,
                    uploader,
                    false,
                    blockedScanlators,
                    blockedUploaders,
                )
            }

        return when (manga.filtered_scanlators == null) {
            true -> chapterList.size
            false -> {
                // Filtered sources, groups and uploaders
                val filtered = ChapterUtil.getScanlators(manga.filtered_scanlators).toSet()
                val scanlatorMatchAll = libraryPreferences.chapterScanlatorFilterOption().get() == 0
                val sources = SourceManager.mergeSourceNames + MdConstants.name
                chapterList
                    .filterNot { scanlators ->
                        sources.any { source ->
                            ChapterUtil.filteredBySource(
                                source,
                                scanlators,
                                MergeType.containsMergeSourceName(scanlators),
                                scanlators == Constants.LOCAL_SOURCE,
                                filtered,
                            )
                        }
                    }
                    .filterNot { pairs ->
                        val (scanlator, uploader) = pairs.split(" [;] ")
                        ChapterUtil.filterByScanlator(
                            scanlator,
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
