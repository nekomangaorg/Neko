package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.retrofit.statusCode
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaUUID
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.lang.toDisplayMessage
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class FollowsHandler {

    val mangaDexPreferences: MangaDexPreferences by injectLazy()
    private val statusHandler: StatusHandler by injectLazy()
    private val authService: MangaDexAuthorizedUserService by lazy {
        Injekt.get<NetworkServices>().authService
    }

    /** fetch all follows */
    suspend fun fetchAllFollows(): Result<List<SourceManga>, ResultError> {
        return withContext(Dispatchers.IO) {
            return@withContext runCatching {
                val readingFuture = async { statusHandler.fetchReadingStatusForAllManga() }

                fetchOffset(0)
                    .andThen { mangaListDto ->
                        Ok(
                            when (mangaListDto.total > mangaListDto.limit) {
                                    true ->
                                        fetchRestOfFollows(
                                            mangaListDto.limit,
                                            mangaListDto.total,
                                        ) + mangaListDto
                                    false -> listOf(mangaListDto)
                                }
                                .map { it.data }
                                .flatten()
                        )
                    }
                    .andThen { allResults ->
                        Ok(allFollowsParser(allResults, readingFuture.await()))
                    }
            }
                .getOrElse {
                    TimberKt.e(it) { "Error fetching all follows" }
                    Err(ResultError.Generic(it.toDisplayMessage()))
                }
        }
    }

    private suspend fun fetchRestOfFollows(limit: Int, total: Int): List<MangaListDto> {
        return withContext(Dispatchers.IO) {
            val totalRequestNo = (total / limit)

            (1..totalRequestNo)
                .map { pos -> async { fetchOffset(pos * limit) } }
                .awaitAll()
                .mapNotNull { it.getOrElse { null } }
        }
    }

    private suspend fun fetchOffset(offset: Int): Result<MangaListDto, ResultError> {
        return authService.userFollowList(offset).getOrResultError("Failed to get follows")
    }

    private fun allFollowsParser(
        mangaDataDtoList: List<MangaDataDto>,
        readingStatusMap: Map<String, String?>,
    ): List<SourceManga> {
        val coverQuality = mangaDexPreferences.coverQuality().get()
        return mangaDataDtoList
            .asSequence()
            .map {
                val followStatus = FollowStatus.fromDex(readingStatusMap[it.id])
                it.toSourceManga(coverQuality, displayTextRes = followStatus.stringRes)
            }
            .sortedBy { it.title }
            .toList()
    }

    /**
     * Change the status of a manga. Returns true only when MangaDex ends up with the wanted follow
     * and the new status.
     */
    suspend fun updateFollowStatus(mangaId: String, followStatus: FollowStatus): Boolean {
        return withContext(Dispatchers.IO) {
            val status =
                when (followStatus == FollowStatus.UNFOLLOWED) {
                    true -> null
                    false -> followStatus.toDex()
                }
            val readingStatusDto = ReadingStatusDto(status)
            val unfollow =
                followStatus == FollowStatus.UNFOLLOWED || followStatus == FollowStatus.DROPPED

            val followResponse = withIOContext {
                if (unfollow) {
                    authService.unfollowManga(mangaId).onFailure {
                        this.log("trying to unfollow manga $mangaId")
                    }
                } else {
                    authService.followManga(mangaId).onFailure {
                        this.log("trying to follow manga $mangaId")
                    }
                }
            }

            // The API docs list a 404 for unfollow without saying when it happens, so after a
            // failed request the follow list decides whether the status is sent
            if (followResponse !is ApiResponse.Success && !followListMatches(mangaId, !unfollow)) {
                return@withContext false
            }

            return@withContext when (
                val response = authService.updateReadingStatusForManga(mangaId, readingStatusDto)
            ) {
                is ApiResponse.Failure.Error,
                is ApiResponse.Failure.Exception -> {
                    response.log("trying to update reading status for manga $mangaId")
                    false
                }
                else -> true
            }
        }
    }

    /**
     * Whether MangaDex's follow list has the manga when [followed] is true, or lacks it when false.
     * A failed check counts as no match.
     */
    private suspend fun followListMatches(mangaId: String, followed: Boolean): Boolean {
        val response = authService.userFollowsManga(mangaId)
        return when {
            response is ApiResponse.Success -> followed
            response is ApiResponse.Failure.Error && response.statusCode.code == 404 -> !followed
            else -> {
                response.log("trying to check the follow of manga $mangaId")
                false
            }
        }
    }

    suspend fun updateReadingProgress(track: Track): Boolean {
        return true
    }

    suspend fun updateRating(track: Track): Boolean {
        return withContext(Dispatchers.IO) {
            val mangaID = getMangaUUID(track.tracking_url)
            val response =
                if (track.score == 0f) {
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
            val followStatus = FollowStatus.fromDex(readingStatusResponse.getOrThrow().status)
            val rating = ratingResponse.getOrThrow().ratings.asMdMap<RatingDto>()[mangaUUID]
            val track =
                Track.create(TrackManager.MDLIST).apply {
                    status = followStatus.int
                    tracking_url = "${MdConstants.baseUrl}/title/$mangaUUID"
                    score = rating?.rating?.toFloat() ?: 0f
                }
            return@withContext track
        }
    }
}
