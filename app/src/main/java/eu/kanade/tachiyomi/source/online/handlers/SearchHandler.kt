package eu.kanade.tachiyomi.source.online.handlers

import androidx.compose.ui.state.ToggleableState
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.ResultListPage
import eu.kanade.tachiyomi.source.online.models.dto.MangaListDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.lang.toResultError
import java.util.Calendar
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.ProxyRetrofitQueryMap
import org.nekomanga.domain.SourceResult
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SearchHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkServices>().service }
    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun searchForManga(mangaUUID: String): Result<MangaListPage, ResultError> {
        return service
            .viewManga(mangaUUID)
            .getOrResultError("trying to view manga with UUID $mangaUUID")
            .andThen { mangaDto ->
                val sourceManga =
                    persistentListOf(
                        mangaDto.data.toSourceManga(
                            preferencesHelper.thumbnailQuality().get(),
                            false,
                        )
                    )
                Ok(MangaListPage(sourceManga = sourceManga, hasNextPage = false))
            }
    }

    suspend fun searchForAuthor(authorQuery: String): Result<ResultListPage, ResultError> {
        return service
            .searchAuthor(query = authorQuery, limit = MdConstants.Limits.author)
            .getOrResultError("trying to search author $authorQuery")
            .andThen { authorListDto ->
                val results =
                    authorListDto.data
                        .map {
                            SourceResult(
                                title = it.attributes.name,
                                uuid = it.id,
                                information = it.attributes.biography.asMdMap<String>()["en"] ?: "",
                            )
                        }
                        .toImmutableList()
                Ok(ResultListPage(hasNextPage = false, results = results))
            }
    }

    suspend fun searchForGroup(groupQuery: String): Result<ResultListPage, ResultError> {
        return service
            .searchGroup(query = groupQuery, limit = MdConstants.Limits.group)
            .getOrResultError("trying to search group $groupQuery")
            .andThen { authorListDto ->
                val results =
                    authorListDto.data
                        .map {
                            SourceResult(
                                title = it.attributes.name,
                                uuid = it.id,
                                information = it.attributes.description ?: "",
                            )
                        }
                        .toImmutableList()
                Ok(ResultListPage(hasNextPage = false, results = results))
            }
    }

    suspend fun search(page: Int, filters: DexFilters): Result<MangaListPage, ResultError> {
        return withContext(Dispatchers.IO) {
            val queryParameters = mutableMapOf<String, Any>()

            queryParameters[MdConstants.SearchParameters.limit] = MdConstants.Limits.manga
            queryParameters[MdConstants.SearchParameters.offset] = (MdUtil.getMangaListOffset(page))
            val actualQuery = filters.query.text.replace(WHITESPACE_REGEX, " ")
            if (actualQuery.isNotBlank()) {
                queryParameters[MdConstants.SearchParameters.titleParam] = actualQuery
            }

            val contentRating = filters.contentRatings.filter { it.state }.map { it.rating.key }

            if (contentRating.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.contentRatingParam] = contentRating
            }

            val originalLanguage =
                filters.originalLanguage.filter { it.state }.map { it.language.lang }
            if (originalLanguage.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.originalLanguageParam] =
                    originalLanguage
            }

            val demographics =
                filters.publicationDemographics.filter { it.state }.map { it.demographic.key }
            if (demographics.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.publicationDemographicParam] =
                    demographics
            }

            val status = filters.statuses.filter { it.state }.map { it.status.key }
            if (status.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.statusParam] = status
            }
            val tagsToInclude =
                filters.tags.filter { it.state == ToggleableState.On }.map { it.tag.uuid }
            if (tagsToInclude.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.includedTagsParam] = tagsToInclude
            }

            val tagsToExclude =
                filters.tags
                    .filter { it.state == ToggleableState.Indeterminate }
                    .map { it.tag.uuid }
            if (tagsToExclude.isNotEmpty()) {
                queryParameters[MdConstants.SearchParameters.excludedTagsParam] = tagsToExclude
            }

            val sortMode = filters.sort.first { it.state }
            queryParameters[MdConstants.SearchParameters.sortParam(sortMode.sort.key)] =
                sortMode.sort.state.key

            if (filters.hasAvailableChapters.state) {
                queryParameters[MdConstants.SearchParameters.availableTranslatedLanguage] =
                    MdUtil.getLangsToShow(preferencesHelper)
            }

            queryParameters[MdConstants.SearchParameters.includedTagModeParam] =
                filters.tagInclusionMode.mode.key
            queryParameters[MdConstants.SearchParameters.excludedTagModeParam] =
                filters.tagExclusionMode.mode.key

            if (filters.authorId.uuid.isNotBlank()) {
                queryParameters[MdConstants.SearchParameters.authorOrArtist] = filters.authorId.uuid
            }

            if (filters.groupId.uuid.isNotBlank()) {
                queryParameters[MdConstants.SearchParameters.group] = filters.groupId.uuid
            }

            service
                .search(ProxyRetrofitQueryMap(queryParameters))
                .getOrResultError("Trying to search")
                .andThen { response ->
                    TimberKt.d { "Page: $page" }
                    response.data.forEach { TimberKt.d { "#mangaid: ${it.id}" } }
                    searchMangaParse(response)
                }
        }
    }

    suspend fun recentlyAdded(page: Int): Result<MangaListPage, ResultError> {
        return withContext(Dispatchers.IO) {
            val queryParameters = mutableMapOf<String, Any>()
            queryParameters[MdConstants.SearchParameters.limit] = MdConstants.Limits.manga
            queryParameters[MdConstants.SearchParameters.offset] = MdUtil.getMangaListOffset(page)
            val contentRatings = preferencesHelper.contentRatingSelections().get().toList()
            if (contentRatings.isNotEmpty()) {
                queryParameters["contentRating[]"] = contentRatings
            }
            val thumbQuality = preferencesHelper.thumbnailQuality().get()
            service
                .recentlyAdded(ProxyRetrofitQueryMap(queryParameters))
                .getOrResultError("Error getting recently added")
                .andThen { mangaListDto ->
                    val hasMoreResults =
                        mangaListDto.limit + mangaListDto.offset < mangaListDto.total

                    Ok(
                        MangaListPage(
                            hasNextPage = hasMoreResults,
                            sourceManga =
                                mangaListDto.data
                                    .distinctBy { it.id }
                                    .map { it.toSourceManga(thumbQuality) }
                                    .toImmutableList(),
                        )
                    )
                }
        }
    }

    suspend fun popularNewTitles(page: Int): Result<MangaListPage, ResultError> {
        return withContext(Dispatchers.IO) {
            val queryParameters = mutableMapOf<String, Any>()
            queryParameters["limit"] = MdConstants.Limits.manga
            val offset = MdUtil.getMangaListOffset(page)
            queryParameters["offset"] = offset
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -1)
            queryParameters["createdAtSince"] = MdUtil.apiDateFormat.format(calendar.time)
            val contentRatings = preferencesHelper.contentRatingSelections().get().toList()
            if (contentRatings.isNotEmpty()) {
                queryParameters["contentRating[]"] = contentRatings
            }
            val thumbQuality = preferencesHelper.thumbnailQuality().get()
            service
                .popularNewReleases(ProxyRetrofitQueryMap(queryParameters))
                .getOrResultError("Error getting recently added")
                .andThen { mangaListDto ->
                    val hasMoreResults =
                        mangaListDto.limit + mangaListDto.offset < mangaListDto.total

                    Ok(
                        MangaListPage(
                            hasNextPage = hasMoreResults,
                            sourceManga =
                                mangaListDto.data
                                    .mapIndexed { index, dto ->
                                        dto.toSourceManga(
                                            thumbQuality,
                                            displayText = "No. ${offset + index + 1}",
                                        )
                                    }
                                    .toImmutableList(),
                        )
                    )
                }
        }
    }

    private fun searchMangaParse(mangaListDto: MangaListDto): Result<MangaListPage, ResultError> {
        return com.github.michaelbull.result
            .runCatching {
                val hasMoreResults = mangaListDto.limit + mangaListDto.offset < mangaListDto.total
                val thumbQuality = preferencesHelper.thumbnailQuality().get()
                val mangaList =
                    mangaListDto.data.map { it.toSourceManga(thumbQuality) }.toImmutableList()
                MangaListPage(hasNextPage = hasMoreResults, sourceManga = mangaList)
            }
            .mapError {
                TimberKt.e(it) { "Error parsing search manga" }
                "error parsing search manga".toResultError()
            }
    }

    companion object {
        val WHITESPACE_REGEX = "\\s".toRegex()
    }
}
