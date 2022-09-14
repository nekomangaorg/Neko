package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.isError
import com.skydoves.sandwich.isSuccess
import com.skydoves.sandwich.onFailure
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
import eu.kanade.tachiyomi.util.throws
import eu.kanade.tachiyomi.util.toResultError
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LatestChapterHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val preferencesHelper: PreferencesHelper by injectLazy()

    private val uniqueManga = mutableSetOf<String>()

    suspend fun getPage(page: Int): Result<MangaListPage, ResultError> {
        if (page == 1) uniqueManga.clear()
        return withContext(Dispatchers.IO) {
            val limit = MdUtil.latestChapterLimit
            val offset = MdUtil.getLatestChapterListOffset(page)

            val langs = MdUtil.getLangsToShow(preferencesHelper)

            val contentRatings = preferencesHelper.contentRatingSelections().toList()

            val response = service.latestChapters(limit, offset, langs, contentRatings)

            when (response.isSuccess) {
                true -> latestChapterParse(response.getOrNull()!!)
                false -> {
                    val errorType = "getting latest chapters"
                    Err(
                        when (response.isError) {
                            true -> (response as ApiResponse.Failure.Error).toResultError(errorType)
                            false -> (response as ApiResponse.Failure.Exception).toResultError(errorType)
                        },
                    )
                }
            }
        }
    }

    private suspend fun latestChapterParse(chapterListDto: ChapterListDto): Result<MangaListPage, ResultError> {
        return runCatching {
            val mangaIds = chapterListDto.data.asSequence().map { it.relationships }.flatten()
                .filter { it.type == MdConstants.Types.manga }.map { it.id }.distinct()
                .filter { !uniqueManga.contains(it) }.toList()

            uniqueManga.addAll(mangaIds)

            val allContentRating = listOf(
                MdConstants.ContentRating.safe,
                MdConstants.ContentRating.suggestive,
                MdConstants.ContentRating.erotica,
                MdConstants.ContentRating.pornographic,
            )

            val queryParameters =
                mutableMapOf(
                    "ids[]" to mangaIds,
                    "limit" to mangaIds.size,
                    "contentRating[]" to allContentRating,
                )

            val mangaListDto = service.search(ProxyRetrofitQueryMap(queryParameters)).onFailure {
                val type = "trying to search manga from latest chapters"
                this.log(type)
                this.throws(type)
            }.getOrThrow()

            val hasMoreResults = chapterListDto.limit + chapterListDto.offset < chapterListDto.total

            val mangaDtoMap = mangaListDto.data.associateBy({ it.id }, { it })

            val thumbQuality = preferencesHelper.thumbnailQuality()
            val mangaList = mangaIds.mapNotNull { mangaDtoMap[it] }.map {
                it.toBasicManga(thumbQuality)
            }.map {
                SourceManga(
                    title = it.originalTitle,
                    url = it.url,
                    currentThumbnail = it.thumbnail_url ?: MdConstants.noCoverUrl,
                )
            }

            Ok(MangaListPage(sourceManga = mangaList.toImmutableList(), hasNextPage = hasMoreResults))
        }.getOrElse {
            XLog.e("Error parsing latest chapters", it)
            Err(ResultError.Generic(errorString = "Error parsing latest chapters response"))
        }
    }
}
