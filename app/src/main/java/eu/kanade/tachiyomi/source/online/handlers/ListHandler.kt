package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.skydoves.sandwich.getOrThrow
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.model.ResultListPage
import eu.kanade.tachiyomi.source.online.models.dto.ResultDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.getOrResultError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.SourceResult
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class ListHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val authService: MangaDexAuthorizedUserService by lazy { Injekt.get<NetworkHelper>().authService }

    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun retrieveList(listUUID: String, page: Int, privateList: Boolean): Result<ListResults, ResultError> {
        return withContext(Dispatchers.IO) {

            val currentService = when (privateList) {
                true -> authService
                false -> service
            }

            val enabledContentRatings = preferencesHelper.contentRatingSelections()
            val contentRatings = MangaContentRating.getOrdered().filter { enabledContentRatings.contains(it.key) }.map { it.key }
            val coverQuality = preferencesHelper.thumbnailQuality()

            val queryParameters =
                ProxyRetrofitQueryMap(
                    mutableMapOf(
                        MdConstants.SearchParameters.contentRatingParam to contentRatings,
                        MdConstants.SearchParameters.offset to MdUtil.getMangaListOffset(page),
                        MdConstants.SearchParameters.limit to MdConstants.Limits.manga,
                    ),
                )


            currentService.viewCustomListInfo(listUUID)
                .getOrResultError("Error getting list with UUID: $listUUID")
                .andThen { customListDto ->
                    val listName = customListDto.data.attributes.name

                    currentService.viewCustomListManga(listUUID, queryParameters)
                        .getOrResultError("Error getting list manga")
                        .andThen { mangaListDto ->
                            when (mangaListDto.data.isEmpty()) {
                                true -> Ok(ListResults(DisplayScreenType.List(listName, listUUID), persistentListOf(), false))
                                false -> {
                                    Ok(
                                        ListResults(
                                            displayScreenType = DisplayScreenType.List(listName, listUUID),
                                            sourceManga = mangaListDto.data.map { it.toSourceManga(coverQuality) }.toImmutableList(),
                                            hasNextPage = mangaListDto.limit + mangaListDto.offset < mangaListDto.total,
                                        ),
                                    )
                                }
                            }
                        }
                }
        }
    }

    suspend fun retrieveAllUserLists(): Result<ResultListPage, ResultError> {
        var hasPages = true
        var page = 1
        val list: MutableList<SourceResult> = mutableListOf()
        var resultError: ResultError? = null
        while (hasPages) {
            retrieveUserLists(page)
                .onFailure {
                    hasPages = false
                    resultError = it
                }
                .onSuccess {
                    page++
                    hasPages = it.hasNextPage
                    list += it.results
                }
        }
        return when (resultError != null) {
            true -> Err(resultError!!)
            false -> Ok(ResultListPage(false, list.toPersistentList()))
        }
    }

    suspend fun retrieveUserLists(page: Int): Result<ResultListPage, ResultError> {
        val offset = MdUtil.getLatestChapterListOffset(page)
        return authService.usersLists(offset, MdConstants.Limits.latest).getOrResultError("Error getting user's lists")
            .andThen { customListListDto ->
                when (customListListDto.data.isEmpty()) {
                    true -> Ok(ResultListPage(false, persistentListOf()))
                    false -> Ok(
                        ResultListPage(
                            hasNextPage = customListListDto.limit + customListListDto.offset < customListListDto.total,
                            customListListDto.data.map { customListDataDto ->
                                SourceResult(
                                    title = customListDataDto.attributes.name,
                                    information = customListDataDto.attributes.visibility,
                                    uuid = customListDataDto.id,
                                )
                            }.toPersistentList(),
                        ),
                    )
                }
            }
    }

    suspend fun addToCustomList(mangaId: String, listUUID: String): ResultDto {
        return authService.addToCustomList(mangaId, listUUID).getOrThrow()
    }

    suspend fun removeFromCustomList(mangaId: String, listUUID: String): ResultDto {
        return authService.removeFromCustomList(mangaId, listUUID).getOrThrow()
    }
}

data class ListResults(
    val displayScreenType: DisplayScreenType, val sourceManga: ImmutableList<SourceManga>, val hasNextPage: Boolean = false,
)
