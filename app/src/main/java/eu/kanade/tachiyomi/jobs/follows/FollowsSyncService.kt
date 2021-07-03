package eu.kanade.tachiyomi.jobs.follows

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

class FollowsSyncService {

    val preferences: PreferencesHelper = Injekt.get()
    val db: DatabaseHelper = Injekt.get()
    val sourceManager: SourceManager = Injekt.get()
    val trackManager: TrackManager = Injekt.get()

    /**
     * Syncs follows list manga into library based off the preference
     */
    suspend fun fromMangaDex(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ): Int {
        return withContext(Dispatchers.IO) {
            XLog.d("Starting from MangaDex sync")
            val count = AtomicInteger(0)
            val countOfAdded = AtomicInteger(0)

            val syncFollowStatusInts =
                preferences.mangadexSyncToLibraryIndexes().get().map { it.toInt() }

            val listManga = sourceManager.getMangadex().fetchAllFollows().filter { networkManga ->
                syncFollowStatusInts.contains(networkManga.follow_status?.int ?: 0)
            }
            XLog.d("total number from mangadex is ${listManga.size}")


            listManga.forEach { networkManga ->
                updateNotification(networkManga.title, count.andIncrement, listManga.size)

                var dbManga = db.getManga(networkManga.url, sourceManager.getMangadex().id)
                    .executeAsBlocking()
                if (dbManga == null) {
                    dbManga = Manga.create(
                        networkManga.url,
                        networkManga.title,
                        sourceManager.getMangadex().id
                    )
                    dbManga.date_added = Date().time
                }

                // Increment and update if it is not already favorited
                if (!dbManga.favorite) {
                    countOfAdded.incrementAndGet()
                    dbManga.favorite = true
                    dbManga.copyFrom(networkManga)

                    db.insertManga(dbManga).executeAsBlocking()
                }
            }
            completeNotification()
            countOfAdded.get()
        }
    }

    /**
     * Syncs Library manga to MangaDex as Reading and puts them on the follows list
     */
    suspend fun toMangaDex(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ): Int {
        return withContext(Dispatchers.IO) {
            val count = AtomicInteger(0)
            val countNew = AtomicInteger(0)

            val listManga = db.getLibraryMangaList().executeAsBlocking()
            // filter all follows from Mangadex and only add reading or rereading manga to library

            listManga.forEach { manga ->

                updateNotification(manga.title, count.andIncrement, listManga.size)

                // Get this manga's trackers from the database
                val dbTracks = db.getTracks(manga).executeAsBlocking()
                val trackItem = TrackItem(dbTracks.find { it.sync_id == trackManager.mdList.id },
                    trackManager.mdList)

                if (trackItem.track?.status == FollowStatus.UNFOLLOWED.int) {
                    trackItem.track.status = FollowStatus.READING.int
                    val returnedTracker = trackItem.service.update(trackItem.track)
                    db.insertTrack(returnedTracker).executeOnIO()
                    countNew.incrementAndGet()
                }

            }
            completeNotification()

            countNew.get()
        }
    }
}
