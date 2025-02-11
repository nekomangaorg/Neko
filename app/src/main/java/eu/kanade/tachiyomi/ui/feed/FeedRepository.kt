package eu.kanade.tachiyomi.ui.feed

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.toDisplayManga
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedRepository(
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val chapterUseCases: ChapterUseCases = Injekt.get(),
) {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val bySeriesSet = mutableSetOf<Long>()

    suspend fun getUpdatedFeedMangaForHistoryBySeries(feedManga: FeedManga): FeedManga {
        val chapterHistories = db.getChapterHistoryByMangaId(feedManga.mangaId).executeOnIO()
        val simpleChapters =
            chapterHistories
                .mapNotNull { chpHistory ->
                    chpHistory.chapter
                        .toSimpleChapter(chpHistory.history.last_read)!!
                        .toChapterItem()
                }
                .toPersistentList()

        return feedManga.copy(chapters = simpleChapters)
    }

    suspend fun getSummaryUpdatesList(): Result<List<FeedManga>, ResultError.Generic> {
        return com.github.michaelbull.result
            .runCatching {
                getUpdatesPage(offset = 0, limit = 200, uploadsFetchSort = false)
                    .get()!!
                    .second
                    .filter { it.chapters.none { it.chapter.read || it.chapter.lastPageRead != 0 } }
                    .groupBy { it.mangaId }
                    .entries
                    .map { it.value.last() }
                    .take(6)
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun getSummaryContinueReadingList(): Result<List<FeedManga>, ResultError.Generic> {
        return com.github.michaelbull.result
            .runCatching {
                db.getRecentMangaLimit(offset = 0, limit = 50, isResuming = false)
                    .executeOnIO()
                    .mapNotNull { history ->
                        history.manga.id ?: return@mapNotNull null
                        history.chapter.id ?: return@mapNotNull null

                        val chapter =
                            getChapterItem(
                                history.manga,
                                history.chapter.toSimpleChapter(history.history.last_read)!!,
                            )

                        FeedManga(
                            mangaId = history.manga.id!!,
                            mangaTitle = history.manga.title,
                            date = history.history.last_read,
                            artwork = history.manga.toDisplayManga().currentArtwork,
                            chapters = persistentListOf(chapter),
                        )
                    }
                    .groupBy { it.mangaId }
                    .entries
                    .mapNotNull { entry ->
                        val feedMangaFiltered =
                            entry.value.mapNotNull { feedManga ->
                                if (
                                    feedManga.chapters.isNotEmpty() &&
                                        feedManga.chapters.none { it.chapter.read }
                                ) {
                                    feedManga
                                } else {
                                    null
                                }
                            }
                        if (feedMangaFiltered.isNotEmpty()) {
                            feedMangaFiltered.last()
                        } else {
                            val lastReadChapter =
                                entry.value
                                    .map { it.chapters }
                                    .flatten()
                                    .firstOrNull()
                                    ?.chapter
                                    ?.name ?: ""
                            val manga = db.getManga(entry.key).executeOnIO()!!
                            val chapters = db.getChapters(manga).executeOnIO()
                            val chapter = ChapterSort(manga).getNextUnreadChapter(chapters)
                            if (chapter == null) {
                                return@mapNotNull null
                            } else {
                                FeedManga(
                                    mangaId = manga.id!!,
                                    mangaTitle = manga.title,
                                    date = 0L,
                                    artwork = manga.toDisplayManga().currentArtwork,
                                    lastReadChapter = lastReadChapter,
                                    chapters =
                                        persistentListOf(
                                            chapter.toSimpleChapter()!!.toChapterItem()
                                        ),
                                )
                            }
                        }
                    }
                    .take(6)
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun getSummaryNewlyAddedList(): Result<List<FeedManga>, ResultError.Generic> {
        return com.github.michaelbull.result
            .runCatching {
                db.getFavoriteMangaList()
                    .executeOnIO()
                    .distinctBy { it.id }
                    .sortedBy { it.date_added }
                    .takeLast(100)
                    .mapNotNull { manga ->
                        val chapters = db.getChapters(manga).executeOnIO()
                        if (chapters.any { it.read || it.last_page_read != 0 }) {
                            return@mapNotNull null
                        }
                        val simpleChapter =
                            chapters.sortedBy { it.source_order }.lastOrNull()?.toSimpleChapter()

                        simpleChapter ?: return@mapNotNull null

                        val displayManga = manga.toDisplayManga()
                        FeedManga(
                            mangaId = displayManga.mangaId,
                            mangaTitle = displayManga.title,
                            date = manga.date_added,
                            artwork = displayManga.currentArtwork,
                            chapters = persistentListOf(getChapterItem(manga, simpleChapter)),
                        )
                    }
                    .takeLast(6)
                    .reversed()
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun getNewlyAddedPage(): Result<List<FeedManga>, ResultError.Generic> {
        return com.github.michaelbull.result
            .runCatching {
                db.getFavoriteMangaList()
                    .executeOnIO()
                    .distinctBy { it.id }
                    .sortedBy { it.date_added }
                    .takeLast(100)
                    .mapNotNull { manga ->
                        val chapters = db.getChapters(manga).executeOnIO()
                        if (chapters.any { it.read }) {
                            return@mapNotNull null
                        }
                        val simpleChapter =
                            chapters.sortedBy { it.source_order }.lastOrNull()?.toSimpleChapter()

                        simpleChapter ?: return@mapNotNull null

                        val displayManga = manga.toDisplayManga()
                        FeedManga(
                            mangaId = displayManga.mangaId,
                            mangaTitle = displayManga.title,
                            date = manga.date_added,
                            artwork = displayManga.currentArtwork,
                            chapters = persistentListOf(getChapterItem(manga, simpleChapter)),
                        )
                    }
                    .takeLast(6)
                    .reversed()
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun getHistoryPage(
        searchQuery: String = "",
        offset: Int,
        limit: Int = FeedPresenter.HISTORY_ENDLESS_LIMIT,
        group: FeedHistoryGroup,
    ): Result<Pair<Boolean, List<FeedManga>>, ResultError.Generic> {
        if (offset > 0) {
            delay(300L)
        }
        return com.github.michaelbull.result
            .runCatching {
                val chapters =
                    when (group) {
                        FeedHistoryGroup.Series -> {
                            if (offset == 0) {
                                bySeriesSet.clear()
                            }
                            db.getRecentMangaLimit(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                    isResuming = false,
                                )
                                .executeOnIO()
                                .mapNotNull { history ->
                                    history.manga.id ?: return@mapNotNull null
                                    history.chapter.id ?: return@mapNotNull null
                                    if (bySeriesSet.contains(history.manga.id)) {
                                        return@mapNotNull null
                                    }

                                    val chapterHistories =
                                        db.getChapterHistoryByMangaId(history.manga.id!!)
                                            .executeOnIO()
                                    val chapterItems =
                                        chapterHistories
                                            .mapNotNull { chpHistory ->
                                                getChapterItem(
                                                    chpHistory.manga,
                                                    chpHistory.chapter.toSimpleChapter(
                                                        chpHistory.history.last_read
                                                    )!!,
                                                )
                                            }
                                            .toPersistentList()

                                    bySeriesSet.add(history.manga.id!!)

                                    FeedManga(
                                        mangaId = history.manga.id!!,
                                        mangaTitle = history.manga.title,
                                        date = history.history.last_read,
                                        artwork = history.manga.toDisplayManga().currentArtwork,
                                        chapters = chapterItems,
                                    )
                                }
                        }
                        FeedHistoryGroup.Day,
                        FeedHistoryGroup.Week -> {
                            val pattern =
                                when (group == FeedHistoryGroup.Week) {
                                    true -> "yyyy-w"
                                    false -> "yyyy-MM-dd"
                                }
                            val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
                            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7 + 1
                            dateFormat.calendar.firstDayOfWeek = dayOfWeek
                            db.getHistoryUngrouped(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                    isResuming = false,
                                )
                                .executeOnIO()
                                .groupBy {
                                    val date = it.history.last_read
                                    it.manga to
                                        (if (date <= 0L) "-1" else dateFormat.format(Date(date)))
                                }
                                .mapNotNull { (manga, matches) ->
                                    val chapterItems =
                                        matches
                                            .map {
                                                getChapterItem(
                                                    it.manga,
                                                    it.chapter.toSimpleChapter(
                                                        it.history.last_read
                                                    )!!,
                                                )
                                            }
                                            .toPersistentList()
                                    FeedManga(
                                        mangaId = manga.first.id!!,
                                        mangaTitle = manga.first.title,
                                        date = 0L,
                                        artwork = manga.first.toDisplayManga().currentArtwork,
                                        chapters = chapterItems,
                                    )
                                }
                        }
                        else -> {
                            db.getHistoryUngrouped(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                    isResuming = false,
                                )
                                .executeOnIO()
                                .mapNotNull {
                                    it.manga.id ?: return@mapNotNull null
                                    it.chapter.id ?: return@mapNotNull null
                                    val chapterItem =
                                        getChapterItem(
                                            it.manga,
                                            it.chapter.toSimpleChapter(it.history.last_read)!!,
                                        )
                                    it.history.last_read
                                    FeedManga(
                                        mangaId = it.manga.id!!,
                                        mangaTitle = it.manga.title,
                                        date = it.history.last_read,
                                        artwork = it.manga.toDisplayManga().currentArtwork,
                                        chapters = persistentListOf(chapterItem),
                                    )
                                }
                        }
                    }

                Pair(chapters.isNotEmpty(), chapters)
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun getUpdatesPage(
        searchQuery: String = "",
        offset: Int,
        limit: Int,
        uploadsFetchSort: Boolean,
    ): Result<Pair<Boolean, List<FeedManga>>, ResultError.Generic> {

        if (offset > 0) {
            delay(300L)
        }
        return com.github.michaelbull.result
            .runCatching {
                val chapters =
                    db.getRecentChapters(
                            search = searchQuery,
                            offset = offset,
                            limit = limit,
                            sortByFetched = uploadsFetchSort,
                        )
                        .executeAsBlocking()
                        .mapNotNull {
                            val chapterItem =
                                getChapterItem(it.manga, it.chapter.toSimpleChapter()!!)
                            val date =
                                when (uploadsFetchSort) {
                                    true -> it.chapter.date_fetch
                                    false -> it.chapter.date_upload
                                }

                            FeedManga(
                                mangaId = chapterItem.chapter.mangaId,
                                mangaTitle = it.manga.title,
                                date = date,
                                artwork = it.manga.toDisplayManga().currentArtwork,
                                chapters = persistentListOf(chapterItem),
                            )
                        }
                Pair(chapters.isNotEmpty(), chapters)
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun deleteAllHistory() {
        db.deleteHistory().executeAsBlocking()
    }

    suspend fun deleteChapter(chapterItem: ChapterItem) {
        val manga = db.getManga(chapterItem.chapter.mangaId).executeOnIO()!!
        downloadManager.deleteChapters(listOf(chapterItem.chapter.toDbChapter()), manga)
    }

    suspend fun deleteAllHistoryForManga(mangaId: Long) {
        val history = db.getHistoryByMangaId(mangaId).executeAsBlocking()
        history.forEach {
            it.last_read = 0L
            it.time_read = 0L
        }
        db.upsertHistoryLastRead(history).executeAsBlocking()
    }

    suspend fun deleteHistoryForChapter(chapterUrl: String) {
        val history = db.getHistoryByChapterUrl(chapterUrl).executeAsBlocking()
        history ?: return
        history.last_read = 0L
        history.time_read = 0L
        db.upsertHistoryLastRead(history).executeAsBlocking()
    }

    fun getChapterItem(manga: Manga, chapter: SimpleChapter): ChapterItem {
        val downloadState =
            when {
                downloadManager.isChapterDownloaded(chapter.toDbChapter(), manga) ->
                    Download.State.DOWNLOADED
                else ->
                    downloadManager.getQueuedDownloadOrNull(chapter.id)?.status
                        ?: Download.State.NOT_DOWNLOADED
            }

        return ChapterItem(
            chapter = chapter,
            downloadState = downloadState,
            downloadProgress =
                when (downloadState == Download.State.DOWNLOADING) {
                    true -> downloadManager.getQueuedDownloadOrNull(chapter.id)?.progress ?: 0
                    false -> 0
                },
        )
    }

    /** this toggle the chapter read and returns the new chapter item */
    suspend fun toggleChapterRead(chapterItem: ChapterItem): ChapterItem {
        val markAction =
            when (!chapterItem.chapter.read) {
                true -> ChapterMarkActions.Read()
                false -> ChapterMarkActions.Unread()
            }

        chapterUseCases.markChapters(markAction, listOf(chapterItem))

        val manga = db.getManga(chapterItem.chapter.mangaId).executeOnIO()!!

        chapterUseCases.markChaptersRemote(markAction, manga.uuid(), listOf(chapterItem))

        val simpleChapter =
            db.getChapter(chapterItem.chapter.id).executeOnIO()!!.toSimpleChapter()!!
        return chapterItem.copy(chapter = simpleChapter)
    }

    suspend fun downloadChapter(
        feedManga: FeedManga,
        chapterItem: ChapterItem,
        downloadAction: MangaConstants.DownloadAction,
    ) {
        val dbManga = db.getManga(feedManga.mangaId).executeOnIO()!!
        val dbChapter = chapterItem.chapter.toDbChapter()

        when (downloadAction) {
            is MangaConstants.DownloadAction.ImmediateDownload ->
                downloadManager.startDownloadNow(dbChapter)
            is MangaConstants.DownloadAction.Download ->
                downloadManager.downloadChapters(dbManga, listOf(dbChapter))
            is MangaConstants.DownloadAction.Remove ->
                downloadManager.deleteChapters(listOf(dbChapter), dbManga)
            is MangaConstants.DownloadAction.Cancel ->
                downloadManager.deleteChapters(listOf(dbChapter), dbManga)
            else -> Unit
        }
    }

    companion object {
        suspend fun getRecentlyReadManga(): List<Manga> {
            val feedRepository = FeedRepository()
            val page = feedRepository.getHistoryPage(offset = 0, group = FeedHistoryGroup.Series)
            return page.get()?.second?.mapNotNull { feedManga ->
                feedRepository.db.getManga(feedManga.mangaId).executeAsBlocking()
            } ?: emptyList()
        }
    }
}
