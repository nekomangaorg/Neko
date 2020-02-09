package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.FollowsPageResult
import eu.kanade.tachiyomi.source.online.handlers.serializers.Result
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import rx.Observable
import timber.log.Timber

class FollowsHandler(val client: OkHttpClient, val headers: Headers) {

    /**
     * fetch follows by page
     */
    fun fetchFollows(page: Int): Observable<MangasPage> {
        return client.newCall(followsListRequest(page))
                .asObservable()
                .map { response ->
                    followsParseMangaPage(response)
                }
    }


    /**
     * Parse follows api to manga page
     * used when multiple follows
     */
    private fun followsParseMangaPage(response: Response): MangasPage {
        val followsPageResult = Json.nonstrict.parse(FollowsPageResult.serializer(), response.body!!.string())

        if (followsPageResult.result.isEmpty()) {
            return MangasPage(mutableListOf(), false)
        }
        val follows = followsPageResult.result.map {
            followFromElement(it)
        }

        return MangasPage(follows, true)
    }

    /**fetch follow status used when fetching status for 1 manga
     *
     */

    private fun followStatusParse(response: Response): Track {
        val followsPageResult = Json.nonstrict.parse(FollowsPageResult.serializer(), response.body!!.string())
        val track = Track.create(TrackManager.MDLIST)
        val result = followsPageResult.result
        if (result.isEmpty()) {
            track.status = FollowStatus.UNFOLLOWED.int
        } else {
            track.status = result[0].follow_type
            if (result[0].chapter.isNotBlank()) {
                track.last_chapter_read = result[0].chapter.toInt()
            }

        }
        return track

    }

    /**build Request for follows page
     *
     */
    private fun followsListRequest(page: Int): Request {

        val url = "${MdUtil.baseUrl}${MdUtil.followsAllApi}".toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("page", page.toString())

        return GET(url.toString(), headers, CacheControl.FORCE_NETWORK)
    }

    /**
     * Parse result element  to manga
     */
    private fun followFromElement(result: Result): SManga {
        val manga = SManga.create()
        manga.title = MdUtil.cleanString(result.title)
        manga.thumbnail_url = "${MdUtil.cdnUrl}/images/manga/${result.manga_id}.jpg"
        manga.url = "/manga/${result.manga_id}/"
        manga.follow_status = FollowStatus.fromInt(result.follow_type)
        return manga
    }

    /**
     * Change the status of a manga
     */
    suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        return withContext(Dispatchers.IO) {

            val response: Response =
                    if (followStatus == FollowStatus.UNFOLLOWED) {
                        client.newCall(GET("$baseUrl/ajax/actions.ajax.php?function=manga_unfollow&id=$mangaID&type=$mangaID", headers, CacheControl.FORCE_NETWORK))
                                .execute()
                    } else {

                        val status = followStatus.int
                        client.newCall(GET("$baseUrl/ajax/actions.ajax.php?function=manga_follow&id=$mangaID&type=$status", headers, CacheControl.FORCE_NETWORK))
                                .execute()
                    }

            response.body!!.string().isEmpty()
        }
    }


    suspend fun updateReadingProgress(track: Track): Boolean {
        return withContext(Dispatchers.IO) {
            val mangaID = getMangaId(track.tracking_url)
            val formBody = FormBody.Builder()
                    .add("chapter", track.last_chapter_read.toString())
            Timber.d("chapter to update %s", track.last_chapter_read.toString())
            val response = client.newCall(POST("$baseUrl/ajax/actions.ajax.php?function=edit_progress&&id=$mangaID", headers, formBody.build()))
                    .execute()

            response.body!!.string().isEmpty()
        }
    }


    /**
     * fetch all manga from all possible pages
     */
    suspend fun fetchAllFollows(): List<SManga> {
        return withContext(Dispatchers.IO) {
            val listManga = mutableListOf<SManga>()
            loop@ for (i in 1..10000) {
                val response = client.newCall(followsListRequest(i))
                        .execute()
                val mangasPage = followsParseMangaPage(response)

                if (mangasPage.mangas.isNotEmpty()) {
                    listManga.addAll(mangasPage.mangas)
                }
                if (!mangasPage.hasNextPage) {
                    break@loop
                }
            }
            listManga
        }
    }


    suspend fun fetchTrackingInfo(manga: SManga): Track {
        return withContext(Dispatchers.IO) {
            val request = GET("${MdUtil.baseUrl}${MdUtil.followsMangaApi}" + getMangaId(manga.url), headers, CacheControl.FORCE_NETWORK)
            val response = client.newCall(request).execute()
            val track = followStatusParse(response)
            track.tracking_url = MdUtil.baseUrl + manga.url
            track.title = manga.title
            track
        }
    }
}