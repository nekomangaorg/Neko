package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.models.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toBasicManga
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.throws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LatestChapterHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val preferencesHelper: PreferencesHelper by injectLazy()

    private val uniqueManga = mutableSetOf<String>()

    suspend fun getPage(page: Int): MangaListPage {
        if (page == 1) uniqueManga.clear()
        return withContext(Dispatchers.IO) {
            val limit = MdUtil.latestChapterLimit
            val offset = MdUtil.getLatestChapterListOffset(page)

            val langs = MdUtil.getLangsToShow(preferencesHelper)

            val contentRatings = preferencesHelper.contentRatingSelections().toList()

            val response = logTimeTaken("fetching latest chapters from dex") {
                service.latestChapters(limit, offset, langs, contentRatings).onError {
                    val type = "getting latest chapters"
                    this.log(type)
                    this.throws(type)
                }.onException {
                    val type = "getting latest chapters"
                    this.log(type)
                    this.throws(type)
                }.getOrThrow()
            }

            latestChapterParse(response)
        }
    }

    private suspend fun latestChapterParse(chapterListDto: ChapterListDto): MangaListPage {

        val mangaIds = chapterListDto.data.asSequence().map { it.relationships }.flatten()
            .filter { it.type == MdConstants.Types.manga }.map { it.id }.distinct()
            .filter { uniqueManga.contains(it).not() }.toList()

        uniqueManga.addAll(mangaIds)

        val allContentRating = listOf(MdConstants.ContentRating.safe,
            MdConstants.ContentRating.suggestive,
            MdConstants.ContentRating.erotica,
            MdConstants.ContentRating.pornographic)

        val queryParameters =
            mutableMapOf("ids[]" to mangaIds,
                "limit" to mangaIds.size,
                "contentRating[]" to allContentRating)

        val mangaListDto = service.search(ProxyRetrofitQueryMap(queryParameters)).onError {
            val type = "trying to search manga from latest chapters"
            this.log(type)
            this.throws(type)
        }.onException {
            val type = "trying to search manga from latest chapters"
            this.log(type)
            this.throws(type)
        }.getOrThrow()

        val hasMoreResults = chapterListDto.limit + chapterListDto.offset < chapterListDto.total

        val mangaDtoMap = mangaListDto.data.associateBy({ it.id }, { it })

        val thumbQuality = preferencesHelper.thumbnailQuality()
        val mangaList = mangaIds.mapNotNull { mangaDtoMap[it] }.map {
            it.toBasicManga(thumbQuality)
        }

        return MangaListPage(mangaList, hasMoreResults)
    }
}
