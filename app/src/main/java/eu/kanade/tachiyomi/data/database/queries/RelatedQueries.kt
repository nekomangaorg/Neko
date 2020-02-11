package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.MangaRelated
import eu.kanade.tachiyomi.data.database.tables.RelatedTable

interface RelatedQueries : DbProvider {

    fun getAllRelated() = db.get()
            .listOfObjects(MangaRelated::class.java)
            .withQuery(Query.builder()
                    .table(RelatedTable.TABLE)
                    .build())
            .prepare()

    fun getRelated(manga_id: Long) = db.get()
            .`object`(MangaRelated::class.java)
            .withQuery(Query.builder()
                    .table(RelatedTable.TABLE)
                    .where("${RelatedTable.COL_MANGA_ID} = ?")
                    .whereArgs(manga_id)
                    .build())
            .prepare()

    fun insertRelated(related: MangaRelated) = db.put().`object`(related).prepare()

    fun insertManyRelated(related: List<MangaRelated>) = db.put().objects(related).prepare()

    fun deleteAllRelated() = db.delete()
            .byQuery(DeleteQuery.builder()
                    .table(RelatedTable.TABLE)
                    .build())
            .prepare()

}