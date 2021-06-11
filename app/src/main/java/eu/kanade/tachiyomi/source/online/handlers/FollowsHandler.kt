package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.GetReadingStatus
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaListResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaStatusListResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.UpdateReadingStatus
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaId
import eu.kanade.tachiyomi.v5.db.V5DbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class FollowsHandler {

    val preferences: PreferencesHelper by injectLazy()
    val network: NetworkHelper by injectLazy()
    val v5DbHelper: V5DbHelper by injectLazy()

    /**
     * fetch all follows
     */
    fun fetchFollows(): Observable<MangasPage> {
        return network.authClient.newCall(followsListRequest(0))
            .asObservable()
            .map { response ->
                val mangaListResponse = MdUtil.jsonParser.decodeFromString<MangaListResponse>(response.body!!.string())
                val results = mangaListResponse.results.toMutableList()

                var hasMoreResults = mangaListResponse.limit + mangaListResponse.offset < mangaListResponse.total

                var offset = mangaListResponse.offset
                val limit = mangaListResponse.limit

                while (hasMoreResults) {
                    offset += limit
                    val newResponse = network.authClient.newCall(followsListRequest(offset)).execute()
                    if (newResponse.code != 200) {
                        hasMoreResults = false
                    } else {
                        val newMangaListResponse = MdUtil.jsonParser.decodeFromString<MangaListResponse>(newResponse.body!!.string())
                        hasMoreResults = newMangaListResponse.limit + newMangaListResponse.offset < newMangaListResponse.total
                        results.addAll(newMangaListResponse.results)
                    }
                }
                val readingStatusResponse = network.authClient.newCall(readingStatusRequest()).execute()
                val json = MdUtil.jsonParser.decodeFromString<MangaStatusListResponse>(readingStatusResponse.body!!.string())

                followsParseMangaPage(results, json.statuses)
            }
    }

    /**
     * Parse follows api to manga page
     * used when multiple follows
     */
    private fun followsParseMangaPage(response: List<MangaResponse>, readingStatusMap: Map<String, String?>): MangasPage {
        val comparator = compareBy<SManga> { it.follow_status }.thenBy { it.title }

        val coverMap = MdUtil.getCoversFromMangaList(response, network.client)

        val result = response.map {
            val coverUrl = coverMap[it.data.id]
            MdUtil.createMangaEntry(it, coverUrl).apply {
                this.follow_status = FollowStatus.fromDex(readingStatusMap[it.data.id])
            }
        }.sortedWith(comparator)

        return MangasPage(result, false)
    }

    /**fetch follow status used when fetching status for 1 manga
     *
     */

    private fun followStatusParse(response: Response, mangaId: String): Track {
        val mangaResponse = MdUtil.jsonParser.decodeFromString<GetReadingStatus>(response.body!!.string())
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
        return GET(tempUrl.build().toString(), MdUtil.getAuthHeaders(network.headers, preferences), CacheControl.FORCE_NETWORK)
    }

    private fun readingStatusRequest(): Request {
        return GET(MdUtil.readingStatusesUrl, MdUtil.getAuthHeaders(network.headers, preferences), CacheControl.FORCE_NETWORK)
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

            val jsonString = MdUtil.jsonParser.encodeToString(UpdateReadingStatus(status))

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
            val mangaListResponse = MdUtil.jsonParser.decodeFromString<MangaListResponse>(response.body!!.string())
            val results = mangaListResponse.results.toMutableList()

            var hasMoreResults = mangaListResponse.limit + mangaListResponse.offset < mangaListResponse.total
            var offset = mangaListResponse.offset
            val limit = mangaListResponse.limit

            while (hasMoreResults) {
                offset += limit
                val newResponse = network.authClient.newCall(followsListRequest(offset)).await()
                if (newResponse.code != 200) {
                    hasMoreResults = false
                } else {
                    val newMangaListResponse = MdUtil.jsonParser.decodeFromString<MangaListResponse>(newResponse.body!!.string())
                    results.addAll(newMangaListResponse.results)
                    hasMoreResults = newMangaListResponse.limit + newMangaListResponse.offset < newMangaListResponse.total
                }
            }

            val readingStatusResponse = network.authClient.newCall(readingStatusRequest()).execute()
            val json = MdUtil.jsonParser.decodeFromString<MangaStatusListResponse>(readingStatusResponse.body!!.string())

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
