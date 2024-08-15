package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.data.database.tables.BrowseFilterTable

interface BrowseFilterQueries : DbProvider {

    fun insertBrowseFilter(browseFilterImpl: BrowseFilterImpl) =
        db.put().`object`(browseFilterImpl).prepare()

    fun insertBrowseFilters(browseFilters: List<BrowseFilterImpl>) =
        db.put().objects(browseFilters).prepare()

    fun deleteBrowseFilter(name: String) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(BrowseFilterTable.TABLE)
                    .where("${BrowseFilterTable.COL_NAME} = ?")
                    .whereArgs(name)
                    .build(),
            )
            .prepare()

    fun deleteAllBrowseFilters() =
        db.delete()
            .byQuery(
                DeleteQuery.builder().table(BrowseFilterTable.TABLE).build(),
            )
            .prepare()

    fun getBrowseFilters() =
        db.get()
            .listOfObjects(BrowseFilterImpl::class.java)
            .withQuery(
                Query.builder().table(BrowseFilterTable.TABLE).build(),
            )
            .prepare()

    fun getDefault() =
        db.get()
            .listOfObjects(BrowseFilterImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(BrowseFilterTable.TABLE)
                    .where("${BrowseFilterTable.COL_DEFAULT} = ?")
                    .whereArgs(true)
                    .build(),
            )
            .prepare()
}
