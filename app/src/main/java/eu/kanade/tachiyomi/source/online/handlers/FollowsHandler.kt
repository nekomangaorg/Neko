package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.GetReadingStatus
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaListResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.MangaResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.UpdateReadingStatus
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import java.util.Locale

class FollowsHandler(val client: OkHttpClient, val headers: Headers, val preferences: PreferencesHelper) {

    /**
     * fetch all follows
     */
    fun fetchFollows(): Observable<MangasPage> {
        return client.newCall(followsListRequest(0))
            .asObservable()
            .map { response ->
                val mangaListResponse = MdUtil.jsonParser.decodeFromString(MangaListResponse.serializer(), response.body!!.string())
                val results = mangaListResponse.results

                var hasMoreResults = mangaListResponse.limit + mangaListResponse.offset < mangaListResponse.total

                while (hasMoreResults) {
                    val offset = mangaListResponse.offset + mangaListResponse.limit
                    val newResponse = client.newCall(followsListRequest(offset)).execute()
                    val newMangaListResponse = MdUtil.jsonParser.decodeFromString(MangaListResponse.serializer(), newResponse.body!!.toString())
                    hasMoreResults = newMangaListResponse.limit + newMangaListResponse.offset < newMangaListResponse.total
                }
                followsParseMangaPage(results)
            }
    }

    /**
     * Parse follows api to manga page
     * used when multiple follows
     */
    private fun followsParseMangaPage(response: List<MangaResponse>): MangasPage {

        val comparator = compareBy<SManga> { it.follow_status }.thenBy { it.title }
        val result = response.map {
            MdUtil.createMangaEntry(it, preferences)
        }.sortedWith(comparator)

        return MangasPage(result, false)
    }

    /**fetch follow status used when fetching status for 1 manga
     *
     */

    private fun followStatusParse(response: Response, mangaId: String): Track {

        val mangaResponse = MdUtil.jsonParser.decodeFromString(GetReadingStatus.serializer(), response.body!!.string())
        val followStatus = FollowStatus.fromDex(mangaResponse.status)
        val track = Track.create(TrackManager.MDLIST)
        track.status = followStatus.int
        track.tracking_url = "$baseUrl/manga/$mangaId"

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
        val tempUrl = MdUtil.userFollows.toHttpUrlOrNull()!!.newBuilder()

        tempUrl.apply {
            addQueryParameter("limit", MdUtil.mangaLimit.toString())
            addQueryParameter("offset", offset.toString())
        }
        return GET(tempUrl.build().toString(), MdUtil.getAuthHeaders(headers, preferences), CacheControl.FORCE_NETWORK)
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

            val jsonString = MdUtil.jsonParser.encodeToString(UpdateReadingStatus.serializer(), UpdateReadingStatus(status))

            val postResult = client.newCall(
                POST(
                    MdUtil.getReadingStatusUrl(mangaId),
                    MdUtil.getAuthHeaders(headers, preferences),
                    jsonString.toRequestBody("application/json".toMediaType())
                )
            ).await()
            postResult.isSuccessful
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
                client.newCall(
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
                client.newCall(
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
            val response = client.newCall(followsListRequest(0)).await()
            val mangaListResponse = MdUtil.jsonParser.decodeFromString(MangaListResponse.serializer(), response.body!!.string())
            val results = mangaListResponse.results

            var hasMoreResults = mangaListResponse.limit + mangaListResponse.offset < mangaListResponse.total

            while (hasMoreResults) {
                val offset = mangaListResponse.offset + mangaListResponse.limit
                val newResponse = client.newCall(followsListRequest(offset)).await()
                val newMangaListResponse = MdUtil.jsonParser.decodeFromString(MangaListResponse.serializer(), newResponse.body!!.toString())
                hasMoreResults = newMangaListResponse.limit + newMangaListResponse.offset < newMangaListResponse.total
            }


            followsParseMangaPage(results).mangas
        }
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        return withContext(Dispatchers.IO) {
            val mangaId = getMangaId(url)
            val request = GET(
                MdUtil.getReadingStatusUrl(mangaId),
                MdUtil.getAuthHeaders(headers, preferences),
                CacheControl.FORCE_NETWORK
            )
            val response = client.newCall(request).await()
            val track = followStatusParse(response, mangaId)

            track
        }
    }
}
