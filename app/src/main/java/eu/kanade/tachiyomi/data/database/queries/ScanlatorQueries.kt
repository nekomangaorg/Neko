package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.ScanlatorImpl
import eu.kanade.tachiyomi.data.database.tables.ScanlatorTable

interface ScanlatorQueries : DbProvider {

    fun insertScanlators(scanlators: List<ScanlatorImpl>) = db.put().objects(scanlators).prepare()

    fun getScanlatorByName(name: String) = db.get()
        .`object`(ScanlatorImpl::class.java)
        .withQuery(
            Query.builder()
                .table(ScanlatorTable.TABLE)
                .where("${ScanlatorTable.COL_NAME} = ?")
                .whereArgs(name)
                .build(),
        )
        .prepare()

    fun deleteScanlator(name: String) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(ScanlatorTable.TABLE)
                .where("${ScanlatorTable.COL_NAME} = ?")
                .whereArgs(name)
                .build(),
        )
        .prepare()
}
