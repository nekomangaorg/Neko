package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.database.tables.SimilarTable

interface SimilarQueries : DbProvider {

    fun getSimilar(manga_id: String) = db.get()
        .`object`(MangaSimilar::class.java)
        .withQuery(
            Query.builder()
                .table(SimilarTable.TABLE)
                .where("${SimilarTable.COL_MANGA_ID} = ?")
                .whereArgs(manga_id)
                .build()
        )
        .prepare()

    fun insertSimilar(similar: MangaSimilar) = db.put().`object`(similar).prepare()

    fun deleteAllSimilar() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(SimilarTable.TABLE)
                .build()
        )
        .prepare()
}
