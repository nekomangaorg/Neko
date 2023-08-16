package eu.kanade.tachiyomi.jobs.customlist

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onFailure
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.handlers.SubscriptionHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import org.nekomanga.core.loggycat
import org.nekomanga.domain.network.message
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CustomListSyncService {

    val preferences: PreferencesHelper = Injekt.get()
    val db: DatabaseHelper = Injekt.get()
    val mangadex: MangaDex = Injekt.get<SourceManager>().mangaDex
    val trackManager: TrackManager = Injekt.get()
    val followsHandler: SubscriptionHandler = Injekt.get()

    /**
     * Syncs follows list manga into library based off the preference
     */
    suspend fun fromMangaDex(
        listUuids: List<String>,
        errorNotification: (String) -> Unit,
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ): Int {
        return withContext(Dispatchers.IO) {
            loggycat { "Starting from MangaDex sync" }
            val count = AtomicInteger(0)
            val countOfAdded = AtomicInteger(0)

            val categories = db.getCategories().executeAsBlocking()
            val defaultCategoryId = preferences.defaultCategory().get()
            val defaultCategory = categories.find { it.id == defaultCategoryId }

            val mangaToAdd = listUuids.map { listUUID ->
                mangadex.fetchAllFromList(listUUID, true).onFailure {
                    val errorMessage = "Error getting from list $listUUID. ${it.message()}"
                    errorNotification(errorMessage)
                    loggycat(LogPriority.ERROR) { errorMessage }
                }.mapBoth(
                    { it },
                    {
                        emptyList()
                    },
                )
            }.flatten().distinct()
            loggycat { "total number from mangadex is ${mangaToAdd.size}" }

            mangaToAdd.forEach { networkManga ->
                updateNotification(networkManga.title, count.andIncrement, mangaToAdd.size)

                var dbManga = db.getManga(networkManga.url, mangadex.id)
                    .executeAsBlocking()
                if (dbManga == null) {
                    dbManga = Manga.create(
                        networkManga.url,
                        networkManga.title,
                        mangadex.id,
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
            loggycat { "Starting to MangaDex sync" }
            val count = AtomicInteger(0)
            val countNew = AtomicInteger(0)

            val listManga =
                ids?.split(", ")?.mapNotNull {
                    db.getManga(it.toLong()).executeAsBlocking()
                }?.toList()
                    ?: db.getLibraryMangaList().executeAsBlocking()

            // only add if the current tracker is not set to reading

            listManga.distinctBy { it.uuid() }.forEach { manga ->
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
                        //val returnedTracker = trackManager.mdList.update(mdListTrack)
                        //    db.insertTrack(returnedTracker).executeOnIO()
                    }
                    countNew.incrementAndGet()
                }
            }
            completeNotification(countNew.get())
        }
    }
}
