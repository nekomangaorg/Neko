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
import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import eu.kanade.tachiyomi.data.database.tables.MangaAggregateTable.COL_MANGA_ID
import eu.kanade.tachiyomi.data.database.tables.MangaAggregateTable.COL_VOLUMES
import eu.kanade.tachiyomi.data.database.tables.MangaAggregateTable.TABLE

class MangaAggregateTypeMapping :
    SQLiteTypeMapping<MangaAggregate>(
        MangaAggregatePutResolver(),
        MangaAggregateGetResolver(),
        MangaAggregateDeleteResolver(),
    )

class MangaAggregatePutResolver : DefaultPutResolver<MangaAggregate>() {

    override fun mapToInsertQuery(obj: MangaAggregate) = InsertQuery.builder().table(TABLE).build()

    override fun mapToUpdateQuery(obj: MangaAggregate) =
        UpdateQuery.builder().table(TABLE).where("$COL_MANGA_ID = ?").whereArgs(obj.mangaId).build()

    override fun mapToContentValues(obj: MangaAggregate) =
        ContentValues(2).apply {
            put(COL_MANGA_ID, obj.mangaId)
            put(COL_VOLUMES, obj.volumes)
        }
}

class MangaAggregateGetResolver : DefaultGetResolver<MangaAggregate>() {

    override fun mapFromCursor(cursor: Cursor): MangaAggregate =
        MangaAggregate(
            mangaId = cursor.getLong(cursor.getColumnIndex(COL_MANGA_ID)),
            volumes = cursor.getString(cursor.getColumnIndex(COL_VOLUMES)),
        )
}

class MangaAggregateDeleteResolver : DefaultDeleteResolver<MangaAggregate>() {

    override fun mapToDeleteQuery(obj: MangaAggregate) =
        DeleteQuery.builder().table(TABLE).where("$COL_MANGA_ID = ?").whereArgs(obj.mangaId).build()
}
