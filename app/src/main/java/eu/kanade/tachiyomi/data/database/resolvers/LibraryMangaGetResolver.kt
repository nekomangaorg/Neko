package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.mappers.BaseMangaGetResolver
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.tables.MangaTable
import eu.kanade.tachiyomi.util.chapter.ChapterUtil

class LibraryMangaGetResolver : DefaultGetResolver<LibraryManga>(), BaseMangaGetResolver {

    companion object {
        val INSTANCE = LibraryMangaGetResolver()
    }

    override fun mapFromCursor(cursor: Cursor): LibraryManga {
        val manga = LibraryManga()

        mapBaseFromCursor(manga, cursor)
        manga.unread = cursor.getString(cursor.getColumnIndex(MangaTable.COL_UNREAD))
            .filterChaptersByScanlators(manga)
        manga.category = cursor.getInt(cursor.getColumnIndex(MangaTable.COL_CATEGORY))
        manga.read = cursor.getString(cursor.getColumnIndex(MangaTable.COL_HAS_READ))
            .filterChaptersByScanlators(manga)
        manga.bookmarkCount = cursor.getInt(cursor.getColumnIndex(MangaTable.COL_BOOKMARK_COUNT))

        return manga
    }

    private fun String.filterChaptersByScanlators(manga: LibraryManga): Int {
        if (isEmpty()) return 0
        val list = split(" [.] ")
        return manga.filtered_scanlators?.let { filteredScanlatorString ->
            val filteredScanlators = ChapterUtil.getScanlators(filteredScanlatorString)
            list.count { ChapterUtil.getScanlators(it).none { group -> filteredScanlators.contains(group) } }
        } ?: list.size
    }
}
