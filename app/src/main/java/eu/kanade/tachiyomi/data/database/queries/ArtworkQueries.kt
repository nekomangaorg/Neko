package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.tables.ArtworkTable
import eu.kanade.tachiyomi.data.database.tables.ChapterTable
import eu.kanade.tachiyomi.data.database.tables.HistoryTable
import eu.kanade.tachiyomi.data.database.tables.MangaTable

interface ArtworkQueries : DbProvider {

    fun getIdsOfMangaWithProjectedVolumeArtwork() =
        db.get()
            .cursor()
            .withQuery(
                RawQuery.builder()
                    .query(getIdsOfMangaWithProjectedVolumeArtworkQuery)
                    .observesTables(
                        MangaTable.TABLE,
                        ChapterTable.TABLE,
                        HistoryTable.TABLE,
                        ArtworkTable.TABLE,
                    )
                    .build()
            )
            .prepare()

    fun insertArtWorkList(artworkList: List<ArtworkImpl>) = db.put().objects(artworkList).prepare()

    fun getArtwork(manga: Manga) =
        db.get()
            .listOfObjects(ArtworkImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(ArtworkTable.TABLE)
                    .where("${ArtworkTable.COL_MANGA_ID} = ?")
                    .whereArgs(manga.id!!)
                    .build()
            )
            .prepare()

    fun getArtwork(mangaId: Long) =
        db.get()
            .listOfObjects(ArtworkImpl::class.java)
            .withQuery(
                Query.builder()
                    .table(ArtworkTable.TABLE)
                    .where("${ArtworkTable.COL_MANGA_ID} = ?")
                    .whereArgs(mangaId)
                    .build()
            )
            .prepare()

    fun deleteArtworkForManga(manga: Manga) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(ArtworkTable.TABLE)
                    .where("${ArtworkTable.COL_MANGA_ID} = ?")
                    .whereArgs(manga.id!!)
                    .build()
            )
            .prepare()
}
