package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.suspendOnFailure
import com.skydoves.sandwich.suspendOnSuccess
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
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
    val authService: MangaDexAuthService by lazy { Injekt.get<NetworkHelper>().authService }

    suspend fun fetchReadingStatusForAllManga(): Map<String, String?> {
        return withContext(Dispatchers.IO) {
            return@withContext when (val response = authService.readingStatusAllManga()) {
                is ApiResponse.Failure<*> -> {
                    response.log("getting reading status")
                    emptyMap()
                }
                else -> response.getOrThrow().statuses

            }
        }
    }

    /* suspend fun fetchMangaWithStatus(statusList: List<FollowStatus>) = flow<List<SManga>> {
         coroutineScope {
             val mangaKeys = statusList.map { it.toDex() }.map { status ->
                 async {
                     network.authService.readingStatusByType(status)
                 }
             }.awaitAll().map {
                 it.body()!!
             }.map {
                 it.statuses.keys
             }.flatten()

             val mangaList = mangaKeys.chunked(100)
                 .map { keys ->
                     async {
                         val map = mutableMapOf<String, Any>("ids[]" to keys)
                         network.service.search(ProxyRetrofitQueryMap(map))
                     }
                 }.awaitAll()
                 .map {
                     it.body()!!
                 }.map {
                     it.results.map { mangaDto ->
                         mangaDto.toBasicManga()
                     }
                 }.flatten()

             emit(mangaList)

         }
     }.flowOn(Dispatchers.IO)*/

    suspend fun markChapterRead(chapterId: String) {
        withIOContext {
            authService.markChapterRead(chapterId).onFailure {
                this.log("trying to mark chapter read")
            }
        }
    }

    suspend fun markChapterUnRead(chapterId: String) {
        withIOContext {
            authService.markChapterUnRead(chapterId).onFailure {
                this.log("trying to mark chapter unread")
            }
        }
    }

    suspend fun getReadChapterIds(mangaId: String) = flow<Set<String>> {

        val response = authService.readChaptersForManga(mangaId)
        response.suspendOnFailure {
            this.log("trying to get chapterIds")
            emit(emptySet())
        }.suspendOnSuccess {
            emit(this.data.data.toSet())
        }
    }.flowOn(Dispatchers.IO)
}
