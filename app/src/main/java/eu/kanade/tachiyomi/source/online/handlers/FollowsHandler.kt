package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.FollowsPageResult
import eu.kanade.tachiyomi.source.online.handlers.serializers.Result
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable

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
    private fun followStatusParse(response: Response): SManga.FollowStatus {
        val followsPageResult = Json.nonstrict.parse(FollowsPageResult.serializer(), response.body!!.string())
        if (followsPageResult.result.isEmpty()) {
            return SManga.FollowStatus.UNFOLLOWED
        } else {
            val result = followsPageResult.result[0]
            return SManga.FollowStatus.fromMangadex(result.follow_type)
        }
    }

    /**build Request for follows page
     *
     */
    private fun followsListRequest(page: Int): Request {

        val url = "${MdUtil.baseUrl}$followsAllApi".toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("page", page.toString())

        return GET(url.toString(), headers)
    }

    /**
     * Parse result element  to manga
     */
    private fun followFromElement(result: Result): SManga {
        val manga = SManga.create()
        manga.title = MdUtil.cleanString(result.title)
        manga.thumbnail_url = "${MdUtil.cdnUrl}/images/manga/${result.manga_id}.jpg"
        manga.url = "/manga/${result.manga_id}/"
        manga.follow_status = SManga.FollowStatus.fromMangadex(result.follow_type)
        return manga
    }

    /**
     * Change the status of a manga
     */
    suspend fun changeFollowStatus(manga: SManga, followStatus: SManga.FollowStatus): Boolean {
        return withContext(Dispatchers.IO) {
            manga.follow_status ?: throw IllegalArgumentException("Cannot tell MD server to set an null follow status")

            val mangaID = getMangaId(manga.url)
            val status = followStatus.toMangadexInt()

            val response = client.newCall(GET("$baseUrl/ajax/actions.ajax.php?function=manga_follow&id=$mangaID&type=$status", headers))
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

    /**
     * fetch individual follow status
     */
    suspend fun fetchMangaFollowStatus(manga: SManga): SManga.FollowStatus {
        return withContext(Dispatchers.IO) {
            val request = GET("${MdUtil.baseUrl}$followsMangaApi" + getMangaId(manga.url), headers)
            val response = client.newCall(request).execute()
            followStatusParse(response)
        }
    }


    companion object {
        const val followsAllApi = "/api/?type=manga_follows"
        const val followsMangaApi = "/api/?type=manga_follows&manga_id="
        private val FOLLOW_STATUS_LIST = listOf(
                Triple(0, SManga.FollowStatus.UNFOLLOWED, "Unfollowed"),
                Triple(1, SManga.FollowStatus.READING, "Reading"),
                Triple(2, SManga.FollowStatus.COMPLETED, "Completed"),
                Triple(3, SManga.FollowStatus.ON_HOLD, "On hold"),
                Triple(4, SManga.FollowStatus.PLAN_TO_READ, "Plan to read"),
                Triple(5, SManga.FollowStatus.DROPPED, "Dropped"),
                Triple(6, SManga.FollowStatus.RE_READING, "Re-reading"))

        fun SManga.FollowStatus.Companion.fromMangadex(x: Int) = FOLLOW_STATUS_LIST.first { it.first == x }.second
        fun SManga.FollowStatus.Companion.fromMangadex(MangadexFollowString: String) = FOLLOW_STATUS_LIST.first { it.third == MangadexFollowString }.second
        fun SManga.FollowStatus.toMangadexInt() = FOLLOW_STATUS_LIST.first { it.second == this }.first
        fun SManga.FollowStatus.toMangadexString() = FOLLOW_STATUS_LIST.first { it.second == this }.third
    }
}