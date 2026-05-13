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
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases

class FollowsSyncProcessor(
    private val preferences: PreferencesHelper,
    private val mangaDexPreferences: MangaDexPreferences,
    private val libraryPreference: LibraryPreferences,
    private val mangaRepository: MangaRepository,
    private val categoryRepository: CategoryRepository,
    private val chapterRepository: ChapterRepository,
    private val trackRepository: TrackRepository,
    private val sourceManager: SourceManager,
    private val trackManager: TrackManager,
    private val chapterUseCases: ChapterUseCases,
    private val followsHandler: FollowsHandler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /** Syncs follows list manga into library based off the preference */
    suspend fun fromMangaDex(
        errorNotification: (String) -> Unit,
        updateNotification: (title: String, progress: Int, total: Int) -> Unit,
        completeNotification: () -> Unit,
        updateManga: (List<Long>) -> Unit,
    ): Int {
        return withContext(ioDispatcher) {
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
                            .asSequence()
                            .filter {
                                FollowStatus.fromStringRes(it.displayTextRes).int in
                                    syncFollowStatusInts
                            }
                            .toList()

                    TimberKt.d { "total number from mangadex is ${listManga.size}" }

                    val categories = categoryRepository.getCategories()
                    val defaultCategoryId = libraryPreference.defaultCategory().get()
                    val defaultCategory = categories.find { it.id == defaultCategoryId }

                    val urls = listManga.map { it.url }
                    val existingMangaMap =
                        urls
                            .chunked(500)
                            .flatMap { chunk -> mangaRepository.getMangaByUrls(chunk) }
                            .filter { it.source == sourceManager.mangaDex.id }
                            .associateBy { it.url }

                    val mangaToInsert = mutableListOf<Manga>()
                    val mangaToUpdate = mutableListOf<Manga>()
                    val mangaIdsThatWereUpdated = mutableListOf<Long>()

                    listManga.forEach { networkManga ->
                        try {
                            updateNotification(
                                networkManga.title,
                                count.getAndIncrement(),
                                listManga.size,
                            )

                            val dbManga = existingMangaMap[networkManga.url]

                            if (dbManga == null) {
                                val newManga =
                                    Manga.create(
                                        networkManga.url,
                                        networkManga.title,
                                        sourceManager.mangaDex.id,
                                    )
                                newManga.date_added = Date().time
                                newManga.favorite = true
                                mangaToInsert.add(newManga)
                            } else if (!dbManga.favorite) {
                                dbManga.favorite = true
                                mangaToUpdate.add(dbManga)
                                dbManga.id?.let { mangaIdsThatWereUpdated.add(it) }
                            }
                        } catch (e: Exception) {
                            TimberKt.e(e) { "Error processing manga ${networkManga.title}" }
                        }
                    }

                    val insertedIds =
                        if (mangaToInsert.isNotEmpty()) {
                            val ids = mangaRepository.insertMangaList(mangaToInsert)
                            countOfAdded.addAndGet(ids.size)
                            if (defaultCategory != null) {
                                val mangaCategories =
                                    mangaToInsert.zip(ids).map { (manga, id) ->
                                        manga.id = id
                                        MangaCategory.create(manga, defaultCategory)
                                    }
                                categoryRepository.setMangaCategories(mangaCategories, ids)
                            }
                            ids
                        } else {
                            emptyList()
                        }

                    if (mangaToUpdate.isNotEmpty()) {
                        mangaRepository.updateMangaList(mangaToUpdate)
                        countOfAdded.addAndGet(mangaToUpdate.size)
                        if (defaultCategory != null) {
                            val mangaCategories = mangaToUpdate.map { manga ->
                                MangaCategory.create(manga, defaultCategory)
                            }
                            categoryRepository.setMangaCategories(
                                mangaCategories,
                                mangaIdsThatWereUpdated,
                            )
                        }
                    }

                    updateManga(insertedIds + mangaIdsThatWereUpdated)
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
        withContext(ioDispatcher) {
            TimberKt.d { "Starting to MangaDex sync" }
            val count = AtomicInteger(0)
            val countNew = AtomicInteger(0)

            val listManga =
                ids?.split(", ")
                    ?.map { it.toLong() }
                    ?.chunked(900)
                    ?.flatMap { chunk -> mangaRepository.getMangaByIds(chunk) }
                    ?.toList() ?: mangaRepository.getLibraryList()

            val mangaIds = listManga.mapNotNull { it.id }
            val allTracksMap =
                mangaIds
                    .chunked(500)
                    .flatMap { chunk -> trackRepository.getTracksForMangaByIds(chunk) }
                    .filter { it.sync_id == TrackManager.MDLIST }
                    .groupBy { it.manga_id }

            val allChaptersMap =
                if (mangaDexPreferences.readingSync().get()) {
                    mangaIds
                        .chunked(500)
                        .flatMap { chunk -> chapterRepository.getChaptersForMangaIds(chunk) }
                        .groupBy { it.manga_id }
                } else {
                    emptyMap()
                }

            val tracksToUpsert = mutableListOf<Track>()
            val mdList =
                trackManager.mdList
                    ?: throw IllegalStateException("MangaDex Track service not found")

            listManga
                .asSequence()
                .distinctBy { it.uuid() }
                .forEach { manga ->
                    val mangaId = manga.id ?: return@forEach
                    try {
                        updateNotification(manga.title, count.getAndIncrement(), listManga.size)

                        // Get this manga's trackers from the database
                        var mdListTrack = allTracksMap[mangaId]?.firstOrNull()
                        var trackToSave: Track? = null

                        // create mdList if missing
                        if (mdListTrack == null) {
                            mdListTrack = mdList.createInitialTracker(manga)
                            trackToSave = mdListTrack
                        }

                        if (mdListTrack.status == FollowStatus.UNFOLLOWED.int) {
                            try {
                                followsHandler.updateFollowStatus(
                                    MdUtil.getMangaUUID(manga.url),
                                    FollowStatus.READING,
                                )

                                mdListTrack.status = FollowStatus.READING.int
                                val returnedTracker = mdList.update(mdListTrack)
                                // Add to upsert list. OnConflictStrategy.REPLACE will handle it
                                trackToSave = returnedTracker
                                countNew.incrementAndGet()
                            } catch (e: Exception) {
                                if (trackToSave != null) {
                                    trackToSave.status = FollowStatus.UNFOLLOWED.int
                                }
                                TimberKt.e(e) {
                                    "Failed to update follow status for '${manga.title}'"
                                }
                            }
                        }

                        trackToSave?.let { tracksToUpsert.add(it) }

                        if (mangaDexPreferences.readingSync().get()) {
                            try {
                                val readMdChapters =
                                    (allChaptersMap[mangaId] ?: emptyList()).mapNotNull {
                                        if (!it.read || it.isMergedChapter()) return@mapNotNull null
                                        it.toSimpleChapter()?.toChapterItem()
                                    }

                                if (readMdChapters.isNotEmpty()) {
                                    chapterUseCases.markChaptersRemote(
                                        markAction = ChapterMarkActions.Read(),
                                        mangaUuid = manga.uuid(),
                                        chapterItems = readMdChapters,
                                    )
                                }
                            } catch (e: Exception) {
                                TimberKt.e(e) {
                                    "Failed to sync read chapters for '${manga.title}'"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        TimberKt.e(e) { "Error processing manga ${manga.title} for toMangaDex" }
                    }
                }

            if (tracksToUpsert.isNotEmpty()) {
                // Remove duplicates if any (e.g. if a tracker was created and then updated in the
                // same loop)
                val finalTracks = tracksToUpsert.associateBy { it.manga_id }.values.toList()
                trackRepository.insertTracks(finalTracks)
            }

            completeNotification(countNew.get())
        }
    }
}
