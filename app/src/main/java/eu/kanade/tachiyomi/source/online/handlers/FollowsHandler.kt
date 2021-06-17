package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.MangaDto
import eu.kanade.tachiyomi.source.online.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.dto.ReadingStatusMapDto
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaId
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class FollowsHandler {

    val preferences: PreferencesHelper by injectLazy()
    val network: NetworkHelper by injectLazy()
    val service: MangaDexAuthService by lazy { Injekt.get<NetworkHelper>().authService }

    /**
     * fetch all follows
     */
    suspend fun fetchFollows(): MangaListPage {
        return withContext(Dispatchers.IO) {
            val response = service.userFollowList(0)

            val mangaListDto = response.body()!!
            val results = mangaListDto.results.toMutableList()

            val readingFuture = async { service.readingStatusForAllManga().body()!!.statuses }

            if (mangaListDto.total > mangaListDto.limit) {
                val totalRequestNo = (mangaListDto.total / mangaListDto.limit)
                val restOfResults = (1..totalRequestNo).asSequence().map { pos ->
                    async {
                        val newResponse = service.userFollowList(pos * mangaListDto.limit)
                        newResponse.body()!!.results
                    }
                }.toList().awaitAll().flatten()
                results.addAll(restOfResults)
            }
            val readingStatusResponse = readingFuture.await()

            followsParseMangaPage(results, readingStatusResponse)
        }
    }

    /**
     * Parse follows api to manga page
     * used when multiple follows
     */
    private fun followsParseMangaPage(
        response: List<MangaDto>,
        readingStatusMap: Map<String, String?>,
    ): MangaListPage {
        val comparator = compareBy<SManga> { it.follow_status }.thenBy { it.title }

        val result = response.map { mangaDto ->
            mangaDto.toBasicManga().apply {
                this.follow_status = FollowStatus.fromDex(readingStatusMap[mangaDto.data.id])
            }
        }.sortedWith(comparator)

        return MangaListPage(result, false)
    }

    /**fetch follow status used when fetching status for 1 manga
     *
     */

    private fun followStatusParse(response: Response, mangaId: String): Track {
        val mangaResponse =
            MdUtil.jsonParser.decodeFromString<ReadingStatusDto>(response.body!!.string())
        val followStatus = FollowStatus.fromDex(mangaResponse.status)
        val track = Track.create(TrackManager.MDLIST)
        track.status = followStatus.int
        track.tracking_url = "$baseUrl/title/$mangaId"

/* if (follow.chapter.isNotBlank()) {
                track.last_chapter_read = floor(follow.chapter.toFloat()).toInt()
            }

        */
        return track
    }

    /**build Request for follows page
     *
     */
    private fun followsListRequest(offset: Int): Request {
        val tempUrl = MdUtil.userFollowsUrl.toHttpUrlOrNull()!!.newBuilder()

        tempUrl.apply {
            addQueryParameter("limit", "100")
            addQueryParameter("offset", offset.toString())
        }
        return GET(tempUrl.build().toString(),
            MdUtil.getAuthHeaders(network.headers, preferences),
            CacheControl.FORCE_NETWORK)
    }

    private fun readingStatusRequest(): Request {
        return GET(MdUtil.readingStatusesUrl,
            MdUtil.getAuthHeaders(network.headers, preferences),
            CacheControl.FORCE_NETWORK)
    }

    /**
     * Change the status of a manga
     */
    suspend fun updateFollowStatus(mangaId: String, followStatus: FollowStatus): Boolean {
        return withContext(Dispatchers.IO) {
            val status = when (followStatus == FollowStatus.UNFOLLOWED) {
                true -> null
                false -> followStatus.name.toLowerCase(Locale.US)
            }

            val jsonString = MdUtil.jsonParser.encodeToString(ReadingStatusDto(status))

            try {
                val postResult = network.authClient.newCall(
                    POST(
                        MdUtil.getReadingStatusUrl(mangaId),
                        MdUtil.getAuthHeaders(network.headers, preferences),
                        jsonString.toRequestBody("application/json".toMediaType())
                    )
                ).await()
                postResult.isSuccessful
            } catch (e: Exception) {
                XLog.e("error posting", e)
                false
            }
        }
    }

    suspend fun updateReadingProgress(track: Track): Boolean {
        return true
        /*return withContext(Dispatchers.IO) {
            val mangaID = getMangaId(track.tracking_url)
            val formBody = FormBody.Builder()
                .add("volume", "0")
                .add("chapter", track.last_chapter_read.toString())
            XLog.d("chapter to update %s", track.last_chapter_read.toString())
            val result = runCatching {
                network.authClient.newCall(
                    POST(
                        "$baseUrl/ajax/actions.ajax.php?function=edit_progress&id=$mangaID",
                        headers,
                        formBody.build()
                    )
                ).execute()
            }
            result.exceptionOrNull()?.let {
                if (it is EOFException) {
                    return@withContext true
                } else {
                    XLog.e("error updating reading progress", it)
                    return@withContext false
                }
            }
            return@withContext result.isSuccess
        }*/
    }

    suspend fun updateRating(track: Track): Boolean {
        return true
        /*return withContext(Dispatchers.IO) {
            val mangaID = getMangaId(track.tracking_url)
            val result = runCatching {
                network.authClient.newCall(
                    GET(
                        "$baseUrl/ajax/actions.ajax.php?function=manga_rating&id=$mangaID&rating=${track.score.toInt()}",
                        headers
                    )
                )
                    .execute()
            }

            result.exceptionOrNull()?.let {
                if (it is EOFException) {
                    return@withContext true
                } else {
                    XLog.e("error updating rating", it)
                    return@withContext false
                }
            }
            return@withContext result.isSuccess
        }*/
    }

    /**
     * fetch all manga from all possible pages
     */
    suspend fun fetchAllFollows(): List<SManga> {
        return withContext(Dispatchers.IO) {
            val response = network.authClient.newCall(followsListRequest(0)).await()
            val mangaListResponse =
                MdUtil.jsonParser.decodeFromString<MangaListDto>(response.body!!.string())
            val results = mangaListResponse.results.toMutableList()

            var hasMoreResults =
                mangaListResponse.limit + mangaListResponse.offset < mangaListResponse.total
            var offset = mangaListResponse.offset
            val limit = mangaListResponse.limit

            while (hasMoreResults) {
                offset += limit
                val newResponse = network.authClient.newCall(followsListRequest(offset)).await()
                if (newResponse.code != 200) {
                    hasMoreResults = false
                } else {
                    val newMangaListResponse =
                        MdUtil.jsonParser.decodeFromString<MangaListDto>(newResponse.body!!.string())
                    results.addAll(newMangaListResponse.results)
                    hasMoreResults =
                        newMangaListResponse.limit + newMangaListResponse.offset < newMangaListResponse.total
                }
            }

            val readingStatusResponse = network.authClient.newCall(readingStatusRequest()).execute()
            val json =
                MdUtil.jsonParser.decodeFromString<ReadingStatusMapDto>(readingStatusResponse.body!!.string())

            followsParseMangaPage(results, json.statuses).manga
        }
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        return withContext(Dispatchers.IO) {
            val mangaId = getMangaId(url)
            val request = GET(
                MdUtil.getReadingStatusUrl(mangaId),
                MdUtil.getAuthHeaders(network.headers, preferences),
                CacheControl.FORCE_NETWORK
            )
            val response = network.authClient.newCall(request).await()
            val track = followStatusParse(response, mangaId)

            track
        }
    }
}
