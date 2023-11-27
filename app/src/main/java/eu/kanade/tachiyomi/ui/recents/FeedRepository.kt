package eu.kanade.tachiyomi.ui.recents

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.toDisplayManga
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedRepository(
    private val db: DatabaseHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val preferenceHelper: PreferencesHelper = Injekt.get(),
) {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val bySeriesSet = mutableSetOf<Long>()

    suspend fun getUpdatedFeedMangaForHistoryBySeries(feedManga: FeedManga): FeedManga {
        val chapterHistories = db.getChapterHistoryByMangaId(feedManga.mangaId).executeOnIO()
        val simpleChapters = chapterHistories.mapNotNull { chpHistory ->
            chpHistory.chapter.toSimpleChapter(chpHistory.history.last_read)!!.toChapterItem()
        }.toPersistentList()

        return feedManga.copy(
            chapters = simpleChapters,
        )
    }

    suspend fun getPage(searchQuery: String = "", offset: Int, limit: Int, type: FeedScreenType, group: FeedHistoryGroup): Result<Pair<Boolean, List<FeedManga>>, ResultError.Generic> {
        return com.github.michaelbull.result.runCatching {
            when (type) {
                FeedScreenType.Updates -> {
                    val chapters = db.getRecentChapters(search = searchQuery, offset = offset, limit = limit, isResuming = false).executeAsBlocking()
                        .mapNotNull {
                            val chapterItem = getChapterItem(it.manga, it.chapter.toSimpleChapter()!!)
                            FeedManga(
                                mangaId = chapterItem.chapter.mangaId,
                                mangaTitle = it.manga.title,
                                date = chapterItem.chapter.dateFetch,
                                artwork = it.manga.toDisplayManga().currentArtwork,
                                chapters = persistentListOf(chapterItem),
                            )
                        }
                    Pair(chapters.isNotEmpty(), chapters)
                }

                FeedScreenType.History -> {
                    val chapters = when (group) {
                        FeedHistoryGroup.Series -> {
                            if (offset == 0) {
                                bySeriesSet.clear()
                            }
                            db.getRecentMangaLimit(search = searchQuery, offset = offset, isResuming = false).executeOnIO()
                                .mapNotNull { history ->
                                    history.manga.id ?: return@mapNotNull null
                                    history.chapter.id ?: return@mapNotNull null
                                    if (bySeriesSet.contains(history.manga.id)) {
                                        return@mapNotNull null
                                    }

                                    val chapterHistories = db.getChapterHistoryByMangaId(history.manga.id!!).executeOnIO()
                                    val chapterItems = chapterHistories.mapNotNull { chpHistory ->
                                        getChapterItem(chpHistory.manga, chpHistory.chapter.toSimpleChapter(chpHistory.history.last_read)!!)
                                    }.toPersistentList()

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

                        FeedHistoryGroup.Day, FeedHistoryGroup.Week -> {
                            val pattern = when (group == FeedHistoryGroup.Week) {
                                true -> "yyyy-w"
                                false -> "yyyy-MM-dd"
                            }
                            val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
                            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7 + 1
                            dateFormat.calendar.firstDayOfWeek = dayOfWeek
                            db.getHistoryUngrouped(search = searchQuery, offset = offset, isResuming = false).executeOnIO().groupBy {
                                val date = it.history.last_read
                                it.manga to (if (date <= 0L) "-1" else dateFormat.format(Date(date)))
                            }.mapNotNull { (manga, matches) ->
                                val chapterItems = matches.map {
                                    getChapterItem(it.manga, it.chapter.toSimpleChapter(it.history.last_read)!!)
                                }.toPersistentList()
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
                            db.getHistoryUngrouped(search = searchQuery, offset = offset, isResuming = false).executeOnIO().mapNotNull {
                                it.manga.id ?: return@mapNotNull null
                                it.chapter.id ?: return@mapNotNull null
                                val chapterItem = getChapterItem(it.manga, it.chapter.toSimpleChapter(it.history.last_read)!!)
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

                else -> throw Exception("Not valid")
            }
        }.mapError { err ->
            TimberKt.e(err)
            ResultError.Generic("Error : ${err.message}")
        }
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

    suspend fun getUpdateChapters(feedManga: FeedManga): ImmutableList<ChapterItem> {
        val dbManga = db.getManga(feedManga.mangaId).executeOnIO()!!
        return feedManga.chapters.map { chapterItem ->
            getChapterItem(dbManga, chapterItem.chapter)
        }.toImmutableList()
    }

    fun getChapterItem(manga: Manga, chapter: SimpleChapter): ChapterItem {
        val downloadState = when {
            downloadManager.isChapterDownloaded(chapter.toDbChapter(), manga) -> Download.State.DOWNLOADED
            downloadManager.hasQueue() -> downloadManager.queue.find { chapter.id == it.chapter.id }?.status ?: Download.State.default
            else -> Download.State.default
        }

        return ChapterItem(
            chapter = chapter,
            downloadState = downloadState,
            downloadProgress = when (downloadState == Download.State.DOWNLOADING) {
                true -> downloadManager.queue.find { it.chapter.id == chapter.id }!!.progressFloat
                false -> 0f
            },
        )
    }

    suspend fun downloadChapter(feedManga: FeedManga, chapterItem: ChapterItem) {
        val dbManga = db.getManga(feedManga.mangaId).executeOnIO()!!
        downloadManager.downloadChapters(dbManga, listOf(chapterItem.chapter.toDbChapter()))
    }
}
