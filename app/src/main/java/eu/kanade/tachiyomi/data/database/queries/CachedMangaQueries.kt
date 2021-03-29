package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.inTransaction
import eu.kanade.tachiyomi.data.database.models.CachedManga
import eu.kanade.tachiyomi.data.database.tables.CachedMangaTable

interface CachedMangaQueries : DbProvider {

    fun insertCachedManga(cachedManga: List<CachedManga>) = db.put().objects(cachedManga).prepare()

    fun insertCachedManga2(cachedManga: List<CachedManga>) = db.inTransaction {
        val query = RawQuery.builder()
            .query("INSERT INTO ${CachedMangaTable.TABLE_FTS} (${CachedMangaTable.COL_MANGA_ID}, ${CachedMangaTable.COL_MANGA_TITLE}) VALUES (?, ?);")

        cachedManga.forEach {
            db.lowLevel().executeSQL(
                query.args(it.mangaId, it.title)
                    .build()
            )
        }
    }

    fun getCachedMangaCount() = db.get().numberOfResults().withQuery(
        RawQuery.builder()
            .query("SELECT * FROM ${CachedMangaTable.TABLE_FTS}")
            .build()
    ).prepare()

    fun searchCachedManga(query: String) = db.get()
        .listOfObjects(CachedManga::class.java)
        .withQuery(
            RawQuery.builder()
                .query(searchCachedMangaQuery(query))
                .build()
        )
        .prepare()

    fun deleteAllCached() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(CachedMangaTable.TABLE_FTS)
                .build()
        )
        .prepare()

    fun deleteAllCachedFTS() = db.lowLevel()
        .rawQuery(
            RawQuery.builder()
                .query(deleteCachedMangaFTSQuery())
                .build()
        )

    /*   fun copyCachedToFTS() = db.lowLevel().rawQuery(
           RawQuery.builder()
               .query(insertCachedMangaQuery())
               .build()
       )*/
}
