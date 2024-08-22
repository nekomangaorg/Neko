package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.database.models.Scanlator
import eu.kanade.tachiyomi.data.database.models.SourceArtwork
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.MangaDetailChapterInformation
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.ResultListPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.uuid
import eu.kanade.tachiyomi.source.online.handlers.ImageHandler
import eu.kanade.tachiyomi.source.online.handlers.LatestChapterHandler
import eu.kanade.tachiyomi.source.online.handlers.ListHandler
import eu.kanade.tachiyomi.source.online.handlers.ListResults
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.RatingHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Response
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

open class MangaDex : HttpSource() {

    private val preferences: PreferencesHelper by injectLazy()

    private val ratingHandler: RatingHandler by injectLazy()

    private val mangaHandler: MangaHandler by injectLazy()

    private val searchHandler: SearchHandler by injectLazy()

    private val listHandler: ListHandler by injectLazy()

    private val pageHandler: PageHandler by injectLazy()

    private val imageHandler: ImageHandler by injectLazy()

    private val loginHelper: MangaDexLoginHelper by injectLazy()

    private val latestChapterHandler: LatestChapterHandler by injectLazy()

    suspend fun addToCustomList(mangaID: String, listID: String): Boolean {
        return listHandler.addToCustomList(mangaID, listID).result?.equals("ok", true) ?: false
    }

    suspend fun removeFromCustomList(mangaID: String, listID: String): Boolean {
        return listHandler.removeFromCustomList(mangaID, listID).result?.equals("ok", true) ?: false
    }

    suspend fun createCustomList(listName: String, isPublic: Boolean): Boolean {
        return listHandler.createCustomList(listName, isPublic).result?.equals("ok", true) ?: false
    }

    suspend fun deleteCustomList(listID: String): Boolean {
        return listHandler.deleteCustomList(listID).result?.equals("ok", true) ?: false
    }

    suspend fun getRandomManga(): Result<SourceManga, ResultError> {
        return withIOContext {
            val response =
                networkServices.service.randomManga(
                    preferences.contentRatingSelections().get().toList()
                )

            val result =
                response.getOrResultError("trying to get random Manga").andThen {
                    Ok(
                        it.data.toSourceManga(
                            preferences.thumbnailQuality().get(),
                            useNoCoverUrl = false
                        )
                    )
                }

            return@withIOContext result
        }
    }

    suspend fun getChapterCommentId(chapterUUID: String): Result<String?, ResultError> {
        return withIOContext {
            return@withIOContext mangaHandler.fetchChapterCommentId(chapterUUID)
        }
    }

    suspend fun getScanlator(scanlator: String): Result<Scanlator, ResultError> {
        return withIOContext {
            networkServices.service
                .scanlatorGroup(scanlator)
                .getOrResultError("Trying to get scanlator")
                .andThen { groupListDto ->
                    val groupDto = groupListDto.data.firstOrNull()
                    when (groupDto == null) {
                        true -> Err("No Scanlator Group found".toResultError())
                        false -> {
                            Ok(
                                Scanlator(
                                    name = groupDto.attributes.name,
                                    uuid = groupDto.id,
                                    description = groupDto.attributes.description,
                                ),
                            )
                        }
                    }
                }
        }
    }

    suspend fun search(page: Int, filters: DexFilters): Result<MangaListPage, ResultError> {
        return searchHandler.search(page, filters)
    }

    suspend fun searchForManga(uuid: String): Result<MangaListPage, ResultError> {
        return searchHandler.searchForManga(uuid)
    }

    suspend fun searchForAuthor(authorQuery: String): Result<ResultListPage, ResultError> {
        return searchHandler.searchForAuthor(authorQuery)
    }

    suspend fun searchForGroup(groupQuery: String): Result<ResultListPage, ResultError> {
        return searchHandler.searchForGroup(groupQuery)
    }

    suspend fun fetchUserLists(page: Int): Result<ResultListPage, ResultError> {
        return listHandler.retrieveUserLists(page)
    }

    suspend fun fetchAllUserLists(): Result<ResultListPage, ResultError> {
        return listHandler.retrieveAllUserLists()
    }

    suspend fun fetchList(
        listId: String,
        page: Int,
        privateList: Boolean
    ): Result<ListResults, ResultError> {
        return listHandler.retrieveMangaFromList(listId, page, privateList)
    }

    suspend fun fetchAllFromList(
        listId: String,
        privateList: Boolean
    ): Result<ImmutableList<SourceManga>, ResultError> {
        return listHandler.retrieveAllMangaFromList(listId, privateList)
    }

    suspend fun fetchHomePageInfo(
        blockedScanlatorUUIDs: List<String>,
        showSubscriptionFeed: Boolean
    ): Result<List<ListResults>, ResultError> {
        return withIOContext {
            coroutineBinding {
                val seasonal = async {
                    fetchList(MdConstants.oldSeasonalId, 1, false)
                        .andThen { listResults ->
                            Ok(
                                listResults.copy(
                                    sourceManga =
                                        listResults.sourceManga.shuffled().toImmutableList()
                                )
                            )
                        }
                        .bind()
                }

                val staffPick = async {
                    fetchList(MdConstants.staffPicksId, 1, false)
                        .andThen { listResults ->
                            Ok(
                                listResults.copy(
                                    sourceManga =
                                        listResults.sourceManga.shuffled().toImmutableList()
                                )
                            )
                        }
                        .bind()
                }

                val nekoDevPicks = async {
                    fetchList(MdConstants.nekoDevPicksId, 1, false)
                        .andThen { listResults ->
                            Ok(
                                listResults.copy(
                                    sourceManga =
                                        listResults.sourceManga.shuffled().toImmutableList()
                                )
                            )
                        }
                        .bind()
                }

                val subscriptionFeed = async {
                    if (showSubscriptionFeed) {
                        latestChapterHandler
                            .getPage(
                                blockedScanlatorUUIDs = blockedScanlatorUUIDs,
                                limit = MdConstants.Limits.latestSmaller,
                                feedType = MdConstants.FeedType.Subscription
                            )
                            .andThen { mangaListPage ->
                                Ok(
                                    ListResults(
                                        displayScreenType = DisplayScreenType.SubscriptionFeed(),
                                        sourceManga = mangaListPage.sourceManga
                                    )
                                )
                            }
                            .bind()
                    } else {
                        null
                    }
                }

                val popularNewTitles = async {
                    searchHandler
                        .popularNewTitles(1)
                        .andThen { mangaListPage ->
                            Ok(
                                ListResults(
                                    displayScreenType = DisplayScreenType.PopularNewTitles(),
                                    sourceManga =
                                        mangaListPage.sourceManga.shuffled().toImmutableList()
                                )
                            )
                        }
                        .bind()
                }

                val latestChapter = async {
                    latestChapterHandler
                        .getPage(
                            blockedScanlatorUUIDs = blockedScanlatorUUIDs,
                            limit = MdConstants.Limits.latestSmaller
                        )
                        .andThen { mangaListPage ->
                            Ok(
                                ListResults(
                                    displayScreenType = DisplayScreenType.LatestChapters(),
                                    sourceManga = mangaListPage.sourceManga
                                )
                            )
                        }
                        .bind()
                }

                val recentlyAdded = async {
                    searchHandler
                        .recentlyAdded(1)
                        .andThen { mangaListPage ->
                            Ok(
                                ListResults(
                                    displayScreenType = DisplayScreenType.RecentlyAdded(),
                                    sourceManga = mangaListPage.sourceManga
                                )
                            )
                        }
                        .bind()
                }

                listOfNotNull(
                    subscriptionFeed.await(),
                    popularNewTitles.await(),
                    latestChapter.await(),
                    seasonal.await(),
                    staffPick.await(),
                    nekoDevPicks.await(),
                    recentlyAdded.await()
                )
            }
        }
    }

    suspend fun recentlyAdded(page: Int): Result<MangaListPage, ResultError> {
        return searchHandler.recentlyAdded(page)
    }

    suspend fun popularNewTitles(page: Int): Result<MangaListPage, ResultError> {
        return searchHandler.popularNewTitles(page)
    }

    suspend fun latestChapters(
        page: Int,
        blockedScanlatorUUIDs: List<String>,
        feedType: MdConstants.FeedType
    ): Result<MangaListPage, ResultError> {
        return latestChapterHandler.getPage(page, blockedScanlatorUUIDs, feedType = feedType)
    }

    suspend fun getMangaDetails(
        mangaUUID: String,
        fetchArtwork: Boolean = true
    ): Result<Pair<SManga, List<SourceArtwork>>, ResultError> {
        return logTimeTaken("Total time to get manga details $mangaUUID") {
            mangaHandler.fetchMangaDetails(mangaUUID, fetchArtwork)
        }
    }

    suspend fun fetchMangaAndChapterDetails(
        manga: SManga,
        fetchArtwork: Boolean
    ): Result<MangaDetailChapterInformation, ResultError> {
        return mangaHandler.fetchMangaAndChapterDetails(manga, fetchArtwork)
    }

    open suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return mangaHandler.getMangaIdFromChapterId(urlChapterId)
    }

    suspend fun fetchChapterList(manga: SManga): Result<List<SChapter>, ResultError> {
        return mangaHandler.fetchChapterList(manga.uuid(), manga.last_chapter_number)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return pageHandler.fetchPageList(chapter)
    }

    override suspend fun getImage(page: Page): Response {
        return imageHandler.getImage(page, loginHelper.isLoggedIn())
    }

    open suspend fun updateRating(track: Track): Boolean {
        return ratingHandler.updateRating(track)
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        if (!loginHelper.isLoggedIn()) {
            throw Exception("Not Logged in to MangaDex")
        }
        return withContext(Dispatchers.IO) {
            val mangaUUID = MdUtil.getMangaUUID(url)
            val ratingResponse = networkServices.authService.retrieveRating(mangaUUID)
            TimberKt.d { "mangaUUID $mangaUUID" }
            val list =
                networkServices.authService
                    .customListsContainingManga(mangaUUID)
                    .onFailure {
                        this.log("trying to fetch list status for $mangaUUID")
                        throw Exception("error trying to get tracking info")
                    }
                    .getOrThrow()

            val rating = ratingResponse.getOrThrow().ratings.asMdMap<RatingDto>()[mangaUUID]
            val track =
                Track.create(TrackManager.MDLIST).apply {
                    listIds = list.data.map { it.id }
                    tracking_url = "${MdConstants.baseUrl}/title/$mangaUUID"
                    score = rating?.rating?.toFloat() ?: 0f
                }
            return@withContext track
        }
    }

    suspend fun checkIfUp(): Boolean {
        return withContext(Dispatchers.IO) {
            true
            // val response = network.client.newCall(GET(MdUtil.apiUrl + MdUtil.apiManga +
            // 1)).await()
            // response.isSuccessful
        }
    }

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return MdConstants.baseUrl + simpleChapter.url
    }

    override val headers: Headers
        get() = network.headers
}
