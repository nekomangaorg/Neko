package eu.kanade.tachiyomi.jobs.follows

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.data.database.model.toCategory
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.model.toLegacyModel
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.model.toTrack
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.TrackRepositoryImpl
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class FollowsSyncProcessor {

    val preferences: PreferencesHelper by injectLazy()
    val mangaDexPreferences: MangaDexPreferences by injectLazy()
    val libraryPreference: LibraryPreferences by injectLazy()
    private val mangaRepository: MangaRepositoryImpl by injectLazy()
    private val categoryRepository: CategoryRepositoryImpl by injectLazy()
    private val trackRepository: TrackRepositoryImpl by injectLazy()
    val sourceManager: SourceManager by injectLazy()
    val trackManager: TrackManager by injectLazy()
    private val followsHandler: FollowsHandler by injectLazy()

    /** Syncs follows list manga into library based off the preference */
    suspend fun fromMangaDex(
        errorNotification: (String) -> Unit,
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
        updateManga: (List<Long>) -> Unit,
    ): Int {
        return withContext(Dispatchers.IO) {
            TimberKt.d { "Starting from MangaDex sync" }
            val count = AtomicInteger(0)
            val countOfAdded = AtomicInteger(0)

            val syncFollowStatusInts =
                mangaDexPreferences.mangaDexPullToLibraryIndices().get().map { it.toInt() }

            sourceManager.mangaDex
                .fetchAllFollows()
                .onFailure {
                    errorNotification(
                        (it as? ResultError.Generic)?.errorString ?: "Error fetching follows"
                    )
                }
                .onSuccess { unfilteredManga ->
                    val listManga =
                        unfilteredManga
                            .groupBy { FollowStatus.fromStringRes(it.displayTextRes).int }
                            .filter { it.key in syncFollowStatusInts }
                            .values
                            .flatten()

                    TimberKt.d { "total number from mangadex is ${listManga.size}" }

                    val categories = categoryRepository.getAllCategoriesList()
                    val defaultCategoryId = libraryPreference.defaultCategory().get()
                    val defaultCategory = categories.find { it.id == defaultCategoryId }

                    val mangaIdsToUpdate = listManga.mapNotNull { networkManga ->
                        updateNotification(networkManga.title, count.getAndIncrement(), listManga.size)

                        val dbMangaEntity =
                            mangaRepository.getMangaByUrlAndSource(networkManga.url, sourceManager.mangaDex.id)

                        var dbManga = dbMangaEntity?.toManga()

                        if (dbManga == null) {
                            dbManga =
                                Manga.create(
                                    networkManga.url,
                                    networkManga.title,
                                    sourceManager.mangaDex.id,
                                )
                            dbManga.date_added = Date().time
                        }

                        // Increment and update if it is not already favorited
                        if (!dbManga.favorite) {
                            countOfAdded.incrementAndGet()
                            dbManga.favorite = true

                            val id = mangaRepository.insertManga(dbManga.toEntity())
                            dbManga.id = id

                            if (defaultCategory != null) {
                                categoryRepository.setMangaCategories(
                                    listOf(MangaCategory.create(dbManga, defaultCategory.toCategory()).toEntity()),
                                    listOf(dbManga.id!!),
                                )
                            }

                            return@mapNotNull dbManga.id
                        }
                        return@mapNotNull null
                    }
                    updateManga(mangaIdsToUpdate)
                }

            completeNotification()
            countOfAdded.get()
        }
    }

    /** Syncs Library manga to MangaDex as Reading and puts them on the follows list */
    suspend fun toMangaDex(
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: (total: Int) -> Unit,
        ids: String? = null,
    ) {
        withContext(Dispatchers.IO) {
            TimberKt.d { "Starting to MangaDex sync" }
            val count = AtomicInteger(0)
            val countNew = AtomicInteger(0)

            val listManga =
                if (ids != null) {
                    val idsList = ids.split(", ").map { it.toLong() }
                    mangaRepository.getMangas(idsList).map { it.toManga() }
                } else {
                    mangaRepository.getLibraryMangaListSync().map { it.toLegacyModel() }
                }

            // only add if the current tracker is not set to reading

            listManga
                .distinctBy { it.uuid() }
                .forEach { manga ->
                    updateNotification(manga.title, count.getAndIncrement(), listManga.size)

                    // Get this manga's trackers from the database
                    var mdListTrack: Track? = trackRepository.getTracksForMangaSync(manga.id!!)
                        .find { it.syncId == TrackManager.MDLIST }
                        ?.toTrack()

                    // create mdList if missing
                    if (mdListTrack == null) {
                        mdListTrack = trackManager.mdList.createInitialTracker(manga)
                        trackRepository.insertTrack(mdListTrack!!.toEntity())
                    }

                    if (mdListTrack!!.status == FollowStatus.UNFOLLOWED.int) {
                        withIOContext {
                            followsHandler.updateFollowStatus(
                                MdUtil.getMangaUUID(manga.url),
                                FollowStatus.READING,
                            )

                            mdListTrack!!.status = FollowStatus.READING.int
                            val returnedTracker = trackManager.mdList.update(mdListTrack!!)
                            trackRepository.insertTrack(returnedTracker.toEntity())
                        }
                        countNew.incrementAndGet()
                    }
                }
            completeNotification(countNew.get())
        }
    }
}
