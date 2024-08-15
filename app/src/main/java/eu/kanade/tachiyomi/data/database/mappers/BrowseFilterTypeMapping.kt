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
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.data.database.tables.BrowseFilterTable

class BrowseFilterTypeMapping :
    SQLiteTypeMapping<BrowseFilterImpl>(
        BrowseFilterPutResolver(),
        BrowseFilterGetResolver(),
        BrowseFilterDeleteResolver(),
    )

class BrowseFilterPutResolver : DefaultPutResolver<BrowseFilterImpl>() {

    override fun mapToInsertQuery(obj: BrowseFilterImpl) =
        InsertQuery.builder().table(BrowseFilterTable.TABLE).build()

    override fun mapToUpdateQuery(obj: BrowseFilterImpl) =
        UpdateQuery.builder()
            .table(BrowseFilterTable.TABLE)
            .where("${BrowseFilterTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: BrowseFilterImpl) =
        ContentValues(4).apply {
            put(BrowseFilterTable.COL_ID, obj.id)
            put(BrowseFilterTable.COL_NAME, obj.name)
            put(BrowseFilterTable.COL_FILTERS, obj.dexFilters)
            put(BrowseFilterTable.COL_DEFAULT, obj.default)
        }
}

class BrowseFilterGetResolver : DefaultGetResolver<BrowseFilterImpl>() {

    override fun mapFromCursor(cursor: Cursor): BrowseFilterImpl =
        BrowseFilterImpl(
            id = cursor.getLong(cursor.getColumnIndex(BrowseFilterTable.COL_ID)),
            name = cursor.getString(cursor.getColumnIndex(BrowseFilterTable.COL_NAME)),
            dexFilters = cursor.getString(cursor.getColumnIndex(BrowseFilterTable.COL_FILTERS)),
            default = cursor.getInt(cursor.getColumnIndex(BrowseFilterTable.COL_DEFAULT)) == 1,
        )
}

class BrowseFilterDeleteResolver : DefaultDeleteResolver<BrowseFilterImpl>() {

    override fun mapToDeleteQuery(obj: BrowseFilterImpl) =
        DeleteQuery.builder()
            .table(BrowseFilterTable.TABLE)
            .where("${BrowseFilterTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()
}
