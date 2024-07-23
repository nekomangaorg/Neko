package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.tables.MergeMangaTable

interface MergeMangaQueries : DbProvider {

    fun insertMergeManga(manga: MergeMangaImpl) = db.put().`object`(manga).prepare()

    fun getMergeMangaList(manga: Manga) = getMergeMangaList(manga.id!!)

    fun getMergeMangaList(mangaId: Long) =
        db.get()
            .listOfObjects(MergeMangaImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(MergeMangaTable.TABLE)
                    .where("${MergeMangaTable.COL_MANGA_ID} = ?")
                    .whereArgs(mangaId)
                    .build(),
            )
            .prepare()

    fun getAllMergeManga() =
        db.get()
            .listOfObjects(MergeMangaImpl::class.java)
            .withQuery(
                Query.builder().table(MergeMangaTable.TABLE).build(),
            )
            .prepare()

    fun deleteMergeMangaForType(mangaId: Long, mergeType: MergeType) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(MergeMangaTable.TABLE)
                    .where(
                        "${MergeMangaTable.COL_MANGA_ID} = ? AND ${MergeMangaTable.COL_MERGE_TYPE} = ?")
                    .whereArgs(mangaId, mergeType.id)
                    .build(),
            )
            .prepare()
}
