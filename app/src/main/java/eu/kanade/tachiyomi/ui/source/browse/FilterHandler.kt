package eu.kanade.tachiyomi.ui.source.browse

import androidx.compose.ui.state.ToggleableState
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.source.online.utils.MdSort
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.filter.QueryType
import org.nekomanga.domain.manga.MangaContentRating

class FilterHandler(
    private val scope: CoroutineScope,
    private val browseRepository: BrowseRepository,
    private val db: DatabaseHelper,
    private val state: MutableStateFlow<BrowseScreenState>,
) {

    fun savedFilters(initialLoad: Boolean = false) {
        scope.launch {
            val filters = db.getBrowseFilters().executeAsBlocking().toImmutableList()
            state.update { it.copy(savedFilters = filters) }
            if (initialLoad) {
                filters
                    .firstOrNull { it.default }
                    ?.let { filter ->
                        val dexFilters = Json.decodeFromString<DexFilters>(filter.dexFilters)
                        state.update { it.copy(filters = dexFilters) }
                    }
            }
        }
    }

    fun save(name: String) {
        scope.launch {
            val browseFilter =
                BrowseFilterImpl(
                    name = name,
                    dexFilters = Json.encodeToString(state.value.filters),
                )
            db.insertBrowseFilter(browseFilter).executeAsBlocking()
            savedFilters()
        }
    }

    fun load(browseFilterImpl: BrowseFilterImpl) {
        scope.launch {
            val dexFilters = Json.decodeFromString<DexFilters>(browseFilterImpl.dexFilters)
            state.update { it.copy(filters = dexFilters) }
        }
    }

    fun markAsDefault(name: String, makeDefault: Boolean) {
        scope.launch {
            val updatedFilters =
                state.value.savedFilters.map {
                    if (it.name == name) {
                        it.copy(default = makeDefault)
                    } else {
                        it.copy(default = false)
                    }
                }
            db.insertBrowseFilters(updatedFilters).executeAsBlocking()
            savedFilters()
        }
    }

    fun delete(name: String) {
        scope.launch {
            db.deleteBrowseFilter(name).executeAsBlocking()
            savedFilters()
        }
    }

    fun reset() {
        scope.launch {
            val resetFilters = browseRepository.createInitialDexFilter()
            state.update { it.copy(filters = resetFilters) }
        }
    }

    fun searchTag(tag: String, getSearchPage: () -> Unit) {
        scope.launch {
            val blankFilter = browseRepository.createInitialDexFilter()

            val filters =
                if (tag.startsWith("Content rating: ")) {
                    val rating =
                        MangaContentRating.getContentRating(tag.substringAfter("Content rating: "))
                    blankFilter.copy(
                        contentRatings =
                        blankFilter.contentRatings.map {
                            if (it.rating == rating) it.copy(state = true)
                            else it.copy(state = false)
                        }
                    )
                } else {
                    blankFilter.copy(
                        tags =
                        blankFilter.tags.map {
                            if (it.tag.prettyPrint.equals(tag, true))
                                it.copy(state = ToggleableState.On)
                            else it
                        }
                    )
                }
            state.update { it.copy(filters = filters) }
            getSearchPage()
        }
    }

    fun searchCreator(creator: String, getSearchPage: () -> Unit) {
        scope.launch {
            val blankFilter = browseRepository.createInitialDexFilter()
            state.update {
                it.copy(
                    filters =
                    blankFilter.copy(
                        queryMode = QueryType.Author,
                        query = Filter.Query(creator, QueryType.Author),
                    )
                )
            }

            getSearchPage()
        }
    }


    fun onFilterChanged(newFilter: Filter) {
        scope.launch {
            val updatedFilters =
                when (newFilter) {
                    is Filter.ContentRating -> {
                        val list =
                            lookupAndReplaceEntry(
                                state.value.filters.contentRatings,
                                { it.rating == newFilter.rating },
                                newFilter,
                            )
                        if (list.none { it.state }) {
                            val default =
                                lookupAndReplaceEntry(
                                    list,
                                    { it.rating == MangaContentRating.Safe },
                                    Filter.ContentRating(MangaContentRating.Safe, true),
                                )
                            state.value.filters.copy(contentRatings = default)
                        } else {
                            state.value.filters.copy(contentRatings = list)
                        }
                    }
                    is Filter.OriginalLanguage -> {
                        val list =
                            lookupAndReplaceEntry(
                                state.value.filters.originalLanguage,
                                { it.language == newFilter.language },
                                newFilter,
                            )
                        state.value.filters.copy(originalLanguage = list)
                    }
                    is Filter.PublicationDemographic -> {
                        val list =
                            lookupAndReplaceEntry(
                                state.value.filters.publicationDemographics,
                                { it.demographic == newFilter.demographic },
                                newFilter,
                            )
                        state.value.filters.copy(publicationDemographics = list)
                    }
                    is Filter.Status -> {
                        val list =
                            lookupAndReplaceEntry(
                                state.value.filters.statuses,
                                { it.status == newFilter.status },
                                newFilter,
                            )
                        state.value.filters.copy(statuses = list)
                    }
                    is Filter.Tag -> {
                        val list =
                            lookupAndReplaceEntry(
                                state.value.filters.tags,
                                { it.tag == newFilter.tag },
                                newFilter,
                            )
                        state.value.filters.copy(tags = list)
                    }
                    is Filter.Sort -> {
                        val filterMode =
                            when (newFilter.state) {
                                true -> newFilter.sort
                                false -> MdSort.Best
                            }

                        state.value.filters.copy(
                            sort = Filter.Sort.getSortList(filterMode)
                        )
                    }
                    is Filter.HasAvailableChapters -> {
                        state.value.filters.copy(hasAvailableChapters = newFilter)
                    }
                    is Filter.TagInclusionMode -> {
                        state.value.filters.copy(tagInclusionMode = newFilter)
                    }
                    is Filter.TagExclusionMode -> {
                        state.value.filters.copy(tagExclusionMode = newFilter)
                    }
                    is Filter.Query -> {
                        when (newFilter.type) {
                            QueryType.Title -> {
                                state.value.filters.copy(
                                    queryMode = QueryType.Title,
                                    query = newFilter,
                                )
                            }
                            QueryType.Author -> {
                                state.value.filters.copy(
                                    queryMode = QueryType.Author,
                                    query = newFilter,
                                )
                            }
                            QueryType.Group -> {
                                state.value.filters.copy(
                                    queryMode = QueryType.Group,
                                    query = newFilter,
                                )
                            }
                            QueryType.List -> {
                                state.value.filters.copy(
                                    queryMode = QueryType.List,
                                    query = newFilter,
                                )
                            }
                        }
                    }
                    is Filter.AuthorId -> {
                        state.value.filters.copy(authorId = newFilter)
                    }
                    is Filter.GroupId -> {
                        state.value.filters.copy(groupId = newFilter)
                    }
                }

            state.update { it.copy(filters = updatedFilters) }
        }
    }

    private fun <T> lookupAndReplaceEntry(
        list: List<T>,
        indexMethod: (T) -> Boolean,
        newEntry: T,
    ): ImmutableList<T> {
        val index = list.indexOfFirst { indexMethod(it) }
        val mutableList = list.toMutableList()
        mutableList[index] = newEntry
        return mutableList.toImmutableList()
    }
}