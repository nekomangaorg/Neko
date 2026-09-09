package org.nekomanga.usecases.filter

import eu.kanade.tachiyomi.source.online.utils.MdSort
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.filter.QueryType
import org.nekomanga.domain.manga.MangaContentRating

class CalculateDexFilterUseCase {

    operator fun invoke(currentFilters: DexFilters, newFilter: Filter): DexFilters {
        return when (newFilter) {
            is Filter.ContentRating -> {
                val list =
                    lookupAndReplaceEntry(
                        currentFilters.contentRatings,
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
                    currentFilters.copy(contentRatings = default)
                } else {
                    currentFilters.copy(contentRatings = list)
                }
            }

            is Filter.OriginalLanguage -> {
                val list =
                    lookupAndReplaceEntry(
                        currentFilters.originalLanguage,
                        { it.language == newFilter.language },
                        newFilter,
                    )
                currentFilters.copy(originalLanguage = list)
            }

            is Filter.PublicationDemographic -> {
                val list =
                    lookupAndReplaceEntry(
                        currentFilters.publicationDemographics,
                        { it.demographic == newFilter.demographic },
                        newFilter,
                    )
                currentFilters.copy(publicationDemographics = list)
            }

            is Filter.Status -> {
                val list =
                    lookupAndReplaceEntry(
                        currentFilters.statuses,
                        { it.status == newFilter.status },
                        newFilter,
                    )
                currentFilters.copy(statuses = list)
            }

            is Filter.Tag -> {
                val list =
                    lookupAndReplaceEntry(
                        currentFilters.tags,
                        { it.tag == newFilter.tag },
                        newFilter,
                    )
                currentFilters.copy(tags = list)
            }

            is Filter.Sort -> {
                val filterMode =
                    when (newFilter.state) {
                        true -> newFilter.sort
                        false -> MdSort.Best
                    }

                currentFilters.copy(sort = Filter.Sort.getSortList(filterMode).toList())
            }

            is Filter.HasAvailableChapters -> {
                currentFilters.copy(hasAvailableChapters = newFilter)
            }

            is Filter.TagInclusionMode -> {
                currentFilters.copy(tagInclusionMode = newFilter)
            }

            is Filter.TagExclusionMode -> {
                currentFilters.copy(tagExclusionMode = newFilter)
            }

            is Filter.Query -> {
                when (newFilter.type) {
                    QueryType.Title -> {
                        currentFilters.copy(
                            queryMode = QueryType.Title,
                            query = newFilter,
                        )
                    }

                    QueryType.Author -> {
                        currentFilters.copy(
                            queryMode = QueryType.Author,
                            query = newFilter,
                        )
                    }

                    QueryType.Group -> {
                        currentFilters.copy(
                            queryMode = QueryType.Group,
                            query = newFilter,
                        )
                    }

                    QueryType.List -> {
                        currentFilters.copy(
                            queryMode = QueryType.List,
                            query = newFilter,
                        )
                    }
                }
            }

            is Filter.AuthorId -> {
                currentFilters.copy(authorId = newFilter)
            }

            is Filter.GroupId -> {
                currentFilters.copy(groupId = newFilter)
            }
        }
    }

    private fun <T> lookupAndReplaceEntry(
        list: List<T>,
        indexMethod: (T) -> Boolean,
        newEntry: T,
    ): List<T> {
        val index = list.indexOfFirst { indexMethod(it) }
        if (index == -1) return list
        return buildList {
            addAll(list)
            set(index, newEntry)
        }
    }
}
