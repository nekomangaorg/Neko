package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.getOrResultError
import kotlin.math.min
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.ProxyRetrofitQueryMap
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class AuthorHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkServices>().service }
    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    suspend fun retrieveMangaFromAuthor(
        authorUuid: String,
        page: Int,
    ): Result<ListResults, ResultError> {
        return withContext(Dispatchers.IO) {
            service.authorByUuid(authorUuid).getOrResultError("Error getting list").andThen {
                authorDto ->
                val allMangaIds = authorDto.data.relationships?.map { it.id } ?: emptyList()
                when (
                    allMangaIds.isEmpty() || allMangaIds.size <= MdUtil.getMangaListOffset(page)
                ) {
                    true ->
                        Ok(
                            ListResults(
                                DisplayScreenType.AuthorWithUuid(
                                    UiText.String(authorDto.data.attributes.name),
                                    authorUuid,
                                ),
                                persistentListOf(),
                            )
                        )
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
                            mangaDexPreferences.visibleContentRatings().get()
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
                        val coverQuality = mangaDexPreferences.coverQuality().get()
                        service
                            .search(ProxyRetrofitQueryMap(queryParameters))
                            .getOrResultError("Error trying to load manga list")
                            .andThen { mangaListDto ->
                                val title = authorDto.data.attributes.name
                                Ok(
                                    ListResults(
                                        displayScreenType =
                                            DisplayScreenType.AuthorWithUuid(
                                                UiText.String(title),
                                                authorUuid,
                                            ),
                                        sourceManga =
                                            mangaListDto.data
                                                .map { it.toSourceManga(coverQuality) }
                                                .toPersistentList(),
                                        hasNextPage =
                                            (mangaListDto.limit + mangaListDto.offset) <
                                                (authorDto.data.relationships?.size ?: 0),
                                    )
                                )
                            }
                    }
                }
            }
        }
    }
}
