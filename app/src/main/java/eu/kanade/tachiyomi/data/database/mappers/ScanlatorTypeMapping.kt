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
import eu.kanade.tachiyomi.data.database.models.ScanlatorImpl
import eu.kanade.tachiyomi.data.database.tables.ScanlatorTable

class ScanlatorTypeMapping : SQLiteTypeMapping<ScanlatorImpl>(
    ScanlatorPutResolver(),
    ScanlatorGetResolver(),
    ScanlatorDeleteResolver(),
)

class ScanlatorPutResolver : DefaultPutResolver<ScanlatorImpl>() {

    override fun mapToInsertQuery(obj: ScanlatorImpl) = InsertQuery.builder()
        .table(ScanlatorTable.TABLE)
        .build()

    override fun mapToUpdateQuery(obj: ScanlatorImpl) = UpdateQuery.builder()
        .table(ScanlatorTable.TABLE)
        .where("${ScanlatorTable.COL_ID} = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: ScanlatorImpl) = ContentValues(4).apply {
        put(ScanlatorTable.COL_ID, obj.id)
        put(ScanlatorTable.COL_NAME, obj.name)
        put(ScanlatorTable.COL_UUID, obj.uuid)
        put(ScanlatorTable.COL_DESCRIPTION, obj.description)
    }
}

class ScanlatorGetResolver : DefaultGetResolver<ScanlatorImpl>() {

    override fun mapFromCursor(cursor: Cursor): ScanlatorImpl = ScanlatorImpl(
        id = cursor.getLong(cursor.getColumnIndex(ScanlatorTable.COL_ID)),
        name = cursor.getString(cursor.getColumnIndex(ScanlatorTable.COL_NAME)),
        uuid = cursor.getString(cursor.getColumnIndex(ScanlatorTable.COL_UUID)),
        description = cursor.getString(cursor.getColumnIndex(ScanlatorTable.COL_DESCRIPTION)),
    )
}

class ScanlatorDeleteResolver : DefaultDeleteResolver<ScanlatorImpl>() {

    override fun mapToDeleteQuery(obj: ScanlatorImpl) = DeleteQuery.builder()
        .table(ScanlatorTable.TABLE)
        .where("${ScanlatorTable.COL_ID} = ?")
        .whereArgs(obj.id)
        .build()
}
