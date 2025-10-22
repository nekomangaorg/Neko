package eu.kanade.tachiyomi.ui.manga

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
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
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.toManga
import org.nekomanga.domain.manga.toMangaItem
import org.nekomanga.domain.manga.uuid
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Class that updates the database with the manga, chapter, information from the source returning a
 * MangaResult at each stage of the process
 */
class MangaUpdateCoordinator {
    private val db: DatabaseHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by lazy { Injekt.get() }
    private val downloadManager: DownloadManager by injectLazy()
    private val mangaShortcutManager: MangaShortcutManager by injectLazy()

    fun update(mangaItem: MangaItem, isMerging: Boolean) =
        channelFlow {
                if (!sourceManager.mangaDex.checkIfUp()) {
                    send(MangaResult.Error(R.string.site_down))
                    return@channelFlow
                }

                TimberKt.d { "Starting update for ${mangaItem.title}" }

                val mangaWasInitialized = mangaItem.initialized

                // Run manga details and chapter updates in parallel.
                coroutineScope {
                    launch { updateMangaDetailsAndPersist(mangaItem) }
                    launch { updateChapters(mangaItem, mangaWasInitialized, isMerging) }
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
                cancel()
                return
            }
            .onSuccess { (networkManga, artwork) ->
                val currentManga = mangaItem.toManga()
                currentManga.copyFrom(networkManga)
                currentManga.initialized = true

                var updatedMangaItem = currentManga.toMangaItem()

                if (
                    updatedMangaItem.userTitle.isNotEmpty() &&
                        updatedMangaItem.userTitle != updatedMangaItem.ogTitle &&
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

                db.inTransaction {
                    db.insertManga(mangaForDb).executeAsBlocking()
                    send(MangaResult.UpdatedManga)

                    if (artwork.isNotEmpty()) {
                        val artworkImpls = artwork.map { it.toArtworkImpl(updatedMangaItem.id) }
                        db.deleteArtworkForManga(mangaForDb).executeAsBlocking()
                        db.insertArtWorkList(artworkImpls).executeAsBlocking()
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
                db = db,
                rawSourceChapters = allChapters,
                manga = manga,
                errorFromMerged = errorFromMerged,
                readFromMerged = readFromMerged,
            )

        if (newChapters.isNotEmpty()) {
            if (
                preferences.downloadNewChapters().get() &&
                    mangaWasAlreadyInitialized &&
                    manga.shouldDownloadNewChapters(db, preferences)
            ) {
                val chaptersToDownload =
                    newChapters
                        .filterNot { isMerging && it.isMergedChapter() }
                        .mapNotNull { it.toSimpleChapter()?.toChapterItem() }
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
                    cancel()
                }
                .getOrElse { emptyList() }
        }

        var mergedSourceError = false

        val mergedSourcesChapters =
            db.getMergeMangaList(manga).executeAsBlocking().map { mergeManga ->
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

        val readFromMerged = mergedChapterPairs.filter { it.second }.map { it.first.url }.toSet()
        val mergedChapters =
            mergedChapterPairs.map { (sChapter, _) ->
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

        val chaptersToDownload =
            chapters
                .filter { isChapterDownloadable(it, blockedGroups, blockedUploaders) }
                .map { it.chapter.toDbChapter() }

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
                db.insertScanlatorGroups(listOf(scanlatorGroupImpl)).executeAsBlocking()
            }
        }
    }

    suspend fun updateUploader(uploader: String) {
        sourceManager.mangaDex.getUploader(uploader).onSuccess {
            db.insertUploader(listOf(it.toUploaderImpl())).executeAsBlocking()
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
