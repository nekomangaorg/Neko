package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.Queries
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.inTransaction
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable

interface MangaCategoryQueries : DbProvider {

    fun insertMangaCategory(mangaCategory: MangaCategory) =
        db.put().`object`(mangaCategory).prepare()

    fun insertMangaListCategories(mangaListCategories: List<MangaCategory>) =
        db.put().objects(mangaListCategories).prepare()

    fun deleteOldMangaListCategories(mangaList: List<Manga>) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(MangaCategoryTable.TABLE)
                .where("${MangaCategoryTable.COL_MANGA_ID} IN (${Queries.placeholders(mangaList.size)})")
                .whereArgs(*mangaList.map { it.id }.toTypedArray())
                .build()
        )
        .prepare()

    fun setMangaCategories(mangaListCategories: List<MangaCategory>, mangaList: List<Manga>) {
        db.inTransaction {
            deleteOldMangaListCategories(mangaList).executeAsBlocking()
            insertMangaListCategories(mangaListCategories).executeAsBlocking()
        }
    }
}
