package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.mappers.BaseMangaGetResolver
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.tables.MangaTable
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import uy.kohesive.injekt.injectLazy

class LibraryMangaGetResolver : DefaultGetResolver<LibraryManga>(), BaseMangaGetResolver {

    private val preferenceHelper: PreferencesHelper by injectLazy()

    companion object {
        val INSTANCE = LibraryMangaGetResolver()
    }

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

        val blockedScanlators = preferenceHelper.blockedScanlators().get()

        val chapterScanlatorList =
            list.filterNot { scanlators ->
                ChapterUtil.filterByGroup(scanlators, false, blockedScanlators)
            }

        return when (manga.filtered_scanlators == null) {
            true -> chapterScanlatorList.size
            false -> {
                val filteredScanlators =
                    ChapterUtil.getScanlators(manga.filtered_scanlators).toSet()
                val chapterScanlatorMatchAll =
                    preferenceHelper.chapterScanlatorFilterOption().get() == 0
                chapterScanlatorList
                    .filterNot { scanlators ->
                        ChapterUtil.filterByGroup(
                            scanlators,
                            chapterScanlatorMatchAll,
                            filteredScanlators,
                        )
                    }
                    .size
            }
        }
    }
}
