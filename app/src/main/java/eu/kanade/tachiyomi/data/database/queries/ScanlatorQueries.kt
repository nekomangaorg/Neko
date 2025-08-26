package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl
import eu.kanade.tachiyomi.data.database.tables.ScanlatorGroupTable

interface ScanlatorGroupQueries : DbProvider {

    fun insertScanlatorGroups(groups: List<ScanlatorGroupImpl>) = db.put().objects(groups).prepare()

    fun getScanlatorGroupByName(name: String) =
        db.get()
            .`object`(ScanlatorGroupImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(ScanlatorGroupTable.TABLE)
                    .where("${ScanlatorGroupTable.COL_NAME} = ?")
                    .whereArgs(name)
                    .build()
            )
            .prepare()

    fun deleteScanlatorGroup(name: String) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(ScanlatorGroupTable.TABLE)
                    .where("${ScanlatorGroupTable.COL_NAME} = ?")
                    .whereArgs(name)
                    .build()
            )
            .prepare()
}
