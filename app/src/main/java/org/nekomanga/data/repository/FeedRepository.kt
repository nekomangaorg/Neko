package org.nekomanga.data.repository

import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.feed.FeedHistoryGroup
import org.nekomanga.domain.feed.FeedItem
import org.nekomanga.domain.feed.FeedPage
import org.nekomanga.domain.network.ResultError

interface FeedRepository {
    suspend fun getUpdatedFeedItemForHistoryBySeries(feedItem: FeedItem): FeedItem

    suspend fun getSummaryUpdatesList(): Result<List<FeedItem>, ResultError.Generic>

    suspend fun getSummaryContinueReadingList(): Result<List<FeedItem>, ResultError.Generic>

    suspend fun getSummaryNewlyAddedList(): Result<List<FeedItem>, ResultError.Generic>

    suspend fun getHistoryPage(
        searchQuery: String = "",
        offset: Int,
        limit: Int = 25,
        group: FeedHistoryGroup,
        seenMangaIds: Set<Long> = emptySet(),
    ): Result<FeedPage, ResultError.Generic>

    suspend fun getUpdatesPage(
        searchQuery: String = "",
        offset: Int,
        limit: Int,
        uploadsFetchSort: Boolean,
    ): Result<FeedPage, ResultError.Generic>

    suspend fun deleteAllHistory()

    suspend fun deleteChapter(chapterItem: ChapterItem)

    suspend fun deleteAllHistoryForManga(mangaId: Long)

    suspend fun deleteHistoryForChapter(chapterUrl: String)

    fun getChapterItem(manga: Manga, chapter: SimpleChapter): ChapterItem

    suspend fun toggleChapterRead(chapterItem: ChapterItem): ChapterItem

    suspend fun markChapter(chapterItem: ChapterItem, markAction: ChapterMarkActions): ChapterItem

    suspend fun downloadChapter(
        feedItem: FeedItem,
        chapterItem: ChapterItem,
        downloadAction: MangaConstants.DownloadAction,
    )

    suspend fun getRecentlyReadManga(): List<Manga>
}
