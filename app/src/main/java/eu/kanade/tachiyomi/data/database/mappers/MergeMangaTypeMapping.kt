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
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.tables.MergeMangaTable

class MergeMangaTypeMapping :
    SQLiteTypeMapping<MergeMangaImpl>(
        MergeMangaPutResolver(),
        MergeMangaGetResolver(),
        MergeMangaDeleteResolver(),
    )

class MergeMangaPutResolver : DefaultPutResolver<MergeMangaImpl>() {

    override fun mapToInsertQuery(obj: MergeMangaImpl) =
        InsertQuery.builder().table(MergeMangaTable.TABLE).build()

    override fun mapToUpdateQuery(obj: MergeMangaImpl) =
        UpdateQuery.builder()
            .table(MergeMangaTable.TABLE)
            .where("${MergeMangaTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: MergeMangaImpl) =
        ContentValues(6).apply {
            put(MergeMangaTable.COL_ID, obj.id)
            put(MergeMangaTable.COL_MANGA_ID, obj.mangaId)
            put(MergeMangaTable.COL_COVER_URL, obj.coverUrl)
            put(MergeMangaTable.COL_TITLE, obj.title)
            put(MergeMangaTable.COL_URL, obj.url)
            put(MergeMangaTable.COL_MERGE_TYPE, obj.mergeType.id)
        }
}

class MergeMangaGetResolver : DefaultGetResolver<MergeMangaImpl>() {

    override fun mapFromCursor(cursor: Cursor): MergeMangaImpl =
        MergeMangaImpl(
            id = cursor.getLong(cursor.getColumnIndex(MergeMangaTable.COL_ID)),
            mangaId = cursor.getLong(cursor.getColumnIndex(MergeMangaTable.COL_MANGA_ID)),
            coverUrl = cursor.getString(cursor.getColumnIndex(MergeMangaTable.COL_COVER_URL)),
            title = cursor.getString(cursor.getColumnIndex(MergeMangaTable.COL_TITLE)),
            url = cursor.getString(cursor.getColumnIndex(MergeMangaTable.COL_URL)),
            mergeType =
                MergeType.getById(
                    cursor.getInt(cursor.getColumnIndex(MergeMangaTable.COL_MERGE_TYPE))),
        )
}

class MergeMangaDeleteResolver : DefaultDeleteResolver<MergeMangaImpl>() {

    override fun mapToDeleteQuery(obj: MergeMangaImpl) =
        DeleteQuery.builder()
            .table(MergeMangaTable.TABLE)
            .where("${MergeMangaTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()
}
