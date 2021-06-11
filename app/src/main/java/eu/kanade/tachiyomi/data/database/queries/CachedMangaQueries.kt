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
            .query(
                "INSERT INTO ${CachedMangaTable.TABLE_FTS} " +
                    "(${CachedMangaTable.COL_MANGA_TITLE}, ${CachedMangaTable.COL_MANGA_UUID}," +
                    " ${CachedMangaTable.COL_MANGA_RATING}) VALUES (?, ?, ?);"
            )

        cachedManga.forEach {
            db.lowLevel().executeSQL(
                query.args(it.title, it.uuid, it.rating)
                    .build()
            )
        }
    }

    fun insertCachedManga2Single(cachedManga: CachedManga) = db.inTransaction {
        val query = RawQuery.builder()
            .query(
                "INSERT INTO ${CachedMangaTable.TABLE_FTS} " +
                    "(${CachedMangaTable.COL_MANGA_TITLE}, ${CachedMangaTable.COL_MANGA_UUID}," +
                    " ${CachedMangaTable.COL_MANGA_RATING}) VALUES (?, ?, ?);"
            )
        db.lowLevel().executeSQL(
            query.args(cachedManga.title, cachedManga.uuid, cachedManga.rating)
                .build()
        )
    }

    fun getCachedMangaCount() = db.get().numberOfResults().withQuery(
        RawQuery.builder()
            .query("SELECT * FROM ${CachedMangaTable.TABLE_FTS}")
            .build()
    ).prepare()

    fun getCachedMangaRange(page: Int, limit: Int = 20) = db.get()
        .listOfObjects(CachedManga::class.java)
        .withQuery(
            RawQuery.builder()
                .query("SELECT * FROM ${CachedMangaTable.TABLE_FTS} LIMIT ${limit + 1} OFFSET ${page * limit}")
                .build()
        )
        .prepare()

    fun searchCachedManga(query: String, page: Int, limit: Int = 20) = db.get()
        .listOfObjects(CachedManga::class.java)
        .withQuery(
            RawQuery.builder()
                .query(searchCachedMangaQuery(query, page, limit))
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

    fun optimizeCachedManga() = db.inTransaction {
        val query = RawQuery.builder()
            .query("INSERT INTO ${CachedMangaTable.TABLE_FTS}(${CachedMangaTable.TABLE_FTS}) VALUES('optimize');")
        db.lowLevel().executeSQL(
            query.build()
        )
    }
}
