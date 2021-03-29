package eu.kanade.tachiyomi.data.database.mappers

import android.content.ContentValues
import android.database.Cursor
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.models.CachedManga
import eu.kanade.tachiyomi.data.database.tables.CachedMangaTable.COL_MANGA_ID
import eu.kanade.tachiyomi.data.database.tables.CachedMangaTable.COL_MANGA_TITLE
import eu.kanade.tachiyomi.data.database.tables.CachedMangaTable.TABLE_FTS

class CacheMangaTypeMapping : SQLiteTypeMapping<CachedManga>(
    CacheMangaPutResolver(),
    CacheMangaGetResolver(),
    CacheMangaDeleteResolver()
)

class CacheMangaPutResolver : DefaultPutResolver<CachedManga>() {

    override fun mapToInsertQuery(obj: CachedManga) = InsertQuery.builder()
        .table(TABLE_FTS)
        .build()

    override fun mapToUpdateQuery(obj: CachedManga) = UpdateQuery.builder()
        .table(TABLE_FTS)
        .where("$COL_MANGA_ID = ?")
        .whereArgs(obj.mangaId)
        .build()

    override fun mapToContentValues(obj: CachedManga) = ContentValues(2).apply {
        put(COL_MANGA_ID, obj.mangaId)
        put(COL_MANGA_TITLE, obj.title)
    }
}

class CacheMangaGetResolver : DefaultGetResolver<CachedManga>() {

    override fun mapFromCursor(cursor: Cursor): CachedManga = CachedManga(
        mangaId = cursor.getLong(cursor.getColumnIndex(COL_MANGA_ID)),
        title = cursor.getString(cursor.getColumnIndex(COL_MANGA_TITLE))
    )
}

class CacheMangaDeleteResolver : DefaultDeleteResolver<CachedManga>() {

    override fun mapToDeleteQuery(obj: CachedManga) = DeleteQuery.builder()
        .table(TABLE_FTS)
        .where("$COL_MANGA_ID = ?")
        .whereArgs(obj.mangaId)
        .build()
}
