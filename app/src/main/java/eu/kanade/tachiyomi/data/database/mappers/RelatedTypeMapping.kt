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
import eu.kanade.tachiyomi.data.database.models.MangaRelated
import eu.kanade.tachiyomi.data.database.models.MangaRelatedImpl
import eu.kanade.tachiyomi.data.database.tables.RelatedTable.TABLE
import eu.kanade.tachiyomi.data.database.tables.RelatedTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.RelatedTable.COL_MANGA_ID
import eu.kanade.tachiyomi.data.database.tables.RelatedTable.COL_MANGA_RELATED_MATCHED_IDS
import eu.kanade.tachiyomi.data.database.tables.RelatedTable.COL_MANGA_RELATED_MATCHED_TITLES
import eu.kanade.tachiyomi.data.database.tables.RelatedTable.COL_MANGA_RELATED_SCORES



class RelatedTypeMapping : SQLiteTypeMapping<MangaRelated>(
        RelatedPutResolver(),
        RelatedGetResolver(),
        RelatedDeleteResolver()
)

class RelatedPutResolver : DefaultPutResolver<MangaRelated>() {

    override fun mapToInsertQuery(obj: MangaRelated) = InsertQuery.builder()
            .table(TABLE)
            .build()

    override fun mapToUpdateQuery(obj: MangaRelated) = UpdateQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: MangaRelated) = ContentValues(5).apply {
        put(COL_ID, obj.id)
        put(COL_MANGA_ID, obj.manga_id)
        put(COL_MANGA_RELATED_MATCHED_IDS, obj.matched_ids)
        put(COL_MANGA_RELATED_MATCHED_TITLES, obj.matched_titles)
        put(COL_MANGA_RELATED_SCORES, obj.scores)
    }

}

class RelatedGetResolver : DefaultGetResolver<MangaRelated>() {

    override fun mapFromCursor(cursor: Cursor): MangaRelated = MangaRelatedImpl().apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        manga_id = cursor.getLong(cursor.getColumnIndex(COL_MANGA_ID))
        matched_ids = cursor.getString(cursor.getColumnIndex(COL_MANGA_RELATED_MATCHED_IDS))
        matched_titles = cursor.getString(cursor.getColumnIndex(COL_MANGA_RELATED_MATCHED_TITLES))
        scores = cursor.getString(cursor.getColumnIndex(COL_MANGA_RELATED_SCORES))
    }

}

class RelatedDeleteResolver : DefaultDeleteResolver<MangaRelated>() {

    override fun mapToDeleteQuery(obj: MangaRelated) = DeleteQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()

}
