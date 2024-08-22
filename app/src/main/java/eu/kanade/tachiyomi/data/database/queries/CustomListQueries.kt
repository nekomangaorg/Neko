package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.CustomListImpl
import eu.kanade.tachiyomi.data.database.tables.CustomListTable

interface CustomListQueries : DbProvider {

    fun insertCustomsLists(customLists: List<CustomListImpl>) =
        db.put().objects(customLists).prepare()

    fun getCustomListByUUID(uuid: String) =
        db.get()
            .`object`(CustomListImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(CustomListTable.TABLE)
                    .where("${CustomListTable.COL_UUID} = ?")
                    .whereArgs(uuid)
                    .build(),
            )
            .prepare()

    fun getCustomLists() =
        db.get()
            .listOfObjects(CustomListImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(CustomListTable.TABLE)
                    .orderBy(CustomListTable.COL_NAME)
                    .build(),
            )
            .prepare()

    fun deleteCustomListByUUID(uuid: String) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(CustomListTable.TABLE)
                    .where("${CustomListTable.COL_UUID} = ?")
                    .whereArgs(uuid)
                    .build(),
            )
            .prepare()
}
