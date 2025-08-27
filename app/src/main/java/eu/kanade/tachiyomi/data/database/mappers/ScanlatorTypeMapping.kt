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
import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl
import eu.kanade.tachiyomi.data.database.tables.ScanlatorGroupTable

class ScanlatorTypeMapping :
    SQLiteTypeMapping<ScanlatorGroupImpl>(
        ScanlatorPutResolver(),
        ScanlatorGetResolver(),
        ScanlatorDeleteResolver(),
    )

class ScanlatorPutResolver : DefaultPutResolver<ScanlatorGroupImpl>() {

    override fun mapToInsertQuery(obj: ScanlatorGroupImpl) =
        InsertQuery.builder().table(ScanlatorGroupTable.TABLE).build()

    override fun mapToUpdateQuery(obj: ScanlatorGroupImpl) =
        UpdateQuery.builder()
            .table(ScanlatorGroupTable.TABLE)
            .where("${ScanlatorGroupTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: ScanlatorGroupImpl) =
        ContentValues(4).apply {
            put(ScanlatorGroupTable.COL_ID, obj.id)
            put(ScanlatorGroupTable.COL_NAME, obj.name)
            put(ScanlatorGroupTable.COL_UUID, obj.uuid)
            put(ScanlatorGroupTable.COL_DESCRIPTION, obj.description)
        }
}

class ScanlatorGetResolver : DefaultGetResolver<ScanlatorGroupImpl>() {

    override fun mapFromCursor(cursor: Cursor): ScanlatorGroupImpl =
        ScanlatorGroupImpl(
            id = cursor.getLong(cursor.getColumnIndex(ScanlatorGroupTable.COL_ID)),
            name = cursor.getString(cursor.getColumnIndex(ScanlatorGroupTable.COL_NAME)),
            uuid = cursor.getString(cursor.getColumnIndex(ScanlatorGroupTable.COL_UUID)),
            description =
                cursor.getString(cursor.getColumnIndex(ScanlatorGroupTable.COL_DESCRIPTION)),
        )
}

class ScanlatorDeleteResolver : DefaultDeleteResolver<ScanlatorGroupImpl>() {

    override fun mapToDeleteQuery(obj: ScanlatorGroupImpl) =
        DeleteQuery.builder()
            .table(ScanlatorGroupTable.TABLE)
            .where("${ScanlatorGroupTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()
}
