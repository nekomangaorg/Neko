package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.system.logTimeTaken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LatestChapterHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun getPage(page: Int): MangaListPage {
        return withContext(Dispatchers.IO) {
            val limit = MdUtil.latestChapterLimit
            val offset = MdUtil.getLatestChapterListOffset(page)

            val langs = MdUtil.getLangsToShow(preferencesHelper)

            val response = logTimeTaken("fetching latest chapters from dex") {
                service.latestChapters(limit, offset, langs)
            }

            latestChapterParse(response)
        }
    }

    private suspend fun latestChapterParse(response: Response<ChapterListDto>): MangaListPage {
        if (response.isSuccessful.not()) {
            throw Exception("Error getting latest chapters http code: ${response.code()}")
        }

        val chapterListDto = response.body()!!

        val mangaIds = chapterListDto.results.map { it.relationships }.flatten()
            .filter { it.type == MdConstants.Types.manga }.map { it.id }.distinct()

        val queryParamters = mutableMapOf("ids[]" to mangaIds, "limit" to mangaIds.size)

        val mangaListResponse = service.search(ProxyRetrofitQueryMap(queryParamters))

        val hasMoreResults = chapterListDto.limit + chapterListDto.offset < chapterListDto.total

        val mangaList = mangaListResponse.body()!!.results.map {
            it.toBasicManga()
        }

        return MangaListPage(mangaList, hasMoreResults)
    }
}
