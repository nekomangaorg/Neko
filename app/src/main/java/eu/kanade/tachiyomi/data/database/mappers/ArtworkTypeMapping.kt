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
import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.database.tables.ArtworkTable

class ArtworkTypeMapping : SQLiteTypeMapping<ArtworkImpl>(
    ArtWorkPutResolver(),
    ArtWorkGetResolver(),
    ArtWorkDeleteResolver(),
)

class ArtWorkPutResolver : DefaultPutResolver<ArtworkImpl>() {

    override fun mapToInsertQuery(obj: ArtworkImpl) = InsertQuery.builder()
        .table(ArtworkTable.TABLE)
        .build()

    override fun mapToUpdateQuery(obj: ArtworkImpl) = UpdateQuery.builder()
        .table(ArtworkTable.TABLE)
        .where("${ArtworkTable.COL_ID} = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: ArtworkImpl) = ContentValues(6).apply {
        put(ArtworkTable.COL_ID, obj.id)
        put(ArtworkTable.COL_MANGA_ID, obj.mangaId)
        put(ArtworkTable.COL_FILENAME, obj.fileName)
        put(ArtworkTable.COL_VOLUME, obj.volume)
        put(ArtworkTable.COL_LOCALE, obj.locale)
        put(ArtworkTable.COL_DESCRIPTION, obj.description)
    }
}

class ArtWorkGetResolver : DefaultGetResolver<ArtworkImpl>() {

    override fun mapFromCursor(cursor: Cursor): ArtworkImpl = ArtworkImpl(
        id = cursor.getLong(cursor.getColumnIndex(ArtworkTable.COL_ID)),
        mangaId = cursor.getLong(cursor.getColumnIndex(ArtworkTable.COL_MANGA_ID)),
        fileName = cursor.getString(cursor.getColumnIndex(ArtworkTable.COL_FILENAME)),
        volume = cursor.getString(cursor.getColumnIndex(ArtworkTable.COL_VOLUME)),
        locale = cursor.getString(cursor.getColumnIndex(ArtworkTable.COL_LOCALE)),
        description = cursor.getString(cursor.getColumnIndex(ArtworkTable.COL_DESCRIPTION)),

    )
}

class ArtWorkDeleteResolver : DefaultDeleteResolver<ArtworkImpl>() {

    override fun mapToDeleteQuery(obj: ArtworkImpl) = DeleteQuery.builder()
        .table(ArtworkTable.TABLE)
        .where("${ArtworkTable.COL_ID} = ?")
        .whereArgs(obj.id)
        .build()
}
