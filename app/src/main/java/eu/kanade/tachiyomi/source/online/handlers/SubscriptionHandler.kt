package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.models.dto.ReadingStatusDto
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaUUID
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SubscriptionHandler {

    val preferences: PreferencesHelper by injectLazy()
    private val authService: MangaDexAuthorizedUserService by lazy { Injekt.get<NetworkHelper>().authService }

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
}
