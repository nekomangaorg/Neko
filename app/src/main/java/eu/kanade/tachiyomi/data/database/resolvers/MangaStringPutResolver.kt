package eu.kanade.tachiyomi.data.database.resolvers

import android.content.ContentValues
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.tables.MangaTable

class MangaStringPutResolver(private val column: String, private val getter: (Manga) -> String?) :
    PutResolver<Manga>() {
    override fun performPut(db: StorIOSQLite, manga: Manga) = db.inTransactionReturn {
        val updateQuery =
            UpdateQuery.builder()
                .table(MangaTable.TABLE)
                .where("${MangaTable.COL_ID} = ?")
                .whereArgs(manga.id)
                .build()

        val contentValues = ContentValues(1).apply { put(column, getter(manga)) }

        val numberOfRowsUpdated = db.lowLevel().update(updateQuery, contentValues)
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }
}
