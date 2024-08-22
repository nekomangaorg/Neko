package eu.kanade.tachiyomi.jobs.customlist

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onFailure
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.network.message
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CustomListSyncProcessor {

    val preferences: PreferencesHelper = Injekt.get()
    val db: DatabaseHelper = Injekt.get()
    val mangadex: MangaDex = Injekt.get<SourceManager>().mangaDex
    val trackManager: TrackManager = Injekt.get()

    /** Syncs from a list of MdLists */
    suspend fun fromMangaDex(
        errorNotification: (String) -> Unit,
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
    ): Int {
        return withContext(Dispatchers.IO) {
            TimberKt.d { "Starting from MangaDex sync" }
            val count = AtomicInteger(0)
            val countOfAdded = AtomicInteger(0)

            val categories = db.getCategories().executeAsBlocking()
            val defaultCategoryId = preferences.defaultCategory().get()
            val defaultCategory = categories.find { it.id == defaultCategoryId }

            val mangaDexListUuids = preferences.syncToFromMangaDexListUuids().get().toList()

            val mangaToAdd =
                mangaDexListUuids
                    .map { listUUID ->
                        mangadex
                            .fetchAllFromList(listUUID, true)
                            .onFailure {
                                val errorMessage =
                                    "Error getting from list $listUUID. ${it.message()}"
                                errorNotification(errorMessage)
                                TimberKt.e { errorMessage }
                            }
                            .mapBoth(
                                { it },
                                { emptyList() },
                            )
                    }
                    .flatten()
                    .distinct()
            TimberKt.d { "total number from mangadex is ${mangaToAdd.size}" }

            mangaToAdd.forEach { networkManga ->
                updateNotification(networkManga.title, count.andIncrement, mangaToAdd.size)

                var dbManga = db.getManga(networkManga.url, mangadex.id).executeAsBlocking()
                if (dbManga == null) {
                    dbManga =
                        Manga.create(
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

            preferences.syncToFromMangaDexListUuids().delete()

            completeNotification()
            countOfAdded.get()
        }
    }

    suspend fun toMangaDex(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: (total: Int) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            TimberKt.d { "Starting to MangaDex sync" }
            val count = AtomicInteger(0)
            val countNew = AtomicInteger(0)

            val mangaDexListUuids = preferences.syncToFromMangaDexListUuids().get().toList()

            if (mangaDexListUuids.isEmpty()) {
                throw Exception("Unable to sync to MangaDex as no Lists were given")
            }

            val mangaIds = preferences.syncToMangaDexMangaIds().get()

            TimberKt.d { "Number of Manga ids to sync ${mangaIds.size}" }

            val listManga =
                when (mangaIds.isEmpty()) {
                    true -> db.getLibraryMangaList().executeAsBlocking()
                    false -> mangaIds.mapNotNull { db.getManga(it.toLong()).executeAsBlocking() }
                }.distinctBy { it.uuid() }

            TimberKt.d { "Number of Manga to Sync to MangaDex ${listManga.size}" }

            listManga.forEach { manga ->
                updateNotification(manga.title, count.andIncrement, listManga.size)

                val mdListTrack = getTrack(manga)
                withIOContext { trackManager.mdList.addToLists(mdListTrack, mangaDexListUuids) }

                countNew.incrementAndGet()
            }

            preferences.syncToFromMangaDexListUuids().delete()
            preferences.syncToMangaDexMangaIds().delete()
            completeNotification(countNew.get())
        }
    }

    private suspend fun getTrack(manga: Manga): Track {
        // Get this manga's trackers from the database
        var mdListTrack = db.getMDList(manga).executeOnIO()

        // create mdList if missing
        if (mdListTrack == null) {
            mdListTrack = trackManager.mdList.createInitialTracker(manga)
            db.insertTrack(mdListTrack).executeAsBlocking()
        }
        return mdListTrack
    }
}
