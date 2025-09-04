package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.Queries
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.tables.TrackTable
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService

interface TrackQueries : DbProvider {

    fun getTrackByTrackId(id: Long) =
        db.get()
            .`object`(Track::class.java)
            .withQuery(
                Query.builder()
                    .table(TrackTable.TABLE)
                    .where("${TrackTable.COL_ID} = ?")
                    .whereArgs(id)
                    .build()
            )
            .prepare()

    fun getTracks(manga: Manga) = getTracks(manga.id)

    fun getTracks(mangaId: Long?) =
        db.get()
            .listOfObjects(Track::class.java)
            .withQuery(
                Query.builder()
                    .table(TrackTable.TABLE)
                    .where("${TrackTable.COL_MANGA_ID} = ?")
                    .whereArgs(mangaId)
                    .build()
            )
            .prepare()

    fun getTracks(mangaIds: List<Long>) =
        db.get()
            .listOfObjects(Track::class.java)
            .withQuery(
                Query.builder()
                    .table(TrackTable.TABLE)
                    .where("${TrackTable.COL_MANGA_ID} IN (${Queries.placeholders(mangaIds.size)})")
                    .whereArgs(*mangaIds.toTypedArray())
                    .build()
            )
            .prepare()

    fun getAllTracks() =
        db.get()
            .listOfObjects(Track::class.java)
            .withQuery(Query.builder().table(TrackTable.TABLE).build())
            .prepare()

    fun getMDList(manga: Manga) =
        db.get()
            .`object`(Track::class.java)
            .withQuery(
                Query.builder()
                    .table(TrackTable.TABLE)
                    .where("${TrackTable.COL_MANGA_ID} = ? AND ${TrackTable.COL_SYNC_ID} = ?")
                    .whereArgs(manga.id, TrackManager.MDLIST)
                    .build()
            )
            .prepare()

    fun insertTrack(track: Track) = db.put().`object`(track).prepare()

    fun insertTracks(tracks: List<Track>) = db.put().objects(tracks).prepare()

    fun deleteTrackForManga(manga: Manga, sync: TrackService) = deleteTrackForManga(manga.id, sync)

    fun deleteTrackForManga(mangaId: Long?, sync: TrackService) =
        db.delete()
            .byQuery(
                DeleteQuery.builder()
                    .table(TrackTable.TABLE)
                    .where("${TrackTable.COL_MANGA_ID} = ? AND ${TrackTable.COL_SYNC_ID} = ?")
                    .whereArgs(mangaId, sync.id)
                    .build()
            )
            .prepare()

    fun deleteTracks() =
        db.delete().byQuery(DeleteQuery.builder().table(TrackTable.TABLE).build()).prepare()
}
