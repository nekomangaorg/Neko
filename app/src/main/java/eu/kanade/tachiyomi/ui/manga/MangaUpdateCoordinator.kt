package eu.kanade.tachiyomi.ui.manga

import androidx.room.withTransaction
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.manga.shouldDownloadNewChapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.MergeRepositoryImpl
import org.nekomanga.data.database.repository.ScanlatorRepositoryImpl
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.toManga
import org.nekomanga.domain.manga.toMangaItem
import org.nekomanga.domain.manga.uuid
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.manga.MangaUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Class that updates the database with the manga, chapter, information from the source returning a
 * MangaResult at each stage of the process
 */
class MangaUpdateCoordinator {
    private val mangaRepository: MangaRepositoryImpl by injectLazy()
    private val chapterRepository: ChapterRepositoryImpl by injectLazy()
    private val categoryRepository: CategoryRepositoryImpl by injectLazy()
    private val mergeRepository: MergeRepositoryImpl by injectLazy()
    private val scanlatorRepository: ScanlatorRepositoryImpl by injectLazy()
    private val appDatabase: AppDatabase by injectLazy()

    private val preferences: PreferencesHelper by injectLazy()

    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by lazy { Injekt.get() }
    private val downloadManager: DownloadManager by injectLazy()
    private val mangaShortcutManager: MangaShortcutManager by injectLazy()
    private val mangaUseCases: MangaUseCases by injectLazy()

    fun update(mangaItem: MangaItem, isMerging: Boolean) =
        channelFlow {
                if (!sourceManager.mangaDex.checkIfUp()) {
                    send(MangaResult.Error(R.string.site_down))
                    return@channelFlow
                }

                TimberKt.d { "Starting update for ${mangaItem.title}" }

                val mangaWasInitialized = mangaItem.initialized

                // Run manga details and chapter updates in parallel.
                try {
                    coroutineScope {
                        launch { updateMangaDetailsAndPersist(mangaItem) }
                        launch { updateChapters(mangaItem, mangaWasInitialized, isMerging) }
                    }
                } catch (e: UpdateError) {
                    return@channelFlow
                }

                send(MangaResult.Success)
            }
            .flowOn(Dispatchers.IO)

    /** Fetches and persists manga details and artwork from the source. */
    private suspend fun ProducerScope<MangaResult>.updateMangaDetailsAndPersist(
        mangaItem: MangaItem
    ) {
        sourceManager.mangaDex
            .getMangaDetails(mangaItem.uuid())
            .onFailure {
                send(MangaResult.Error(text = "Error getting manga from MangaDex"))
                throw UpdateError()
            }
            .onSuccess { (networkManga, artwork) ->
                val currentManga = mangaItem.toManga()
                currentManga.copyFrom(networkManga)
                currentManga.initialized = true

                var updatedMangaItem = currentManga.toMangaItem()

                if (
                    updatedMangaItem.userTitle.isNotEmpty() &&
                        updatedMangaItem.userTitle != updatedMangaItem.title &&
                        updatedMangaItem.userTitle !in updatedMangaItem.altTitles
                ) {
                    updatedMangaItem = updatedMangaItem.copy(userTitle = "")
                }

                if (
                    networkManga.thumbnail_url != null &&
                        networkManga.thumbnail_url != mangaItem.coverUrl
                ) {
                    coverCache.deleteFromCache(mangaItem.coverUrl, mangaItem.favorite)
                }

                val mangaForDb = updatedMangaItem.toManga()

                mangaUseCases.updateMangaAggregate(
                    mangaForDb.id!!,
                    mangaForDb.url,
                    mangaForDb.favorite,
                )

                appDatabase.withTransaction {
                    mangaRepository.insertManga(mangaForDb.toEntity())
                    send(MangaResult.UpdatedManga)

                    if (artwork.isNotEmpty()) {
                        val artworkImpls = artwork.map { it.toArtworkImpl(updatedMangaItem.id) }
                        mangaRepository.deleteArtworkForManga(mangaForDb.id!!)
                        mangaRepository.insertArtworks(artworkImpls.map { it.toEntity() })
                        send(MangaResult.UpdatedArtwork)
                    }
                }
            }
    }

    private suspend fun ProducerScope<MangaResult>.updateChapters(
        mangaItem: MangaItem,
        mangaWasAlreadyInitialized: Boolean,
        isMerging: Boolean,
    ) {
        val manga = mangaItem.toManga()

        val (allChapters, readFromMerged, errorFromMerged) = fetchAndCombineChapters(manga)
        val (newChapters, removedChapters) =
            syncChaptersWithSource(
                mangaRepository = mangaRepository,
                chapterRepository = chapterRepository,
                appDatabase = appDatabase,
                rawSourceChapters = allChapters,
                manga = manga,
                errorFromMerged = errorFromMerged,
                readFromMerged = readFromMerged,
            )

        if (newChapters.isNotEmpty()) {
            if (
                preferences.downloadNewChapters().get() &&
                    mangaWasAlreadyInitialized &&
                    manga.shouldDownloadNewChapters(categoryRepository, preferences)
            ) {
                val chaptersToDownload =
                    newChapters
                        .mapNotNull {
                            if (isMerging && it.isMergedChapter()) null
                            else it.toSimpleChapter()?.toChapterItem()
                        }
                        .sortedBy { it.chapter.chapterNumber }
                downloadChapters(manga, chaptersToDownload)
            }
            mangaShortcutManager.updateShortcuts()
        }

        if (removedChapters.isNotEmpty()) {
            send(MangaResult.ChaptersRemoved(removedChapters.mapNotNull { it.id }))
        }

        send(MangaResult.UpdatedChapters)
    }

    /** Fetches chapters from the main source and any merged sources concurrently. */
    private suspend fun ProducerScope<MangaResult>.fetchAndCombineChapters(
        manga: Manga
    ): Triple<List<SChapter>, Set<String>, Boolean> = coroutineScope {
        val dexChaptersDeferred = async {
            sourceManager.mangaDex
                .fetchChapterList(manga)
                .onFailure {
                    send(MangaResult.Error(text = "MangaDex chapter fetch failed: ${it.message()}"))
                    throw UpdateError()
                }
                .getOrElse { emptyList() }
        }

        var mergedSourceError = false

        val mergedSourcesChapters =
            mergeRepository.getMergeMangaListSync(manga.id!!).map { mergeManga ->
                async {
                    val source = MergeType.getSource(mergeManga.mergeType, sourceManager)
                    source
                        .fetchChapters(mergeManga.url)
                        .onFailure {
                            val msg = "Failed to fetch from ${source.name}: ${it.message()}"
                            send(MangaResult.Error(text = msg))

                            TimberKt.e { msg }
                            mergedSourceError = true
                        }
                        .getOrElse { emptyList() }
                }
            }

        val dexChapters = dexChaptersDeferred.await()
        val mergedChapterPairs =
            mergedSourcesChapters
                .awaitAll()
                .flatten()
                .sortedWith(compareBy { getChapterNum(it.first) })

        val readFromMerged =
            mergedChapterPairs.mapNotNull { if (it.second) it.first.url else null }.toSet()
        val mergedChapters = mergedChapterPairs.map { (sChapter, _) ->
            val lastChapterNum = manga.last_chapter_number?.toFloat()
            if (lastChapterNum != null && sChapter.chapter_number == lastChapterNum) {
                sChapter.name += " [END]"
            }
            sChapter
        }

        val allChapters = (dexChapters + mergedChapters).sortedWith(compareBy { getChapterNum(it) })
        Triple(allChapters, readFromMerged, mergedSourceError)
    }

    /** Filters and downloads the given list of chapters. */
    fun downloadChapters(manga: Manga, chapters: List<ChapterItem>) {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        val chaptersToDownload = chapters.mapNotNull { item ->
            if (isChapterDownloadable(item, blockedGroups, blockedUploaders)) {
                item.chapter.toDbChapter()
            } else {
                null
            }
        }

        downloadManager.downloadChapters(manga, chaptersToDownload)
    }

    private fun isChapterDownloadable(
        item: ChapterItem,
        blockedGroups: Set<String>,
        blockedUploaders: Set<String>,
    ): Boolean {
        if (item.isDownloaded) return false
        val scanlators = item.chapter.scanlatorList()
        if (scanlators.any { it in blockedGroups }) return false

        // Download if it has a group OR if the uploader isn't blocked for "no group" chapters
        val isNoGroup = Constants.NO_GROUP in scanlators
        return !isNoGroup || item.chapter.uploader !in blockedUploaders
    }

    suspend fun updateGroup(group: String) {
        sourceManager.mangaDex.getScanlatorGroup(group).onSuccess {
            val scanlatorGroupImpl = it.toScanlatorGroupImpl()
            if (group == scanlatorGroupImpl.name) {
                scanlatorRepository.insertScanlatorGroups(listOf(scanlatorGroupImpl.toEntity()))
            }
        }
    }

    suspend fun updateUploader(uploader: String) {
        sourceManager.mangaDex.getUploader(uploader).onSuccess {
            mangaRepository.insertUploader(listOf(it.toUploaderImpl().toEntity()))
        }
    }
}

/** Represents the state of the manga update process. */
sealed class MangaResult {
    class Error(val id: Int? = null, val text: String? = null) : MangaResult()

    object UpdatedManga : MangaResult()

    object UpdatedArtwork : MangaResult()

    object UpdatedChapters : MangaResult()

    class ChaptersRemoved(val chapterIdsRemoved: List<Long>) : MangaResult()

    object Success : MangaResult()
}

private class UpdateError : Exception()
