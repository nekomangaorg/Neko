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
import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import eu.kanade.tachiyomi.data.database.tables.UploaderTable

class UploaderTypeMapping :
    SQLiteTypeMapping<UploaderImpl>(
        UploaderPutResolver(),
        UploaderGetResolver(),
        UploaderDeleteResolver(),
    )

class UploaderPutResolver : DefaultPutResolver<UploaderImpl>() {

    override fun mapToInsertQuery(obj: UploaderImpl) =
        InsertQuery.builder().table(UploaderTable.TABLE).build()

    override fun mapToUpdateQuery(obj: UploaderImpl) =
        UpdateQuery.builder()
            .table(UploaderTable.TABLE)
            .where("${UploaderTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: UploaderImpl) =
        ContentValues(4).apply {
            put(UploaderTable.COL_ID, obj.id)
            put(UploaderTable.COL_USERNAME, obj.username)
            put(UploaderTable.COL_UUID, obj.uuid)
        }
}

class UploaderGetResolver : DefaultGetResolver<UploaderImpl>() {

    override fun mapFromCursor(cursor: Cursor): UploaderImpl =
        UploaderImpl(
            id = cursor.getLong(cursor.getColumnIndex(UploaderTable.COL_ID)),
            username = cursor.getString(cursor.getColumnIndex(UploaderTable.COL_USERNAME)),
            uuid = cursor.getString(cursor.getColumnIndex(UploaderTable.COL_UUID)),
        )
}

class UploaderDeleteResolver : DefaultDeleteResolver<UploaderImpl>() {

    override fun mapToDeleteQuery(obj: UploaderImpl) =
        DeleteQuery.builder()
            .table(UploaderTable.TABLE)
            .where("${UploaderTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()
}
