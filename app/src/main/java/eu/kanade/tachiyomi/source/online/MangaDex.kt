package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.database.models.Scanlator
import eu.kanade.tachiyomi.data.database.models.SourceArtwork
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.MangaDetailChapterInformation
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.uuid
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.ImageHandler
import eu.kanade.tachiyomi.source.online.handlers.LatestChapterHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.lang.toResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Response
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.injectLazy

open class MangaDex : HttpSource() {

    private val preferences: PreferencesHelper by injectLazy()

    private val filterHandler: FilterHandler by injectLazy()

    private val followsHandler: FollowsHandler by injectLazy()

    private val mangaHandler: MangaHandler by injectLazy()

    private val searchHandler: SearchHandler by injectLazy()

    private val pageHandler: PageHandler by injectLazy()

    private val imageHandler: ImageHandler by injectLazy()

    private val loginHelper: MangaDexLoginHelper by injectLazy()

    private val latestChapterHandler: LatestChapterHandler by injectLazy()

    suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        return followsHandler.updateFollowStatus(mangaID, followStatus)
    }

    suspend fun getRandomManga(): Result<SourceManga, ResultError> {
        return withIOContext {
            val response = network.service.randomManga(preferences.contentRatingSelections().toList())

            val result = response.getOrResultError("trying to get random Manga")
                .andThen {
                    Ok(it.data.toSourceManga(preferences.thumbnailQuality(), useNoCoverUrl = false))
                }

            return@withIOContext result
        }
    }

    suspend fun getScanlator(scanlator: String): Result<Scanlator, ResultError> {
        return withIOContext {
            network.service.scanlatorGroup(scanlator).getOrResultError("Trying to get scanlator")
                .andThen { groupListDto ->
                    val groupDto = groupListDto.data.firstOrNull()
                    when (groupDto == null) {
                        true -> Err("No Results".toResultError())
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

    suspend fun search(page: Int, query: String, filters: FilterList): MangaListPage {
        return searchHandler.search(page, query, filters)
    }

    suspend fun latestChapters(page: Int, blockedScanlatorUUIDs: List<String>): Result<MangaListPage, ResultError> {
        return latestChapterHandler.getPage(page, blockedScanlatorUUIDs)
    }

    suspend fun getMangaDetails(mangaUUID: String, fetchArtwork: Boolean = true): Result<Pair<SManga, List<SourceArtwork>>, ResultError> {
        return logTimeTaken("Total time to get manga details $mangaUUID") { mangaHandler.fetchMangaDetails(mangaUUID, fetchArtwork) }
    }

    suspend fun fetchMangaAndChapterDetails(manga: SManga, fetchArtwork: Boolean): Result<MangaDetailChapterInformation, ResultError> {
        return mangaHandler.fetchMangaAndChapterDetails(manga, fetchArtwork)
    }

    open suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return mangaHandler.getMangaIdFromChapterId(urlChapterId)
    }

    suspend fun fetchChapterList(manga: SManga): Result<List<SChapter>, ResultError> {
        return mangaHandler.fetchChapterList(manga.uuid(), manga.last_chapter_number)
    }

    override suspend fun fetchPageList(chapter: SChapter): List<Page> {
        return pageHandler.fetchPageList(chapter)
    }

    override suspend fun fetchImage(page: Page): Response {
        return imageHandler.getImage(page, isLogged())
    }

    suspend fun fetchAllFollows(): Result<Map<Int, List<SourceManga>>, ResultError> {
        return followsHandler.fetchAllFollows()
    }

    open suspend fun updateReadingProgress(track: Track): Boolean {
        return followsHandler.updateReadingProgress(track)
    }

    open suspend fun updateRating(track: Track): Boolean {
        return followsHandler.updateRating(track)
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        if (!isLogged()) {
            throw Exception("Not Logged in to MangaDex")
        }
        return followsHandler.fetchTrackingInfo(url)
    }

    override fun isLogged(): Boolean {
        return !preferences.sourceUsername(this).isNullOrBlank() &&
            !preferences.sourcePassword(this).isNullOrBlank() &&
            !preferences.sessionToken().isNullOrBlank() &&
            !preferences.refreshToken().isNullOrBlank()
    }

    override suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String,
    ): Boolean {
        return loginHelper.login(username, password)
    }

    suspend fun checkIfUp(): Boolean {
        return withContext(Dispatchers.IO) {
            true
            // val response = network.client.newCall(GET(MdUtil.apiUrl + MdUtil.apiManga + 1)).await()
            // response.isSuccessful
        }
    }

    override suspend fun logout(): Logout {
        return withContext(Dispatchers.IO) {
            network.authService.logout().onFailure {
                this.log("trying to logout")
            }
            return@withContext Logout(true)
        }
    }

    override val headers: Headers
        get() = network.headers

    override fun getFilterList(): FilterList {
        return filterHandler.getMDFilterList()
    }
}
