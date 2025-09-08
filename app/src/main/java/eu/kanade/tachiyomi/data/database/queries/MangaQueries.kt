package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.resolvers.LibraryMangaGetResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaDateAddedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaFavoritePutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaFlagsPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaLastUpdatedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaNextUpdatedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaScanlatorFilterFlagsPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaTitlePutResolver
import eu.kanade.tachiyomi.data.database.tables.CategoryTable
import eu.kanade.tachiyomi.data.database.tables.ChapterTable
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable
import eu.kanade.tachiyomi.data.database.tables.MangaTable

interface MangaQueries : DbProvider {

    fun getMangaList() =
        db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(Query.builder().table(MangaTable.TABLE).build())
            .prepare()

    fun getLibraryMangaList() =
        db.get()
            .listOfObjects(LibraryManga::class.java)
            .withQuery(
                RawQuery.builder()
                    .query(libraryQuery)
                    .observesTables(
                        MangaTable.TABLE,
                        ChapterTable.TABLE,
                        MangaCategoryTable.TABLE,
                        CategoryTable.TABLE,
                    )
                    .build()
            )
            .withGetResolver(LibraryMangaGetResolver.INSTANCE)
            .prepare()

    fun getFavoriteMangaList() =
        db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(
                Query.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_FAVORITE} = ?")
                    .whereArgs(1)
                    .orderBy(MangaTable.COL_TITLE)
                    .build()
            )
            .prepare()

    fun getManga(url: String, sourceId: Long) =
        db.get()
            .`object`(Manga::class.java)
            .withQuery(
                Query.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_URL} = ? AND ${MangaTable.COL_SOURCE} = ?")
                    .whereArgs(url, sourceId)
                    .build()
            )
            .prepare()

    fun getMangadexManga(url: String) =
        db.get()
            .`object`(Manga::class.java)
            .withQuery(
                Query.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_URL} = ?")
                    .whereArgs(url)
                    .build()
            )
            .prepare()

    fun getManga(id: Long) =
        db.get()
            .`object`(Manga::class.java)
            .withQuery(
                Query.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_ID} = ?")
                    .whereArgs(id)
                    .build()
            )
            .prepare()

    fun insertManga(manga: Manga) = db.put().`object`(manga).prepare()

    fun insertMangaList(mangaList: List<Manga>) = db.put().objects(mangaList).prepare()

    fun updateChapterFlags(manga: Manga) =
        db.put()
            .`object`(manga)
            .withPutResolver(
                MangaFlagsPutResolver(MangaTable.COL_CHAPTER_FLAGS, Manga::chapter_flags)
            )
            .prepare()

    fun updateChapterFlags(manga: List<Manga>) =
        db.put()
            .objects(manga)
            .withPutResolver(
                MangaFlagsPutResolver(MangaTable.COL_CHAPTER_FLAGS, Manga::chapter_flags, true)
            )
            .prepare()

    fun updateViewerFlags(manga: Manga) =
        db.put()
            .`object`(manga)
            .withPutResolver(MangaFlagsPutResolver(MangaTable.COL_VIEWER, Manga::viewer_flags))
            .prepare()

    fun updateViewerFlags(manga: List<Manga>) =
        db.put()
            .objects(manga)
            .withPutResolver(
                MangaFlagsPutResolver(MangaTable.COL_VIEWER, Manga::viewer_flags, true)
            )
            .prepare()

    fun updateScanlatorFilterFlag(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaScanlatorFilterFlagsPutResolver()).prepare()

    fun updateNextUpdated(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaNextUpdatedPutResolver()).prepare()

    fun updateLastUpdated(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaLastUpdatedPutResolver()).prepare()

    fun updateMangaFavorite(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaFavoritePutResolver()).prepare()

    fun updateMangaAdded(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaDateAddedPutResolver()).prepare()

    fun updateMangaTitle(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaTitlePutResolver()).prepare()

    fun updateMangaInfo(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaTitlePutResolver()).prepare()

    fun deleteManga(manga: Manga) = db.delete().`object`(manga).prepare()

    fun deleteManga(mangaList: List<Manga>) = db.delete().objects(mangaList).prepare()

    fun deleteAllMangaNotInLibrary() =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(MangaTable.TABLE)
                    .where("${MangaTable.COL_FAVORITE} = ?")
                    .whereArgs(0)
                    .build()
            )
            .prepare()

    fun deleteAllMangaNotInLibraryAndNotRead() =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(MangaTable.TABLE)
                    .where(
                        """
                    ${MangaTable.COL_FAVORITE} = ? AND ${MangaTable.COL_ID} NOT IN (
                        SELECT ${ChapterTable.COL_MANGA_ID} FROM ${ChapterTable.TABLE} WHERE ${ChapterTable.COL_READ} = 1 OR ${ChapterTable.COL_LAST_PAGE_READ} != 0
                    )
                    """
                            .trimIndent()
                    )
                    .whereArgs(0)
                    .build()
            )
            .prepare()

    fun deleteAllManga() =
        db.delete().byQuery(DeleteQuery.builder().table(MangaTable.TABLE).build()).prepare()

    fun getReadNotInLibraryMangas() =
        db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(RawQuery.builder().query(getReadMangaNotInLibraryQuery()).build())
            .prepare()

    fun getLastReadManga() =
        db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(
                RawQuery.builder()
                    .query(getLastReadMangaQuery())
                    .observesTables(MangaTable.TABLE)
                    .build()
            )
            .prepare()

    fun getLastFetchedManga() =
        db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(
                RawQuery.builder()
                    .query(getLastFetchedMangaQuery())
                    .observesTables(MangaTable.TABLE)
                    .build()
            )
            .prepare()

    fun getTotalChapterManga() =
        db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(
                RawQuery.builder()
                    .query(getTotalChapterMangaQuery())
                    .observesTables(MangaTable.TABLE)
                    .build()
            )
            .prepare()

    fun updateMangaFilteredScanlators(manga: Manga) =
        db.put().`object`(manga).withPutResolver(MangaScanlatorFilterFlagsPutResolver()).prepare()
}
