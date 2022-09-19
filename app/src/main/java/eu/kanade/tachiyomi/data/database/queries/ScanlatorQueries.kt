package eu.kanade.tachiyomi.data.database.queries

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

    fun getScanlatorByUUID(uuid: String) = db.get()
        .`object`(ScanlatorImpl::class.java)
        .withQuery(
            Query.builder()
                .table(ScanlatorTable.TABLE)
                .where("${ScanlatorTable.COL_UUID} = ?")
                .whereArgs(uuid)
                .build(),
        )
        .prepare()
}
