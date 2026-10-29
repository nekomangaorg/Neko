package org.nekomanga.data.repository

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.isAvailable
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import org.nekomanga.constants.Constants
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.feed.FeedHistoryGroup
import org.nekomanga.domain.feed.FeedItem
import org.nekomanga.domain.feed.FeedPage
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedRepositoryImpl(
    private val chapterRepository: ChapterRepository = Injekt.get(),
    private val historyRepository: HistoryRepository = Injekt.get(),
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val chapterUseCases: ChapterUseCases = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) : FeedRepository {

    companion object {
        private val dayDateFormat = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
        private val weekDateFormat = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-w", Locale.getDefault()).apply {
                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7 + 1
                calendar.firstDayOfWeek = dayOfWeek
            }
        }
    }

    private inline fun <T> runCatchingCancelSafe(block: () -> T): Result<T, ResultError.Generic> {
        return try {
            Ok(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            TimberKt.e(e)
            Err(ResultError.Generic("Error : ${e.message}"))
        }
    }

    override suspend fun getUpdatedFeedItemForHistoryBySeries(feedItem: FeedItem): FeedItem {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        val chapterHistories = historyRepository.getChapterHistoryByMangaId(feedItem.mangaId)
        val simpleChapters = chapterHistories.mapNotNull { chpHistory ->
            val chapter = chpHistory.chapter
            if (blockedGroups.isNotEmpty() || blockedUploaders.isNotEmpty()) {
                if (
                    !chapterUseCases.validateChapterNotBlocked(
                        chapter.scanlatorList(),
                        chapter.uploader,
                        blockedGroups,
                        blockedUploaders,
                    )
                ) {
                    return@mapNotNull null
                }
            }

            chapter.toSimpleChapter(chpHistory.history.last_read)?.toChapterItem()
        }

        return feedItem.copy(chapters = simpleChapters)
    }

    override suspend fun getSummaryUpdatesList(): Result<List<FeedItem>, ResultError.Generic> {
        return runCatchingCancelSafe {
            suspend fun lookup(current: List<Long>, offset: Int, limit: Int): List<FeedItem> {
                val updatesPage =
                    getUpdatesPage(offset = offset, limit = limit, uploadsFetchSort = false).get()
                        ?: return emptyList()

                val groupedEntries =
                    updatesPage.items
                        .filterNot { current.contains(it.mangaId) }
                        .filter {
                            it.chapters.none { chapterItem ->
                                chapterItem.chapter.read || chapterItem.chapter.lastPageRead != 0
                            }
                        }
                        .groupBy { it.mangaId }
                        .entries
                        .toList()

                if (groupedEntries.isEmpty()) return emptyList()

                val mangaIds = groupedEntries.map { it.key }
                val mangasMap =
                    mangaRepository
                        .getMangaByIds(mangaIds)
                        .filter { it.id != null }
                        .associateBy { it.id!! }
                val chaptersMap =
                    chapterRepository.getChaptersForMangaIds(mangaIds).groupBy { it.manga_id }

                return groupedEntries.mapNotNull { entry ->
                    val mangaId = entry.key
                    val manga = mangasMap[mangaId] ?: return@mapNotNull null
                    val rawChapters = chaptersMap[mangaId] ?: emptyList()

                    val chapters = rawChapters.filter { it.isAvailable(downloadManager, manga) }

                    val recentUploadDate =
                        entry.value
                            .mapNotNull { it.chapters.firstOrNull() }
                            .maxOfOrNull { it.chapter.dateUpload }

                    val simpleChapterItems = chapters.mapNotNull { ch ->
                        ch.toSimpleChapter()?.toChapterItem()
                    }

                    val chapter =
                        ChapterItemSort().getNextUnreadChapter(manga, simpleChapterItems)?.chapter
                            ?: return@mapNotNull null

                    val itemMangaId = manga.id ?: return@mapNotNull null
                    val dateToUse =
                        recentUploadDate
                            ?: chapter.dateUpload.takeIf { it > 0L }
                            ?: System.currentTimeMillis()
                    FeedItem(
                        mangaId = itemMangaId,
                        mangaTitle = manga.displayTitle(),
                        date = dateToUse,
                        artwork = manga.toDisplayManga().currentArtwork,
                        chapters = listOf(getChapterItem(manga, chapter)),
                    )
                }
            }

            var offset = 0
            val limit = 100
            val results = emptyList<FeedItem>().toMutableList()
            while (results.size < 6 && offset < 2000) {
                results += lookup(results.map { it.mangaId }, offset, limit)
                offset += limit
            }
            results.take(6)
        }
    }

    override suspend fun getSummaryContinueReadingList():
        Result<List<FeedItem>, ResultError.Generic> {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        return runCatchingCancelSafe {
            suspend fun lookup(current: List<Long>, offset: Int, limit: Int): List<FeedItem> {
                val groupedEntries =
                    historyRepository
                        .getRecentMangaLimit(search = "", offset = offset, limit = limit)
                        .mapNotNull { history ->
                            if (current.contains(history.manga.id)) return@mapNotNull null
                            history.manga.id ?: return@mapNotNull null
                            history.chapter.id ?: return@mapNotNull null

                            val simpleChapter =
                                history.chapter.toSimpleChapter(history.history.last_read)
                                    ?: return@mapNotNull null
                            val chapter = getChapterItem(history.manga, simpleChapter)

                            val scanlators = chapter.chapter.scanlatorList()
                            if (
                                scanlators.any { scanlator -> scanlator in blockedGroups } ||
                                    (Constants.NO_GROUP in scanlators &&
                                        chapter.chapter.uploader in blockedUploaders)
                            ) {
                                return@mapNotNull null
                            }

                            FeedItem(
                                mangaId = history.manga.id!!,
                                mangaTitle = history.manga.displayTitle(),
                                date = history.history.last_read,
                                artwork = history.manga.toDisplayManga().currentArtwork,
                                chapters = listOf(chapter),
                            )
                        }
                        .groupBy { it.mangaId }
                        .entries
                        .toList()

                if (groupedEntries.isEmpty()) return emptyList()

                val processedEntries = groupedEntries.map { entry ->
                    val feedMangaFiltered =
                        entry.value.filter { feedManga ->
                            feedManga.chapters.isNotEmpty() &&
                                feedManga.chapters.none { it.chapter.read }
                        }
                    entry to feedMangaFiltered
                }

                val mangaIdsToFetch = processedEntries.mapNotNull {
                    if (it.second.isEmpty()) it.first.key else null
                }

                val mangasMap =
                    if (mangaIdsToFetch.isNotEmpty()) {
                        mangaRepository
                            .getMangaByIds(mangaIdsToFetch)
                            .filter { it.id != null }
                            .associateBy { it.id!! }
                    } else {
                        emptyMap()
                    }

                val chaptersMap =
                    if (mangaIdsToFetch.isNotEmpty()) {
                        chapterRepository.getChaptersForMangaIds(mangaIdsToFetch).groupBy {
                            it.manga_id
                        }
                    } else {
                        emptyMap()
                    }

                return processedEntries.mapNotNull { (entry, feedMangaFiltered) ->
                    if (feedMangaFiltered.isNotEmpty()) {
                        feedMangaFiltered.last()
                    } else {
                        val lastReadChapter =
                            entry.value.map { it.chapters }.flatten().firstOrNull()?.chapter?.name
                                ?: ""
                        val mangaId = entry.key
                        val manga = mangasMap[mangaId] ?: return@mapNotNull null
                        val rawChapters = chaptersMap[mangaId] ?: emptyList()

                        val chapters = rawChapters.filter { it.isAvailable(downloadManager, manga) }
                        val simpleChapterItems = chapters.mapNotNull { ch ->
                            ch.toSimpleChapter()?.toChapterItem()
                        }
                        val chapter =
                            ChapterItemSort()
                                .getNextUnreadChapter(manga, simpleChapterItems)
                                ?.chapter ?: return@mapNotNull null

                        val itemMangaId = manga.id ?: return@mapNotNull null
                        val dateToUse = entry.value.firstOrNull()?.date ?: 0L
                        FeedItem(
                            mangaId = itemMangaId,
                            mangaTitle = manga.displayTitle(),
                            date = dateToUse,
                            artwork = manga.toDisplayManga().currentArtwork,
                            lastReadChapter = lastReadChapter,
                            chapters = listOf(chapter.toChapterItem()),
                        )
                    }
                }
            }
            var offset = 0
            val limit = 50
            val results = emptyList<FeedItem>().toMutableList()
            while (results.size < 6 && offset < 2000) {
                results += lookup(results.map { it.mangaId }, offset, limit)
                offset += limit
            }
            results.take(6)
        }
    }

    override suspend fun getSummaryNewlyAddedList(): Result<List<FeedItem>, ResultError.Generic> {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        return runCatchingCancelSafe {
            val favoriteMangaList =
                mangaRepository
                    .getFavoriteMangaList()
                    .distinctBy { it.id }
                    .sortedByDescending { it.date_added }
                    .take(100)
            val favoriteMangaIds = favoriteMangaList.mapNotNull { it.id }
            val chaptersMap =
                chapterRepository.getChaptersForMangaIds(favoriteMangaIds).groupBy { it.manga_id }

            favoriteMangaList
                .mapNotNull { manga ->
                    val mangaId = manga.id ?: return@mapNotNull null
                    val rawChapters = chaptersMap[mangaId] ?: emptyList()
                    val chapters =
                        rawChapters
                            .filterNot {
                                it.scanlatorList().any { scanlator -> scanlator in blockedGroups }
                            }
                            .filterNot {
                                it.uploader in blockedUploaders &&
                                    Constants.NO_GROUP in it.scanlatorList()
                            }

                    if (chapters.any { it.read || it.last_page_read != 0 }) {
                        return@mapNotNull null
                    }

                    val simpleChapter =
                        chapters
                            .filter { it.isAvailable(downloadManager, manga) }
                            .maxByOrNull { it.smart_order }
                            ?.toSimpleChapter() ?: return@mapNotNull null

                    val displayManga = manga.toDisplayManga()
                    FeedItem(
                        mangaId = displayManga.mangaId,
                        mangaTitle = displayManga.getTitle(),
                        date = manga.date_added,
                        artwork = displayManga.currentArtwork,
                        chapters = listOf(getChapterItem(manga, simpleChapter)),
                    )
                }
                .take(6)
        }
    }

    override suspend fun getHistoryPage(
        searchQuery: String,
        offset: Int,
        limit: Int,
        group: FeedHistoryGroup,
        seenMangaIds: Set<Long>,
    ): Result<FeedPage, ResultError.Generic> {
        return runCatchingCancelSafe {
            val chapters =
                when (group) {
                    FeedHistoryGroup.Series -> {
                        val currentSeen = seenMangaIds.toMutableSet()
                        val recentManga =
                            historyRepository.getRecentMangaLimit(
                                search = searchQuery,
                                offset = offset,
                                limit = limit,
                            )
                        val mangaIds = recentManga.mapNotNull { it.manga.id }.distinct()
                        val allHistories = historyRepository.getChapterHistoryByMangaIds(mangaIds)
                        val historiesByMangaId = allHistories.groupBy { it.manga.id }

                        recentManga.mapNotNull { history ->
                            val mangaId = history.manga.id ?: return@mapNotNull null
                            if (history.chapter.id == null) return@mapNotNull null
                            if (currentSeen.contains(mangaId)) {
                                return@mapNotNull null
                            }

                            val chapterHistories =
                                (historiesByMangaId[mangaId] ?: emptyList()).take(25)
                            val chapterItems = chapterHistories.mapNotNull { chpHistory ->
                                val simpleChapter =
                                    chpHistory.chapter.toSimpleChapter(chpHistory.history.last_read)
                                        ?: return@mapNotNull null
                                getChapterItem(chpHistory.manga, simpleChapter)
                            }

                            currentSeen.add(mangaId)

                            FeedItem(
                                mangaId = mangaId,
                                mangaTitle = history.manga.displayTitle(),
                                date = history.history.last_read,
                                artwork = history.manga.toDisplayManga().currentArtwork,
                                chapters = chapterItems,
                            )
                        }
                    }
                    FeedHistoryGroup.Day,
                    FeedHistoryGroup.Week -> {
                        val formatter: SimpleDateFormat =
                            when (group == FeedHistoryGroup.Week) {
                                true -> weekDateFormat.get()
                                false -> dayDateFormat.get()
                            }
                        historyRepository
                            .getRecentHistoryUngrouped(
                                search = searchQuery,
                                offset = offset,
                                limit = limit,
                            )
                            .groupBy {
                                val date = it.history.last_read
                                it.manga to (if (date <= 0L) "-1" else formatter.format(Date(date)))
                            }
                            .mapNotNull { (manga, matches) ->
                                val chapterItems = matches.mapNotNull {
                                    val simpleChapter =
                                        it.chapter.toSimpleChapter(it.history.last_read)
                                            ?: return@mapNotNull null
                                    getChapterItem(it.manga, simpleChapter)
                                }
                                val mangaId = manga.first.id ?: return@mapNotNull null
                                FeedItem(
                                    mangaId = mangaId,
                                    mangaTitle = manga.first.displayTitle(),
                                    date = 0L,
                                    artwork = manga.first.toDisplayManga().currentArtwork,
                                    chapters = chapterItems,
                                )
                            }
                    }
                    else -> {
                        historyRepository
                            .getRecentHistoryUngrouped(
                                search = searchQuery,
                                offset = offset,
                                limit = limit,
                            )
                            .mapNotNull {
                                val mangaId = it.manga.id ?: return@mapNotNull null
                                if (it.chapter.id == null) return@mapNotNull null
                                val simpleChapter =
                                    it.chapter.toSimpleChapter(it.history.last_read)
                                        ?: return@mapNotNull null
                                val chapterItem = getChapterItem(it.manga, simpleChapter)
                                FeedItem(
                                    mangaId = mangaId,
                                    mangaTitle = it.manga.displayTitle(),
                                    date = it.history.last_read,
                                    artwork = it.manga.toDisplayManga().currentArtwork,
                                    chapters = listOf(chapterItem),
                                )
                            }
                    }
                }

            FeedPage(hasNextPage = chapters.isNotEmpty(), items = chapters)
        }
    }

    override suspend fun getUpdatesPage(
        searchQuery: String,
        offset: Int,
        limit: Int,
        uploadsFetchSort: Boolean,
    ): Result<FeedPage, ResultError.Generic> {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        return runCatchingCancelSafe {
            val chapters =
                chapterRepository
                    .getRecentChapters(
                        search = searchQuery,
                        offset = offset,
                        limit = limit,
                        sortByFetched = uploadsFetchSort,
                    )
                    .mapNotNull {
                        val simpleChapter = it.chapter.toSimpleChapter() ?: return@mapNotNull null
                        val chapterItem = getChapterItem(it.manga, simpleChapter)
                        val date =
                            when (uploadsFetchSort) {
                                true -> it.chapter.date_fetch
                                false -> it.chapter.date_upload
                            }

                        if (blockedGroups.isNotEmpty() || blockedUploaders.isNotEmpty()) {
                            if (
                                !chapterUseCases.validateChapterNotBlocked(
                                    chapterItem.chapter.scanlatorList(),
                                    chapterItem.chapter.uploader,
                                    blockedGroups,
                                    blockedUploaders,
                                )
                            ) {
                                return@mapNotNull null
                            }
                        }

                        FeedItem(
                            mangaId = chapterItem.chapter.mangaId,
                            mangaTitle = it.manga.displayTitle(),
                            date = date,
                            artwork = it.manga.toDisplayManga().currentArtwork,
                            chapters = listOf(chapterItem),
                        )
                    }
            FeedPage(hasNextPage = chapters.isNotEmpty(), items = chapters)
        }
    }

    override suspend fun deleteAllHistory() {
        historyRepository.deleteAllHistory()
    }

    override suspend fun deleteChapter(chapterItem: ChapterItem) {
        val manga = mangaRepository.getMangaById(chapterItem.chapter.mangaId) ?: return
        downloadManager.deleteChapters(manga, listOf(chapterItem.chapter.toDbChapter()))
    }

    override suspend fun deleteAllHistoryForManga(mangaId: Long) {
        val history = historyRepository.getHistoryByMangaId(mangaId)
        history.forEach {
            it.last_read = 0L
            it.time_read = 0L
        }
        historyRepository.upsertHistoryList(history)
    }

    override suspend fun deleteHistoryForChapter(chapterUrl: String) {
        val history = historyRepository.getHistoryByChapterUrl(chapterUrl)
        history ?: return
        history.last_read = 0L
        history.time_read = 0L
        historyRepository.upsertHistory(history)
    }

    override fun getChapterItem(manga: Manga, chapter: SimpleChapter): ChapterItem {
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

    override suspend fun toggleChapterRead(chapterItem: ChapterItem): ChapterItem {
        val markAction =
            when (!chapterItem.chapter.read) {
                true -> ChapterMarkActions.Read()
                false -> ChapterMarkActions.Unread()
            }

        return markChapter(chapterItem, markAction)
    }

    override suspend fun markChapter(
        chapterItem: ChapterItem,
        markAction: ChapterMarkActions,
    ): ChapterItem {
        chapterUseCases.markChapters(markAction, listOf(chapterItem))

        val manga = mangaRepository.getMangaById(chapterItem.chapter.mangaId)
        if (manga != null) {
            chapterUseCases.markChaptersRemote(markAction, manga.uuid(), listOf(chapterItem))
        }

        val simpleChapter =
            chapterRepository.getChapterById(chapterItem.chapter.id)?.toSimpleChapter()
        return if (simpleChapter != null) chapterItem.copy(chapter = simpleChapter) else chapterItem
    }

    override suspend fun downloadChapter(
        feedItem: FeedItem,
        chapterItem: ChapterItem,
        downloadAction: MangaConstants.DownloadAction,
    ) {
        val dbManga = mangaRepository.getMangaById(feedItem.mangaId) ?: return
        val dbChapter = chapterItem.chapter.toDbChapter()

        when (downloadAction) {
            is MangaConstants.DownloadAction.ImmediateDownload ->
                downloadManager.startDownloadNow(dbChapter)
            is MangaConstants.DownloadAction.Download ->
                downloadManager.downloadChapters(dbManga, listOf(dbChapter))
            is MangaConstants.DownloadAction.Remove ->
                downloadManager.deleteChapters(dbManga, listOf(dbChapter))
            is MangaConstants.DownloadAction.Cancel ->
                downloadManager.deleteChapters(dbManga, listOf(dbChapter))
            else -> Unit
        }
    }

    override suspend fun getRecentlyReadManga(): List<Manga> {
        val page = getHistoryPage(offset = 0, limit = 25, group = FeedHistoryGroup.Series)
        val feedItemList = page.get()?.items ?: return emptyList()
        val mangaIds = feedItemList.map { it.mangaId }
        val mangas = mangaRepository.getMangaByIds(mangaIds)
        val mangasMap = mangas.filter { it.id != null }.associateBy { it.id!! }
        return mangaIds.mapNotNull { mangasMap[it] }
    }
}
