package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.MangaDto
import eu.kanade.tachiyomi.source.online.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaId
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class FollowsHandler {

    val preferences: PreferencesHelper by injectLazy()
    val network: NetworkHelper by injectLazy()
    val service: MangaDexAuthService by lazy { Injekt.get<NetworkHelper>().authService }

    /**
     * fetch all follows
     */
    suspend fun fetchFollows(): MangaListPage {
        return withContext(Dispatchers.IO) {
            val readingFuture = async { service.readingStatusAllManga().body()!!.statuses }

            val response = async { service.userFollowList(0) }
            val mangaListDto = response.await().body()!!
            val results = mangaListDto.results.toMutableList()

            if (mangaListDto.total > mangaListDto.limit) {
                val totalRequestNo = (mangaListDto.total / mangaListDto.limit)
                val restOfResults = (1..totalRequestNo).map { pos ->
                    async {
                        val newResponse = service.userFollowList(pos * mangaListDto.limit)
                        newResponse.body()!!.results
                    }
                }.awaitAll().flatten()
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

    /**
     * Change the status of a manga
     */
    suspend fun updateFollowStatus(mangaId: String, followStatus: FollowStatus): Boolean {
        return withContext(Dispatchers.IO) {
            val status = when (followStatus == FollowStatus.UNFOLLOWED) {
                true -> null
                false -> followStatus.toDex()
            }
            val readingStatusDto = ReadingStatusDto(status)

            try {
                withIOContext {
                    if (followStatus == FollowStatus.UNFOLLOWED) {
                        network.authService.unfollowManga(mangaId)
                    } else {
                        network.authService.followManga(mangaId)
                    }
                }

                val response =
                    network.authService.updateReadingStatusForManga(mangaId, readingStatusDto)

                response.isSuccessful
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
        return fetchFollows().manga
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        return withContext(Dispatchers.IO) {
            val mangaId = getMangaId(url)
            val response = network.authService.readingStatusForManga(mangaId)

            val followStatus = FollowStatus.fromDex(response.body()!!.status)
            val track = Track.create(TrackManager.MDLIST).apply {
                status = followStatus.int
                tracking_url = "$baseUrl/title/$mangaId"
            }
            /* if (follow.chapter.isNotBlank()) {
                track.last_chapter_read = floor(follow.chapter.toFloat()).toInt()
            } */
            track
        }
    }
}
