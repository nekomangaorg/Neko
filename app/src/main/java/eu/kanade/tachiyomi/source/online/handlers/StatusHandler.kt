package eu.kanade.tachiyomi.source.online.handlers

import androidx.core.text.isDigitsOnly
import com.github.michaelbull.result.mapBoth
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.suspendOnFailure
import com.skydoves.sandwich.suspendOnSuccess
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.source.online.models.dto.MarkStatusDto
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class StatusHandler {
    val preferences: PreferencesHelper by injectLazy()
    private val authService: MangaDexAuthService by lazy { Injekt.get<NetworkHelper>().authService }

    suspend fun fetchReadingStatusForAllManga(): Map<String, String?> {
        return withContext(Dispatchers.IO) {
            return@withContext authService.readingStatusAllManga()
                .getOrResultError("getting reading status")
                .mapBoth(
                    success = {
                        it.statuses
                    },
                    failure = { emptyMap() },
                )
        }
    }

    /**
     * Mark a list of chapters as read or unread for a manga on MangaDex.  Defaults to marking read
     */
    suspend fun marksChaptersStatus(
        mangaId: String,
        chapterIds: List<String>,
        read: Boolean = true,
    ) {
        withIOContext {
            val dto = when (read) {
                true -> MarkStatusDto(chapterIdsRead = chapterIds)
                false -> MarkStatusDto(chapterIdsUnread = chapterIds)
            }
            authService.markStatusForMultipleChapters(mangaId, dto).onFailure {
                this.log("trying to mark chapters read=$read")
            }
        }
    }

    suspend fun getReadChapterIds(mangaId: String) = flow<Set<String>> {
        if (mangaId.isDigitsOnly()) {
            emit(emptySet())
        } else {
            val response = authService.readChaptersForManga(mangaId)
            response.suspendOnFailure {
                this.log("trying to get chapterIds")
                emit(emptySet())
            }.suspendOnSuccess {
                emit(this.data.data.toSet())
            }
        }
    }.flowOn(Dispatchers.IO)
}
