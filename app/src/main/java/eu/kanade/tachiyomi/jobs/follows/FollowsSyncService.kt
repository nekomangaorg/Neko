package eu.kanade.tachiyomi.jobs.follows

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FollowsSyncService {

    val preferences: PreferencesHelper = Injekt.get()
    val db: DatabaseHelper = Injekt.get()
    val sourceManager: SourceManager = Injekt.get()
    val trackManager: TrackManager = Injekt.get()
    val followsHandler: FollowsHandler = Injekt.get()

    /**
     * Syncs follows list manga into library based off the preference
     */
    suspend fun fromMangaDex(
        errorNotification: (String) -> Unit,
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ): Int {
        return withContext(Dispatchers.IO) {
            XLog.d("Starting from MangaDex sync")
            val count = AtomicInteger(0)
            val countOfAdded = AtomicInteger(0)

            val syncFollowStatusInts =
                preferences.mangadexSyncToLibraryIndexes().get().map { it.toInt() }

            sourceManager.getMangadex().fetchAllFollows().onFailure {
                errorNotification((it as? ResultError.Generic)?.errorString ?: "Error fetching follows")
            }.onSuccess { unfilteredManga ->

                val listManga = unfilteredManga.filter { it.key in syncFollowStatusInts }.values.flatten()

                XLog.d("total number from mangadex is ${listManga.size}")

                val categories = db.getCategories().executeAsBlocking()
                val defaultCategoryId = preferences.defaultCategory()
                val defaultCategory = categories.find { it.id == defaultCategoryId }

                listManga.forEach { networkManga ->
                    updateNotification(networkManga.title, count.andIncrement, listManga.size)

                    var dbManga = db.getManga(networkManga.url, sourceManager.getMangadex().id)
                        .executeAsBlocking()
                    if (dbManga == null) {
                        dbManga = Manga.create(
                            networkManga.url,
                            networkManga.title,
                            sourceManager.getMangadex().id,
                        )
                        dbManga.date_added = Date().time
                    }

                    // Increment and update if it is not already favorited
                    if (!dbManga.favorite) {
                        countOfAdded.incrementAndGet()
                        dbManga.favorite = true
                        if (defaultCategory != null) {
                            val mc = MangaCategory.create(dbManga, defaultCategory)
                            db.setMangaCategories(listOf(mc), listOf(dbManga))
                        }

                        db.insertManga(dbManga).executeAsBlocking()
                    }
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
        completeNotification: (total: Int) -> Unit,
        ids: String? = null,
    ) {
        withContext(Dispatchers.IO) {
            XLog.d("Starting to MangaDex sync")
            val count = AtomicInteger(0)
            val countNew = AtomicInteger(0)

            val listManga =
                ids?.split(", ")?.mapNotNull {
                    db.getManga(it.toLong()).executeAsBlocking()
                }?.toList()
                    ?: db.getLibraryMangaList().executeAsBlocking()

            // only add if the current tracker is not set to reading

            listManga.forEach { manga ->
                updateNotification(manga.title, count.andIncrement, listManga.size)

                // Get this manga's trackers from the database
                var mdListTrack = db.getMDList(manga).executeOnIO()

                // create mdList if missing
                if (mdListTrack == null) {
                    mdListTrack = trackManager.mdList.createInitialTracker(manga)
                    db.insertTrack(mdListTrack).executeAsBlocking()
                }

                if (mdListTrack.status == FollowStatus.UNFOLLOWED.int) {
                    withIOContext {
                        followsHandler.updateFollowStatus(
                            MdUtil.getMangaUUID(manga.url),
                            FollowStatus.READING,
                        )

                        mdListTrack.status = FollowStatus.READING.int
                        val returnedTracker = trackManager.mdList.update(mdListTrack)
                        db.insertTrack(returnedTracker).executeOnIO()
                    }
                    countNew.incrementAndGet()
                }
            }
            completeNotification(countNew.get())
        }
    }
}
