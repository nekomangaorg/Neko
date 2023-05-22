package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.model.ResultListPage
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

    suspend fun retrieveList(listUUID: String, privateList: Boolean): Result<ListResults, ResultError> {
        return withContext(Dispatchers.IO) {

            val enabledContentRatings = preferencesHelper.contentRatingSelections()
            val contentRatings = MangaContentRating.getOrdered().filter { enabledContentRatings.contains(it.key) }.map { it.key }
            val coverQuality = preferencesHelper.thumbnailQuality()

            val queryParameters =
                mutableMapOf(
                    "contentRating[]" to contentRatings,
                    "offset" to 0,
                )

            when (privateList) {
                true -> authService.viewCustomListInfo(listUUID)
                false -> service.viewCustomListInfo(listUUID)
            }
                .getOrResultError("Error getting list with UUID: $listUUID")
                .andThen { customListDto ->
                    val listName = customListDto.data.attributes.name

                    when (privateList) {
                        true -> authService.viewCustomListManga(listUUID, 0)
                        false -> service.viewCustomListManga(listUUID, 0)
                    }
                        .getOrResultError("Error getting list manga")
                        .andThen { mangaListDto ->
                            when (mangaListDto.data.isEmpty()) {
                                true -> Ok(ListResults(DisplayScreenType.List(listName, listUUID), persistentListOf()))
                                false -> {
                                    Ok(
                                        ListResults(
                                            displayScreenType = DisplayScreenType.List(listName, listUUID),
                                            sourceManga = mangaListDto.data.map { it.toSourceManga(coverQuality) }.toImmutableList(),
                                        ),
                                    )
                                }
                            }
                        }
                }
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
}

data class ListResults(val displayScreenType: DisplayScreenType, val sourceManga: ImmutableList<SourceManga>)
