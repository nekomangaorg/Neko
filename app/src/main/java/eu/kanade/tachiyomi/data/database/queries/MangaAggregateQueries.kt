package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import eu.kanade.tachiyomi.data.database.tables.MangaAggregateTable

interface MangaAggregateQueries : DbProvider {

    fun getMangaAggregate(mangaId: Long) =
        db.get()
            .`object`(MangaAggregate::class.java)
            .withQuery(
                Query.builder()
                    .table(MangaAggregateTable.TABLE)
                    .where("${MangaAggregateTable.COL_MANGA_ID} = ?")
                    .whereArgs(mangaId)
                    .build()
            )
            .prepare()

    fun insertMangaAggregate(aggregate: MangaAggregate) = db.put().`object`(aggregate).prepare()

    fun deleteMangaAggregate(mangaId: Long) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(MangaAggregateTable.TABLE)
                    .where("${MangaAggregateTable.COL_MANGA_ID} = ?")
                    .whereArgs(mangaId)
                    .build()
            )
            .prepare()
}
