package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.getOrNull
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.online.models.dto.RatingDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil.Companion.getMangaUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class RatingHandler {

    val preferences: PreferencesHelper by injectLazy()
    private val authService: MangaDexAuthorizedUserService by lazy {
        Injekt.get<NetworkServices>().authService
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
}
