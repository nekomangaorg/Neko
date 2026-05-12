package eu.kanade.tachiyomi.jobs.follows

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
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
                            .groupBy { FollowStatus.fromStringRes(it.displayTextRes).int }
                            .filter { it.key in syncFollowStatusInts }
                            .values
                            .flatten()

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

                    val mangaIdsToUpdate = listManga.mapNotNull { networkManga ->
                        updateNotification(
                            networkManga.title,
                            count.getAndIncrement(),
                            listManga.size,
                        )

                        var dbManga = existingMangaMap[networkManga.url]

                        if (dbManga == null) {
                            dbManga =
                                Manga.create(
                                    networkManga.url,
                                    networkManga.title,
                                    sourceManager.mangaDex.id,
                                )
                            dbManga.date_added = Date().time
                            dbManga.favorite = true

                            countOfAdded.incrementAndGet()
                            val id = mangaRepository.insertManga(dbManga)
                            dbManga.id = id

                            if (defaultCategory != null) {
                                val mc = MangaCategory.create(dbManga, defaultCategory)
                                categoryRepository.setMangaCategories(listOf(mc), listOf(id))
                            }
                            return@mapNotNull id
                        }

                        // Increment and update if it is not already favorited
                        if (!dbManga.favorite) {
                            countOfAdded.incrementAndGet()
                            dbManga.favorite = true

                            mangaRepository.updateManga(dbManga)

                            val mangaId = dbManga.id ?: return@mapNotNull null

                            if (defaultCategory != null) {
                                val mc = MangaCategory.create(dbManga, defaultCategory)
                                categoryRepository.setMangaCategories(listOf(mc), listOf(mangaId))
                            }

                            return@mapNotNull mangaId
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
            val allTracks =
                mangaIds
                    .chunked(500)
                    .flatMap { chunk -> trackRepository.getTracksForMangaByIds(chunk) }
                    .filter { it.sync_id == TrackManager.MDLIST }
                    .groupBy { it.manga_id }

            // only add if the current tracker is not set to reading

            listManga
                .distinctBy { it.uuid() }
                .forEach { manga ->
                    updateNotification(manga.title, count.getAndIncrement(), listManga.size)

                    // Get this manga's trackers from the database
                    var mdListTrack = allTracks[manga.id]?.firstOrNull()

                    // create mdList if missing
                    if (mdListTrack == null) {
                        mdListTrack = trackManager.mdList.createInitialTracker(manga)
                        trackRepository.insertTrack(mdListTrack)
                    }

                    if (mdListTrack.status == FollowStatus.UNFOLLOWED.int) {
                        followsHandler.updateFollowStatus(
                            MdUtil.getMangaUUID(manga.url),
                            FollowStatus.READING,
                        )

                        mdListTrack.status = FollowStatus.READING.int
                        val returnedTracker = trackManager.mdList.update(mdListTrack)
                        trackRepository.insertTrack(returnedTracker)
                        countNew.incrementAndGet()
                    }

                    if (mangaDexPreferences.readingSync().get()) {
                        val mangaId = manga.id ?: return@forEach
                        try {
                            val readMdChapters =
                                chapterRepository.getChaptersForManga(mangaId).mapNotNull {
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
                            TimberKt.e(e) { "Failed to sync read chapters for '${manga.title}'" }
                        }
                    }
                }
            completeNotification(countNew.get())
        }
    }
}
