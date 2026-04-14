package eu.kanade.tachiyomi.ui.feed

import androidx.compose.ui.util.fastAny
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.nekomanga.constants.Constants
import org.nekomanga.data.database.entity.isAvailable
import org.nekomanga.data.database.entity.scanlatorList
import org.nekomanga.data.database.model.toChapter
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.model.toSimpleChapter
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedRepository(
    private val mangaRepository: MangaRepositoryImpl = Injekt.get(),
    private val chapterRepository: ChapterRepositoryImpl = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val chapterUseCases: ChapterUseCases = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val bySeriesSet = mutableSetOf<Long>()

    suspend fun getUpdatedFeedMangaForHistoryBySeries(feedManga: FeedManga): FeedManga {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        val chapterHistories =
            chapterRepository.getChapterHistoryByMangaId(feedManga.mangaId).first()
        val simpleChapters =
            chapterHistories
                .mapNotNull { chpHistory ->
                    val chapter = chpHistory.toChapter()
                    if (blockedGroups.isNotEmpty() || blockedUploaders.isNotEmpty()) {
                        val scanlators = chapter.scanlatorList()
                        if (
                            scanlators.fastAny { scanlator -> scanlator in blockedGroups } ||
                                (chapter.uploader in blockedUploaders &&
                                    Constants.NO_GROUP in scanlators)
                        ) {
                            return@mapNotNull null
                        }
                    }

                    chpHistory.toSimpleChapter().toChapterItem()
                }
                .toPersistentList()

        return feedManga.copy(chapters = simpleChapters)
    }

    suspend fun getSummaryUpdatesList(): Result<List<FeedManga>, ResultError.Generic> {
        return com.github.michaelbull.result
            .runCatching {
                suspend fun lookup(current: List<Long>, offset: Int, limit: Int): List<FeedManga> {
                    val groupedEntries =
                        getUpdatesPage(offset = offset, limit = limit, uploadsFetchSort = false)
                            .get()!!
                            .second
                            .filterNot { current.contains(it.mangaId) }
                            .filter {
                                it.chapters.none { chapterItem ->
                                    chapterItem.chapter.read ||
                                        chapterItem.chapter.lastPageRead != 0
                                }
                            }
                            .groupBy { it.mangaId }
                            .entries
                            .toList()

                    if (groupedEntries.isEmpty()) return emptyList()

                    val mangaIds = groupedEntries.map { it.key }
                    val mangasMap = mangaRepository.getMangas(mangaIds).associateBy { it.id!! }
                    val chaptersMap =
                        chapterRepository.getChaptersForMangas(mangaIds).groupBy { it.mangaId }

                    return groupedEntries.mapNotNull { entry ->
                        val mangaId = entry.key
                        val mangaEntity = mangasMap[mangaId] ?: return@mapNotNull null
                        val manga = mangaEntity.toManga()
                        val rawChapters = chaptersMap[mangaId] ?: emptyList()

                        val chapters = rawChapters.filter { it.isAvailable(downloadManager, manga) }

                        val recentUploadDate =
                            entry.value
                                .mapNotNull { it.chapters.firstOrNull() }
                                .maxOfOrNull { it.chapter.dateUpload }

                        val displayManga = manga.toDisplayManga()

                        val chapter =
                            ChapterItemSort()
                                .getNextUnreadChapter(
                                    manga,
                                    chapters.map { it.toSimpleChapter().toChapterItem() },
                                )
                                ?.chapter ?: return@mapNotNull null

                        FeedManga(
                            mangaId = displayManga.mangaId,
                            mangaTitle = displayManga.getTitle(),
                            date = recentUploadDate ?: 0L,
                            artwork = displayManga.currentArtwork,
                            chapters = persistentListOf(getChapterItem(manga, chapter)),
                        )
                    }
                }

                var offset = 0
                val limit = 100
                val results = emptyList<FeedManga>().toMutableList()
                while (results.size < 6 && offset < 2000) {
                    results += lookup(results.map { it.mangaId }, offset, limit)
                    offset += limit
                }
                results.take(6)
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun getSummaryContinueReadingList(): Result<List<FeedManga>, ResultError.Generic> {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        return com.github.michaelbull.result
            .runCatching {
                suspend fun lookup(current: List<Long>, offset: Int, limit: Int): List<FeedManga> {
                    val groupedEntries =
                        chapterRepository
                            .getRecentMangaLimit("", limit, offset)
                            .first()
                            .filterNot { current.contains(it.manga.id) }
                            .mapNotNull { history ->
                                history.manga.id ?: return@mapNotNull null
                                history.chapter.id ?: return@mapNotNull null

                                val chapter =
                                    getChapterItem(history.toManga(), history.toSimpleChapter())

                                val scanlators = chapter.chapter.scanlatorList()
                                if (
                                    scanlators.fastAny { scanlator ->
                                        scanlator in blockedGroups
                                    } ||
                                        (Constants.NO_GROUP in scanlators &&
                                            chapter.chapter.uploader in blockedUploaders)
                                ) {
                                    return@mapNotNull null
                                }

                                val displayManga = history.toManga().toDisplayManga()

                                FeedManga(
                                    mangaId = displayManga.mangaId,
                                    mangaTitle = displayManga.getTitle(),
                                    date = history.history.lastRead,
                                    artwork = displayManga.currentArtwork,
                                    chapters = persistentListOf(chapter),
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
                            mangaRepository.getMangas(mangaIdsToFetch).associateBy { it.id!! }
                        } else {
                            emptyMap()
                        }

                    val chaptersMap =
                        if (mangaIdsToFetch.isNotEmpty()) {
                            chapterRepository.getChaptersForMangas(mangaIdsToFetch).groupBy {
                                it.mangaId
                            }
                        } else {
                            emptyMap()
                        }

                    return processedEntries.mapNotNull { (entry, feedMangaFiltered) ->
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
                            val mangaId = entry.key
                            val mangaEntity = mangasMap[mangaId] ?: return@mapNotNull null
                            val manga = mangaEntity.toManga()
                            val rawChapters = chaptersMap[mangaId] ?: emptyList()

                            val chapters = rawChapters.filter {
                                it.isAvailable(downloadManager, manga)
                            }
                            val chapter =
                                ChapterItemSort()
                                    .getNextUnreadChapter(
                                        manga,
                                        chapters.map { it.toSimpleChapter().toChapterItem() },
                                    )
                                    ?.chapter ?: return@mapNotNull null

                            val displayManga = manga.toDisplayManga()

                            FeedManga(
                                mangaId = displayManga.mangaId,
                                mangaTitle = displayManga.getTitle(),
                                date = 0L,
                                artwork = displayManga.currentArtwork,
                                lastReadChapter = lastReadChapter,
                                chapters = persistentListOf(chapter.toChapterItem()),
                            )
                        }
                    }
                }
                var offset = 0
                val limit = 50
                val results = emptyList<FeedManga>().toMutableList()
                while (results.size < 6 && offset < 2000) {
                    results += lookup(results.map { it.mangaId }, offset, limit)
                    offset += limit
                }
                results.take(6)
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun getSummaryNewlyAddedList(): Result<List<FeedManga>, ResultError.Generic> {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        return com.github.michaelbull.result
            .runCatching {
                mangaRepository
                    .getFavoriteMangaList()
                    .first()
                    .distinctBy { it.id }
                    .sortedBy { it.dateAdded }
                    .takeLast(100)
                    .mapNotNull { mangaEntity ->
                        val manga = mangaEntity.toManga()
                        val chapters =
                            chapterRepository
                                .getChaptersForManga(manga.id!!)
                                .first()
                                .filterNot {
                                    it.scanlatorList().fastAny { scanlator ->
                                        scanlator in blockedGroups
                                    }
                                }
                                .filterNot {
                                    it.uploader in blockedUploaders &&
                                        Constants.NO_GROUP in it.scanlatorList()
                                }

                        if (chapters.any { it.read || it.lastPageRead != 0 }) {
                            return@mapNotNull null
                        }

                        val simpleChapter =
                            chapters
                                .filter { it.isAvailable(downloadManager, manga) }
                                .maxByOrNull { it.smartOrder }
                                ?.toSimpleChapter() ?: return@mapNotNull null

                        val displayManga = manga.toDisplayManga()
                        FeedManga(
                            mangaId = displayManga.mangaId,
                            mangaTitle = displayManga.getTitle(),
                            date = mangaEntity.dateAdded,
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
        limit: Int = FeedViewModel.HISTORY_ENDLESS_LIMIT,
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
                            chapterRepository
                                .getRecentMangaLimit(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                )
                                .first()
                                .mapNotNull { history ->
                                    history.manga.id ?: return@mapNotNull null
                                    history.chapter.id ?: return@mapNotNull null
                                    if (bySeriesSet.contains(history.manga.id)) {
                                        return@mapNotNull null
                                    }

                                    val chapterHistories =
                                        chapterRepository
                                            .getChapterHistoryByMangaId(history.manga.id!!)
                                            .first()
                                    val chapterItems =
                                        chapterHistories
                                            .mapNotNull { chpHistory ->
                                                getChapterItem(
                                                    chpHistory.toManga(),
                                                    chpHistory.toSimpleChapter(),
                                                )
                                            }
                                            .toPersistentList()

                                    bySeriesSet.add(history.manga.id!!)

                                    val displayManga = history.toManga().toDisplayManga()

                                    FeedManga(
                                        mangaId = displayManga.mangaId,
                                        mangaTitle = displayManga.getTitle(),
                                        date = history.history.lastRead,
                                        artwork = displayManga.currentArtwork,
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
                            chapterRepository
                                .getRecentHistoryUngrouped(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                )
                                .first()
                                .groupBy {
                                    val date = it.history.lastRead
                                    it.manga.id to
                                        (if (date <= 0L) "-1" else dateFormat.format(Date(date)))
                                }
                                .mapNotNull { (_, matches) ->
                                    val firstMatch = matches.first()
                                    val chapterItems =
                                        matches
                                            .map {
                                                getChapterItem(it.toManga(), it.toSimpleChapter())
                                            }
                                            .toPersistentList()

                                    val displayManga = firstMatch.toManga().toDisplayManga()

                                    FeedManga(
                                        mangaId = displayManga.mangaId,
                                        mangaTitle = displayManga.getTitle(),
                                        date = 0L,
                                        artwork = displayManga.currentArtwork,
                                        chapters = chapterItems,
                                    )
                                }
                        }
                        else -> {
                            chapterRepository
                                .getRecentHistoryUngrouped(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                )
                                .first()
                                .mapNotNull {
                                    it.manga.id ?: return@mapNotNull null
                                    it.chapter.id ?: return@mapNotNull null
                                    val chapterItem =
                                        getChapterItem(it.toManga(), it.toSimpleChapter())

                                    val displayManga = it.toManga().toDisplayManga()

                                    FeedManga(
                                        mangaId = displayManga.mangaId,
                                        mangaTitle = displayManga.getTitle(),
                                        date = it.history.lastRead,
                                        artwork = displayManga.currentArtwork,
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

        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        if (offset > 0) {
            delay(300L)
        }
        return com.github.michaelbull.result
            .runCatching {
                val chapters =
                    chapterRepository
                        .getRecentChapters(
                            search = searchQuery,
                            offset = offset,
                            limit = limit,
                            sortByFetched = uploadsFetchSort,
                        )
                        .first()
                        .mapNotNull {
                            val chapterItem = getChapterItem(it.toManga(), it.toSimpleChapter())
                            val date =
                                when (uploadsFetchSort) {
                                    true -> it.chapter.dateFetch
                                    false -> it.chapter.dateUpload
                                }

                            val scanlators = chapterItem.chapter.scanlatorList()
                            if (
                                scanlators.fastAny { scanlator -> scanlator in blockedGroups } ||
                                    (Constants.NO_GROUP in scanlators &&
                                        chapterItem.chapter.uploader in blockedUploaders)
                            ) {
                                return@mapNotNull null
                            }

                            val displayManga = it.toManga().toDisplayManga()

                            FeedManga(
                                mangaId = displayManga.mangaId,
                                mangaTitle = displayManga.getTitle(),
                                date = date,
                                artwork = displayManga.currentArtwork,
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
        chapterRepository.deleteAllHistory()
    }

    suspend fun deleteChapter(chapterItem: ChapterItem) {
        val mangaEntity = mangaRepository.getMangaById(chapterItem.chapter.mangaId)!!
        downloadManager.deleteChapters(
            mangaEntity.toManga(),
            listOf(chapterItem.chapter.toDbChapter()),
        )
    }

    suspend fun deleteAllHistoryForManga(mangaId: Long) {
        val history = chapterRepository.getHistoryByMangaId(mangaId)
        history.forEach { chapterRepository.upsertHistory(it.copy(lastRead = 0L, timeRead = 0L)) }
    }

    suspend fun deleteHistoryForChapter(chapterUrl: String) {
        val history = chapterRepository.getHistoryByChapterUrl(chapterUrl)
        history ?: return
        chapterRepository.upsertHistory(history.copy(lastRead = 0L, timeRead = 0L))
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

        return markChapter(chapterItem, markAction)
    }

    /** this toggle the chapter read and returns the new chapter item */
    suspend fun markChapter(chapterItem: ChapterItem, markAction: ChapterMarkActions): ChapterItem {
        chapterUseCases.markChapters(markAction, listOf(chapterItem))

        val manga = mangaRepository.getMangaById(chapterItem.chapter.mangaId)!!.toManga()

        chapterUseCases.markChaptersRemote(markAction, manga.uuid(), listOf(chapterItem))

        val simpleChapter =
            chapterRepository.getChapterById(chapterItem.chapter.id)!!.toSimpleChapter()
        return chapterItem.copy(chapter = simpleChapter)
    }

    suspend fun downloadChapter(
        feedManga: FeedManga,
        chapterItem: ChapterItem,
        downloadAction: MangaConstants.DownloadAction,
    ) {
        val dbManga = mangaRepository.getMangaById(feedManga.mangaId)!!.toManga()
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

    companion object {
        suspend fun getRecentlyReadManga(): List<Manga> {
            val feedRepository = FeedRepository()
            val page = feedRepository.getHistoryPage(offset = 0, group = FeedHistoryGroup.Series)
            return page.get()?.second?.mapNotNull { feedManga ->
                feedRepository.mangaRepository.getMangaById(feedManga.mangaId)?.toManga()
            } ?: emptyList()
        }
    }
}
