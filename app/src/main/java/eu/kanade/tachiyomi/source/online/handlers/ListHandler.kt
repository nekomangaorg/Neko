package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.getOrResultError
import kotlin.math.min
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.ProxyRetrofitQueryMap
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

// If the List Changes ever go live this entire file can be overridden by that branch
class ListHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkServices>().service }
    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun retrieveMangaFromList(
        listUUID: String,
        page: Int,
        privateList: Boolean = false,
        useDefaultContentRating: Boolean = false,
    ): Result<ListResults, ResultError> {
        return withContext(Dispatchers.IO) {
            service.viewList(listUUID).getOrResultError("Error getting list").andThen { listDto ->
                val allMangaIds =
                    listDto.data.relationships
                        .filter { it.type == MdConstants.Types.manga }
                        .map { it.id }
                when (
                    allMangaIds.isEmpty() || allMangaIds.size <= MdUtil.getMangaListOffset(page)
                ) {
                    true ->
                        Ok(ListResults(DisplayScreenType.List("", listUUID), persistentListOf()))
                    false -> {
                        val mangaIds =
                            when (allMangaIds.size < MdConstants.Limits.manga) {
                                true -> allMangaIds
                                false -> {

                                    val initial = MdUtil.getMangaListOffset(page)
                                    val potentialMax =
                                        MdUtil.getMangaListOffset(page) + MdConstants.Limits.manga
                                    val end = min(potentialMax, allMangaIds.size - 1)

                                    allMangaIds.subList(initial, end)
                                }
                            }

                        val enabledContentRatings =
                            preferencesHelper.contentRatingSelections().get()
                        val contentRatings =
                            MangaContentRating.getOrdered()
                                .filter { enabledContentRatings.contains(it.key) }
                                .map { it.key }

                        val queryParameters =
                            mutableMapOf(
                                MdConstants.SearchParameters.mangaIds to mangaIds,
                                MdConstants.SearchParameters.offset to 0,
                                MdConstants.SearchParameters.limit to MdConstants.Limits.manga,
                                MdConstants.SearchParameters.contentRatingParam to contentRatings,
                            )
                        val coverQuality = preferencesHelper.thumbnailQuality().get()
                        service
                            .search(ProxyRetrofitQueryMap(queryParameters))
                            .getOrResultError("Error trying to load manga list")
                            .andThen { mangaListDto ->
                                Ok(
                                    ListResults(
                                        displayScreenType =
                                            DisplayScreenType.List(
                                                listDto.data.attributes.name ?: "",
                                                listUUID,
                                            ),
                                        sourceManga =
                                            mangaListDto.data
                                                .map { it.toSourceManga(coverQuality) }
                                                .toImmutableList(),
                                        hasNextPage =
                                            (mangaListDto.limit + mangaListDto.offset) <
                                                listDto.data.relationships.size,
                                    )
                                )
                            }
                    }
                }
            }
        }
    }

    suspend fun retrieveAllMangaFromList(
        listUUID: String,
        privateList: Boolean,
    ): Result<ListResults, ResultError> {
        var hasPages = true
        var page = 1
        var list: List<SourceManga> = listOf()
        var displayScreenType: DisplayScreenType? = null
        var resultError: ResultError? = null
        while (hasPages) {
            retrieveMangaFromList(listUUID, page, privateList, false)
                .onFailure {
                    hasPages = false
                    resultError = it
                }
                .onSuccess { successResult ->
                    page++
                    hasPages = successResult.hasNextPage
                    list = list + (successResult.sourceManga.toMutableList())
                    displayScreenType = successResult.displayScreenType
                }
        }

        return when (resultError != null) {
            true -> Err(resultError!!)
            false ->
                Ok(
                    ListResults(
                        displayScreenType = displayScreenType!!,
                        sourceManga = list.toImmutableList(),
                    )
                )
        }
    }
}

data class ListResults(
    val displayScreenType: DisplayScreenType,
    val sourceManga: ImmutableList<SourceManga>,
    val hasNextPage: Boolean = false,
)
