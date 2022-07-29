package eu.kanade.tachiyomi.ui.manga

import androidx.core.text.isDigitsOnly
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MergeSource
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.shouldDownloadNewChapters
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaDetailsRepository {
    private val db: DatabaseHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()
    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by lazy { Injekt.get() }
    private val mangaDex: MangaDex by lazy { sourceManager.getMangadex() }
    private val mergedSource: MergeSource by lazy { sourceManager.getMergeSource() }
    private val downloadManager: DownloadManager by injectLazy()
    private val mangaShortcutManager: MangaShortcutManager by injectLazy()

    suspend fun update(manga: Manga, isOnline: Boolean, scope: CoroutineScope) = channelFlow {
        var errorFromNetwork: Throwable? = null
        var errorFromMerged: Throwable? = null

        val mangaWasInitialized = manga.initialized


        if (!isOnline) {
            send(MangaResult.Error(R.string.no_network_connection))
            return@channelFlow
        }
        if (!mangaDex.checkIfUp()) {
            send(MangaResult.Error(R.string.site_down))
            return@channelFlow
        }

        val mangaUUID = MdUtil.getMangaId(manga.url)

        if (mangaUUID.isDigitsOnly()) {
            send(MangaResult.Error(R.string.v3_manga))
            return@channelFlow
        }

        XLog.d("Begin processing manga/chapter update for manga $mangaUUID")

        val mangaJob = startMangaJob(scope, manga)

        if (mangaJob.isCompleted || mangaJob.isActive) {
            val chapterJob = startChapterJob(scope, manga, mangaWasInitialized)
            mangaJob.join()
            chapterJob.join()

            if (mangaJob.isCompleted && chapterJob.isCompleted) {
                send(MangaResult.Success)
            }
        }

    }.flowOn(Dispatchers.IO)

    private fun ProducerScope<MangaResult>.startMangaJob(scope: CoroutineScope, manga: Manga): Job {
        return scope.launchIO {
            runCatching {
                withIOContext {
                    val artwork = mangaDex.getArtwork(manga.id!!, MdUtil.getMangaId(manga.url))
                    db.deleteArtworkForManga(manga).executeOnIO()
                    db.insertArtWorkList(artwork).executeOnIO()
                    send(MangaResult.UpdatedArtwork)

                }
                mangaDex.getMangaDetails(manga)
            }.onFailure { e ->
                XLog.e("error with mangadex getting manga", e)
                send(MangaResult.Error(text = "Error getting manga from MangaDex"))
                cancel()
            }.onSuccess { resultingManga ->
                val originalThumbnail = manga.thumbnail_url

                resultingManga.let { networkManga ->
                    manga.copyFrom(networkManga)
                    manga.initialized = true
                    if (networkManga.thumbnail_url != null && networkManga.thumbnail_url != originalThumbnail) {
                        coverCache.deleteFromCache(originalThumbnail, manga.favorite)
                    }
                    db.insertManga(manga).executeOnIO()
                    send(MangaResult.UpdatedManga)
                }
            }
        }
    }

    private fun ProducerScope<MangaResult>.startChapterJob(scope: CoroutineScope, manga: Manga, mangaWasAlreadyInitialized: Boolean): Job {
        return scope.launchIO {

            val deferredChapters = async {
                runCatching {
                    mangaDex.fetchChapterList(manga)
                }.onFailure { e ->
                    XLog.e("error with mangadex getting chapters", e)
                    send(MangaResult.Error(text = "error with MangaDex: getting chapters "))
                    this.cancel()
                }.getOrNull()!!
            }

            val deferredMergedChapters =
                async {
                    if (manga.isMerged()) {
                        kotlin.runCatching {
                            mergedSource.fetchChapters(manga.merge_manga_url!!)
                        }.onFailure { e ->
                            XLog.e("error with mergedsource", e)
                            send(MangaResult.Error(text = "error with merged source: getting chapters "))
                            this.cancel()
                        }.getOrNull()!!
                    } else {
                        emptyList()
                    }
                }

            val allChapters = deferredChapters.await() + deferredMergedChapters.await()

            val newChapters = syncChaptersWithSource(db, allChapters, manga)
            //chapters that were added
            if (newChapters.first.isNotEmpty()) {
                val downloadNew = preferences.downloadNewChapters().get()
                if (downloadNew && mangaWasAlreadyInitialized) {
                    if (manga.shouldDownloadNewChapters(db, preferences)) {
                        downloadChapters(manga, newChapters.first.mapNotNull { it.toSimpleChapter()?.toChapterItem() }.sortedBy { it.chapter.chapterNumber })
                    }
                }
                mangaShortcutManager.updateShortcuts()
            }
            //chapters that were removed
            if (newChapters.second.isNotEmpty()) {
                val removedChaptersId = newChapters.second.mapNotNull { it.id }
                send(MangaResult.ChaptersRemoved(removedChaptersId))
            }

            send(MangaResult.UpdatedChapters)
        }
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param chapters the list of chapters to download.
     */
    fun downloadChapters(manga: Manga, chapters: List<ChapterItem>) {
        downloadManager.downloadChapters(manga, chapters.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() })
    }
}

sealed class MangaResult {
    class Error(val id: Int? = null, val text: String = "") : MangaResult()
    object UpdatedManga : MangaResult()
    object UpdatedArtwork : MangaResult()
    object UpdatedChapters : MangaResult()
    class ChaptersRemoved(val chapterIdsRemoved: List<Long>) : MangaResult()
    object Success : MangaResult()
}

