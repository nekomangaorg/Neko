package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.suspendOnFailure
import com.skydoves.sandwich.suspendOnSuccess
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaId
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.log
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
    val statusHandler: StatusHandler by injectLazy()
    val authService: MangaDexAuthService by lazy { Injekt.get<NetworkHelper>().authService }

    /**
     * fetch all follows
     */
    suspend fun fetchFollows(): MangaListPage {
        return withContext(Dispatchers.IO) {
            val readingFuture = async {
                statusHandler.fetchReadingStatusForAllManga()
            }

            val results = mutableListOf<MangaDataDto>()
            val response = authService.userFollowList(0)
            response.suspendOnSuccess {
                val mangaListDto = this.data
                results.addAll(mangaListDto.data.toMutableList())

                if (mangaListDto.total > mangaListDto.limit) {
                    val totalRequestNo = (mangaListDto.total / mangaListDto.limit)
                    val restOfResults = (1..totalRequestNo).map { pos ->
                        async {
                            authService.userFollowList(pos * mangaListDto.limit)
                                .getOrNull()?.data ?: emptyList()
                        }
                    }.awaitAll().flatten()
                    results.addAll(restOfResults)
                    val readingStatusResponse = readingFuture.await()

                    followsParseMangaPage(results, readingStatusResponse)
                }
            }.suspendOnFailure {
                this.log("getting follows")
                throw Exception("Failure to get follows")
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
        mangaDataDtoList: List<MangaDataDto>,
        readingStatusMap: Map<String, String?>,
    ): MangaListPage {
        val comparator = compareBy<SManga> { it.follow_status }.thenBy { it.title }

        val result = mangaDataDtoList.map { mangaDto ->
            mangaDto.toBasicManga(preferences.thumbnailQuality()).apply {
                this.follow_status = FollowStatus.fromDex(readingStatusMap[mangaDto.id])
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

            withIOContext {
                if (followStatus == FollowStatus.UNFOLLOWED) {
                    authService.unfollowManga(mangaId).onFailure {
                        this.log("trying to unfollow manga $mangaId")
                    }
                } else {
                    authService.followManga(mangaId).onFailure {
                        this.log("trying to follow manga $mangaId")
                    }
                }
            }

            return@withContext when (val response =
                authService.updateReadingStatusForManga(mangaId, readingStatusDto)) {
                is ApiResponse.Failure<*> -> {
                    response.log("trying to update reading status for manga $mangaId")
                    false
                }
                else -> true
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
            when (val response = authService.readingStatusForManga(mangaId)) {
                is ApiResponse.Failure<*> -> {
                    response.log("trying to fetch tracking info")
                    throw Exception("error trying to get tracking info")
                }
                else -> {
                    val followStatus = FollowStatus.fromDex(response.getOrThrow().status)
                    val track = Track.create(TrackManager.MDLIST).apply {
                        status = followStatus.int
                        tracking_url = "$baseUrl/title/$mangaId"
                    }
                    return@withContext track
                }
            }
        }
    }
}
