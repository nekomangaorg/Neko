package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.mapSuccess
import eu.kanade.tachiyomi.data.database.models.Scanlator
import eu.kanade.tachiyomi.data.database.models.SourceArtwork
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.Uploader
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.MangaDetailChapterInformation
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.ResultListPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.uuid
import eu.kanade.tachiyomi.source.online.handlers.FeedUpdatesHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.ImageHandler
import eu.kanade.tachiyomi.source.online.handlers.LatestChapterHandler
import eu.kanade.tachiyomi.source.online.handlers.ListHandler
import eu.kanade.tachiyomi.source.online.handlers.ListResults
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.system.withIOContext
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
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.injectLazy

open class MangaDex : HttpSource() {

    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    private val followsHandler: FollowsHandler by injectLazy()

    private val mangaHandler: MangaHandler by injectLazy()

    private val searchHandler: SearchHandler by injectLazy()

    private val listHandler: ListHandler by injectLazy()

    private val pageHandler: PageHandler by injectLazy()

    private val imageHandler: ImageHandler by injectLazy()

    private val loginHelper: MangaDexLoginHelper by injectLazy()

    private val latestChapterHandler: LatestChapterHandler by injectLazy()

    private val feedUpdatesHandler: FeedUpdatesHandler by injectLazy()

    suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        return followsHandler.updateFollowStatus(mangaID, followStatus)
    }

    suspend fun getRandomManga(): Result<SourceManga, ResultError> {
        return withIOContext {
            val response =
                networkServices.service.randomManga(
                    mangaDexPreferences.visibleContentRatings().get().toList()
                )

            val result =
                response.getOrResultError("trying to get random Manga").andThen {
                    Ok(
                        it.data.toSourceManga(
                            mangaDexPreferences.coverQuality().get(),
                            useNoCoverUrl = false,
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
                                )
                            )
                        }
                    }
                }
        }
    }

    suspend fun getUploader(uploader: String): Result<Uploader, ResultError> {
        return withIOContext {
            networkServices.authService
                .uploader(uploader)
                .getOrResultError("Trying to get uploader")
                .andThen { userListDto ->
                    val userDto = userListDto.data.firstOrNull()
                    when (userDto == null) {
                        true -> Err("User not found".toResultError())
                        false -> {
                            Ok(Uploader(username = userDto.attributes.username, uuid = userDto.id))
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

    suspend fun fetchList(listId: String): Result<ListResults, ResultError> {
        return listHandler.retrieveMangaFromList(listId, 1)
    }

    suspend fun fetchAllList(listId: String): Result<ListResults, ResultError> {
        return listHandler.retrieveAllMangaFromList(listId, false)
    }

    suspend fun fetchHomePageInfo(
        blockedScanlatorUUIDs: List<String>,
        blockedUploaderUUIDs: List<String>,
    ): Result<List<ListResults>, ResultError> {
        return withIOContext {
            coroutineBinding {
                val seasonal = async {
                    fetchList(
                            networkServices.service.getSeasonalList().mapSuccess { id }.getOrNull()
                                ?: return@async null
                        )
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
                    fetchList(MdConstants.staffPicksId)
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
                    fetchList(MdConstants.nekoDevPicksId)
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

                val popularNewTitles = async {
                    searchHandler
                        .popularNewTitles(1)
                        .andThen { mangaListPage ->
                            Ok(
                                ListResults(
                                    displayScreenType = DisplayScreenType.PopularNewTitles(),
                                    sourceManga =
                                        mangaListPage.sourceManga.shuffled().toImmutableList(),
                                )
                            )
                        }
                        .bind()
                }

                val latestFeed = async {
                    if (!loginHelper.isLoggedIn()) return@async null
                    feedUpdatesHandler
                        .getPage(
                            blockedScanlatorUUIDs = blockedScanlatorUUIDs,
                            blockedUploaderUUIDs = blockedUploaderUUIDs,
                            limit = MdConstants.Limits.latestSmaller,
                        )
                        .andThen { mangaListPage ->
                            Ok(
                                ListResults(
                                    displayScreenType = DisplayScreenType.FeedUpdates(),
                                    sourceManga = mangaListPage.sourceManga,
                                )
                            )
                        }
                        .bind()
                }

                val latestChapter = async {
                    latestChapterHandler
                        .getPage(
                            blockedScanlatorUUIDs = blockedScanlatorUUIDs,
                            blockedUploaderUUIDs = blockedUploaderUUIDs,
                            limit = MdConstants.Limits.latestSmaller,
                        )
                        .andThen { mangaListPage ->
                            Ok(
                                ListResults(
                                    displayScreenType = DisplayScreenType.LatestChapters(),
                                    sourceManga = mangaListPage.sourceManga,
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
                                    sourceManga = mangaListPage.sourceManga,
                                )
                            )
                        }
                        .bind()
                }

                listOfNotNull(
                    latestFeed.await(),
                    popularNewTitles.await(),
                    latestChapter.await(),
                    seasonal.await(),
                    staffPick.await(),
                    nekoDevPicks.await(),
                    recentlyAdded.await(),
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
        blockedUploaderUUIDs: List<String>,
    ): Result<MangaListPage, ResultError> {
        return latestChapterHandler.getPage(page, blockedScanlatorUUIDs, blockedUploaderUUIDs)
    }

    suspend fun feedUpdates(
        page: Int,
        blockedScanlatorUUIDs: List<String>,
        blockedUploaderUUIDs: List<String>,
    ): Result<MangaListPage, ResultError> {
        return feedUpdatesHandler.getPage(page, blockedScanlatorUUIDs, blockedUploaderUUIDs)
    }

    suspend fun getMangaDetails(
        mangaUUID: String,
        fetchArtwork: Boolean = true,
    ): Result<Pair<SManga, List<SourceArtwork>>, ResultError> {
        return logTimeTaken("Total time to get manga details $mangaUUID") {
            mangaHandler.fetchMangaDetails(mangaUUID, fetchArtwork)
        }
    }

    suspend fun fetchMangaAndChapterDetails(
        manga: SManga,
        fetchArtwork: Boolean,
    ): Result<MangaDetailChapterInformation, ResultError> {
        return mangaHandler.fetchMangaAndChapterDetails(manga, fetchArtwork)
    }

    open suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return mangaHandler.getMangaIdFromChapterId(urlChapterId)
    }

    suspend fun fetchChapterList(manga: SManga): Result<List<SChapter>, ResultError> {
        return mangaHandler.fetchChapterList(
            manga.uuid(),
            manga.last_chapter_number,
            manga.last_volume_number,
            mangaDexPreferences.includeUnavailableChapters().get(),
        )
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return pageHandler.fetchPageList(chapter)
    }

    override suspend fun getImage(page: Page): Response {
        return imageHandler.getImage(page, loginHelper.isLoggedIn())
    }

    suspend fun fetchAllFollows(): Result<List<SourceManga>, ResultError> {
        return followsHandler.fetchAllFollows()
    }

    open suspend fun updateReadingProgress(track: Track): Boolean {
        return followsHandler.updateReadingProgress(track)
    }

    open suspend fun updateRating(track: Track): Boolean {
        return followsHandler.updateRating(track)
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        if (!loginHelper.isLoggedIn()) {
            throw Exception("Not Logged in to MangaDex")
        }
        return followsHandler.fetchTrackingInfo(url)
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
