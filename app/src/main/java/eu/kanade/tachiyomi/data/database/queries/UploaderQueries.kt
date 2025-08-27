package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import eu.kanade.tachiyomi.data.database.tables.UploaderTable

interface UploaderQueries : DbProvider {

    fun insertUploader(uploaders: List<UploaderImpl>) = db.put().objects(uploaders).prepare()

    fun getUploaderByName(name: String) =
        db.get()
            .`object`(UploaderImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(UploaderTable.TABLE)
                    .where("${UploaderTable.COL_USERNAME} = ?")
                    .whereArgs(name)
                    .build()
            )
            .prepare()

    fun deleteUploader(name: String) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(UploaderTable.TABLE)
                    .where("${UploaderTable.COL_USERNAME} = ?")
                    .whereArgs(name)
                    .build()
            )
            .prepare()
}
