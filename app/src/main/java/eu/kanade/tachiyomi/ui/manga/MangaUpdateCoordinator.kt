package eu.kanade.tachiyomi.ui.manga

import androidx.core.text.isDigitsOnly
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.mergeSorted
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
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

    /** Channel flow for updating the Manga/Chapters in the given scope */
    suspend fun update(manga: Manga, scope: CoroutineScope, isMerging: Boolean) =
        channelFlow {
                val mangaWasInitialized = manga.initialized

                if (!sourceManager.mangaDex.checkIfUp()) {
                    send(MangaResult.Error(R.string.site_down))
                    return@channelFlow
                }

                val mangaUUID = manga.uuid()

                if (mangaUUID.isDigitsOnly()) {
                    send(MangaResult.Error(R.string.v3_manga))
                    return@channelFlow
                }

                TimberKt.d { "Begin processing manga/chapter update for manga $mangaUUID" }

                val mangaJob = startMangaJob(scope, manga)

                if (mangaJob.isCompleted || mangaJob.isActive) {
                    val chapterJob = startChapterJob(scope, manga, mangaWasInitialized, isMerging)
                    mangaJob.join()
                    chapterJob.join()

                    if (mangaJob.isCompleted && chapterJob.isCompleted) {
                        send(MangaResult.Success)
                    }
                }
            }
            .flowOn(Dispatchers.IO)

    /** Starts the updating of manga from Dex */
    private fun ProducerScope<MangaResult>.startMangaJob(scope: CoroutineScope, manga: Manga): Job {
        return scope.launchIO {
            sourceManager.mangaDex
                .getMangaDetails(manga.uuid())
                .onFailure {
                    send(MangaResult.Error(text = "Error getting manga from MangaDex"))
                    cancel()
                }
                .onSuccess {
                    val resultingManga = it.first
                    val artwork = it.second

                    val originalThumbnail = manga.thumbnail_url

                    resultingManga.let { networkManga ->
                        manga.copyFrom(networkManga)
                        manga.initialized = true
                        // This clears custom titles from j2k/sy and if MangaDex removes the title
                        manga.user_title?.let { customTitle ->
                            if (
                                customTitle != manga.originalTitle &&
                                    customTitle !in manga.getAltTitles()
                            ) {
                                manga.user_title = null
                            }
                        }
                        if (
                            networkManga.thumbnail_url != null &&
                                networkManga.thumbnail_url != originalThumbnail &&
                                originalThumbnail != MdConstants.noCoverUrl
                        ) {
                            coverCache.deleteFromCache(originalThumbnail, manga.favorite)
                        }
                        db.insertManga(manga).executeOnIO()
                        send(MangaResult.UpdatedManga)
                    }

                    if (artwork.isNotEmpty()) {
                        artwork
                            .map { art -> art.toArtworkImpl(manga.id!!) }
                            .let { transformedArtwork ->
                                db.deleteArtworkForManga(manga).executeOnIO()
                                db.insertArtWorkList(transformedArtwork).executeOnIO()
                            }
                    }
                    send(MangaResult.UpdatedArtwork)
                }
        }
    }

    /**
     * Starts the update chapter job for the main source, as well as merged source, diffs them
     * compared to the existing info, downloads new ones if needed
     */
    private fun ProducerScope<MangaResult>.startChapterJob(
        scope: CoroutineScope,
        manga: Manga,
        mangaWasAlreadyInitialized: Boolean,
        isMerging: Boolean,
    ): Job {
        return scope.launchIO {
            val deferredChapters = async {
                sourceManager.mangaDex
                    .fetchChapterList(manga)
                    .onFailure {
                        send(MangaResult.Error(text = "error with MangaDex: ${it.message()}"))
                        cancel()
                    }
                    .getOrElse { emptyList() }
            }

            val mergedMangaList = db.getMergeMangaList(manga).executeOnIO()

            val deferredMergedChapters =
                if (mergedMangaList.isNotEmpty()) {
                    mergedMangaList.map { mergeManga ->
                        async {
                            // in the future check the merge type
                            MergeType.getSource(mergeManga.mergeType, sourceManager)
                                .fetchChapters(mergeManga.url)
                                .onFailure {
                                    send(
                                        MangaResult.Error(
                                            text =
                                                "error with ${MergeType.getMergeTypeName(mergeManga.mergeType)}: ${it.message()}"
                                        )
                                    )
                                    this.cancel()
                                }
                                .getOrElse { emptyList() }
                                .map { (sChapter, status) ->
                                    val sameVolume =
                                        sChapter.vol == "" ||
                                            manga.last_volume_number == null ||
                                            sChapter.vol == manga.last_volume_number.toString()
                                    if (
                                        manga.last_chapter_number != null &&
                                            sChapter.chapter_number ==
                                                manga.last_chapter_number?.toFloat() &&
                                            sameVolume
                                    ) {
                                        sChapter.name += " [END]"
                                    }
                                    sChapter to status
                                }
                        }
                    }
                } else {
                    emptyList()
                }

            val mergedChapters =
                if (deferredMergedChapters.size > 1) {
                    deferredMergedChapters
                        .awaitAll()
                        .mergeSorted(
                            compareBy<SChapterStatusPair> { getChapterNum(it.first) != null }
                                .thenBy { getChapterNum(it.first) }
                        )
                } else {
                    deferredMergedChapters.awaitAll().flatten()
                }
            val readFromMerged = mergedChapters.filter { it.second }.map { it.first.url }.toSet()

            val allChapters =
                listOf(deferredChapters.await(), mergedChapters.map { it.first })
                    .mergeSorted(
                        compareBy<SChapter> { getChapterNum(it) != null }
                            .thenBy { getChapterNum(it) }
                    )

            val (newChapters, removedChapters) =
                syncChaptersWithSource(db, allChapters, manga, readFromMerged = readFromMerged)
            // chapters that were added
            if (newChapters.isNotEmpty()) {
                val downloadNew = preferences.downloadNewChapters().get()
                if (downloadNew && mangaWasAlreadyInitialized) {
                    if (manga.shouldDownloadNewChapters(db, preferences)) {
                        downloadChapters(
                            manga,
                            newChapters
                                .filterNot { isMerging && it.isMergedChapter() }
                                .mapNotNull { it.toSimpleChapter()?.toChapterItem() }
                                .sortedBy { it.chapter.chapterNumber },
                        )
                    }
                }
                mangaShortcutManager.updateShortcuts()
            }
            // chapters that were removed
            if (removedChapters.isNotEmpty()) {
                val removedChaptersId = removedChapters.mapNotNull { it.id }
                send(MangaResult.ChaptersRemoved(removedChaptersId))
            }

            send(MangaResult.UpdatedChapters)
        }
    }

    /**
     * Downloads the given list of chapters with the manager.
     *
     * @param chapters the list of chapters to download.
     */
    fun downloadChapters(manga: Manga, chapters: List<ChapterItem>) {
        val blockedScanlators = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        downloadManager.downloadChapters(
            manga,
            chapters
                .filter {
                    val scanlators = it.chapter.scanlatorList()
                    !it.isDownloaded &&
                        scanlators.none { scanlator -> scanlator in blockedScanlators } &&
                        (Constants.NO_GROUP !in scanlators || it.chapter.uploader !in blockedUploaders)
                }
                .map { it.chapter.toDbChapter() },
        )
    }

    suspend fun updateScanlator(scanlator: String) {
        sourceManager.mangaDex.getScanlator(scanlator).onSuccess {
            // Sanity check for merged
            val scanlatorImpl = it.toScanlatorImpl()
            if (scanlator == scanlatorImpl.name) {
                db.insertScanlators(listOf(scanlatorImpl)).executeAsBlocking()
            }
        }
    }

    suspend fun updateUploader(uploader: String) {
        sourceManager.mangaDex.getUploader(uploader).onSuccess {
            // Uploader only comes from the MD source
            db.insertUploader(listOf(it.toUploaderImpl())).executeAsBlocking()
        }
    }
}

/** Types of Results that can be returned by the parent class */
sealed class MangaResult {
    class Error(val id: Int? = null, val text: String? = null) : MangaResult()

    object UpdatedManga : MangaResult()

    object UpdatedArtwork : MangaResult()

    object UpdatedChapters : MangaResult()

    class ChaptersRemoved(val chapterIdsRemoved: List<Long>) : MangaResult()

    object Success : MangaResult()
}
