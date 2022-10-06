package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProxyRetrofitQueryMap
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.getOrResultError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class ListHandler {
    private val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }
    private val preferencesHelper: PreferencesHelper by injectLazy()

    suspend fun retrieveList(listUUID: String): Result<ListResults, ResultError> {
        return withContext(Dispatchers.IO) {
            service.viewList(listUUID)
                .getOrResultError("Error getting list")
                .andThen { listDto ->
                    val mangaIds = listDto.data.relationships.filter { it.type == MdConstants.Types.manga }.map { it.id }
                    when (mangaIds.isEmpty()) {
                        true -> Ok(ListResults(DisplayScreenType.List("", listUUID), persistentListOf()))
                        false -> {
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
                            val coverQuality = preferencesHelper.thumbnailQuality()
                            service.search(ProxyRetrofitQueryMap(queryParameters))
                                .getOrResultError("Error trying to load manga list")
                                .andThen { mangaListDto ->
                                    Ok(
                                        ListResults(
                                            displayScreenType = DisplayScreenType.List(listDto.data.attributes.name ?: "", listUUID),
                                            sourceManga = mangaListDto.data.map { it.toSourceManga(coverQuality) }.toImmutableList(),
                                        ),
                                    )
                                }
                        }
                    }
                }
        }
    }
}

data class ListResults(val displayScreenType: DisplayScreenType, val sourceManga: ImmutableList<SourceManga>)
