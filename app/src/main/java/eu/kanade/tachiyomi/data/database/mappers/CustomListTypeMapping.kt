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
import eu.kanade.tachiyomi.data.database.models.CustomListImpl
import eu.kanade.tachiyomi.data.database.tables.CustomListTable

class CustomListTypeMapping :
    SQLiteTypeMapping<CustomListImpl>(
        CustomListPutResolver(),
        CustomListGetResolver(),
        CustomListDeleteResolver(),
    )

class CustomListPutResolver : DefaultPutResolver<CustomListImpl>() {

    override fun mapToInsertQuery(obj: CustomListImpl) =
        InsertQuery.builder().table(CustomListTable.TABLE).build()

    override fun mapToUpdateQuery(obj: CustomListImpl) =
        UpdateQuery.builder()
            .table(CustomListTable.TABLE)
            .where("${CustomListTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: CustomListImpl) =
        ContentValues(3).apply {
            put(CustomListTable.COL_ID, obj.id)
            put(CustomListTable.COL_NAME, obj.name)
            put(CustomListTable.COL_UUID, obj.uuid)
        }
}

class CustomListGetResolver : DefaultGetResolver<CustomListImpl>() {

    override fun mapFromCursor(cursor: Cursor): CustomListImpl =
        CustomListImpl(
            id = cursor.getLong(cursor.getColumnIndex(CustomListTable.COL_ID)),
            name = cursor.getString(cursor.getColumnIndex(CustomListTable.COL_NAME)),
            uuid = cursor.getString(cursor.getColumnIndex(CustomListTable.COL_UUID)),
        )
}

class CustomListDeleteResolver : DefaultDeleteResolver<CustomListImpl>() {

    override fun mapToDeleteQuery(obj: CustomListImpl) =
        DeleteQuery.builder()
            .table(CustomListTable.TABLE)
            .where("${CustomListTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()
}
