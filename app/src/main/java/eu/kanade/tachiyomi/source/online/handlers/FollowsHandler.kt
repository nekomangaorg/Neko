package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.baseUrl
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaUUID
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class FollowsHandler {

    val preferences: PreferencesHelper by injectLazy()
    val statusHandler: StatusHandler by injectLazy()
    private val authService: MangaDexAuthService by lazy { Injekt.get<NetworkHelper>().authService }

    /**
     * fetch all follows
     */
    suspend fun fetchAllFollows(): Result<Map<Int, List<SourceManga>>, ResultError> {
        return withContext(Dispatchers.IO) {
            return@withContext runCatching {
                val readingFuture = async {
                    statusHandler.fetchReadingStatusForAllManga()
                }

                fetchOffset(0).andThen { mangaListDto ->
                    Ok(
                        when (mangaListDto.total > mangaListDto.limit) {
                            true -> fetchRestOfFollows(mangaListDto.limit, mangaListDto.total) + mangaListDto
                            false -> listOf(mangaListDto)
                        }.map { it.data }.flatten(),
                    )
                }
                    .andThen { allResults ->
                        Ok(allFollowsParser(allResults, readingFuture.await()))
                    }
            }.getOrElse {
                XLog.e("Error fetching all follows", it)
                Err(ResultError.Generic("Unknown error fetching all follows"))
            }
        }
    }

    private suspend fun fetchRestOfFollows(limit: Int, total: Int): List<MangaListDto> {
        return withContext(Dispatchers.IO) {
            val totalRequestNo = (total / limit)

            (1..totalRequestNo).map { pos ->
                async {
                    fetchOffset(pos * limit)
                }
            }.awaitAll().mapNotNull { it.getOrElse { null } }
        }
    }

    private suspend fun fetchOffset(offset: Int): Result<MangaListDto, ResultError> {
        return authService.userFollowList(offset).getOrResultError("Failed to get follows")
    }

    private fun allFollowsParser(mangaDataDtoList: List<MangaDataDto>, readingStatusMap: Map<String, String?>): Map<Int, List<SourceManga>> {
        val coverQuality = preferences.thumbnailQuality()
        return mangaDataDtoList.asSequence().map {
            val followStatus = FollowStatus.fromDex(readingStatusMap[it.id])
            followStatus to it.toSourceManga(coverQuality)
        }
            .sortedBy { it.second.title }
            .groupBy({ it.first.int }, { it.second })
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

            return@withContext when (
                val response =
                    authService.updateReadingStatusForManga(mangaId, readingStatusDto)
            ) {
                is ApiResponse.Failure.Error<*>, is ApiResponse.Failure.Exception<*> -> {
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
        return withContext(Dispatchers.IO) {
            val mangaID = getMangaUUID(track.tracking_url)
            val response = if (track.score == 0f) {
                authService.removeRating(mangaID)
            } else {
                authService.updateRating(mangaID, RatingDto(track.score.toInt()))
            }

            response.getOrNull()?.result == "ok"
        }
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        return withContext(Dispatchers.IO) {
            val mangaUUID = getMangaUUID(url)
            val readingStatusResponse = authService.readingStatusForManga(mangaUUID)
            val ratingResponse = authService.retrieveRating(mangaUUID)

            readingStatusResponse.onFailure {
                this.log("trying to fetch reading status for $mangaUUID")
                throw Exception("error trying to get tracking info")
            }
            val followStatus =
                FollowStatus.fromDex(readingStatusResponse.getOrThrow().status)
            val rating =
                ratingResponse.getOrThrow().ratings.asMdMap<RatingDto>()[mangaUUID]
            val track = Track.create(TrackManager.MDLIST).apply {
                status = followStatus.int
                tracking_url = "$baseUrl/title/$mangaUUID"
                score = rating?.rating?.toFloat() ?: 0f
            }
            return@withContext track
        }
    }
}
