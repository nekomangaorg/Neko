package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import uy.kohesive.injekt.injectLazy

class StatusHandler {
    val preferences: PreferencesHelper by injectLazy()
    val network: NetworkHelper by injectLazy()

    suspend fun fetchMangaWithStatus(statusList: List<FollowStatus>) = flow<List<SManga>> {
        coroutineScope {
            val mangaKeys = statusList.map { it.toDex() }.map { status ->
                async {
                    network.authService.readingStatus(status)
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
    }.flowOn(Dispatchers.IO)
}