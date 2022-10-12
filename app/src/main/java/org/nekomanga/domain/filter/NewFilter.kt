package org.nekomanga.domain.filter

import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.source.model.MangaTag
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdSort
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaDemographic
import org.nekomanga.domain.manga.MangaStatus

data class DexFilters(
    val titleQuery: NewFilter.TitleQuery = NewFilter.TitleQuery(""),
    val authorQuery: NewFilter.AuthorQuery = NewFilter.AuthorQuery(""),
    val originalLanguage: List<NewFilter.OriginalLanguage> = MdLang.values().map { NewFilter.OriginalLanguage(it, false) },
    val contentRatings: List<NewFilter.ContentRating>,
    val publicationDemographics: List<NewFilter.PublicationDemographic> = MangaDemographic.getOrdered().map { NewFilter.PublicationDemographic(it, false) },
    val statuses: List<NewFilter.Status> = MangaStatus.getMangaDexStatus().map { NewFilter.Status(it, false) },
    val tags: List<NewFilter.Tag> = MangaTag.values().map { NewFilter.Tag(it, ToggleableState.Off) },
    val sort: List<NewFilter.Sort> = NewFilter.Sort.getSortList(MdSort.relevance, MangaConstants.SortState.Descending),
    val hasAvailableChapters: NewFilter.HasAvailableChapters = NewFilter.HasAvailableChapters(false),
    val tagInclusionMode: NewFilter.TagInclusionMode = NewFilter.TagInclusionMode(),
    val tagExclusionMode: NewFilter.TagExclusionMode = NewFilter.TagExclusionMode(),
) {
    companion object {
        private fun setEnabledFilter(dexFilters: DexFilters, enabled: Boolean): DexFilters {
            return dexFilters.copy(
                titleQuery = dexFilters.titleQuery.copy(enabled = enabled),
                authorQuery = dexFilters.authorQuery.copy(enabled = enabled),
                originalLanguage = dexFilters.originalLanguage.map { it.copy(enabled = enabled) },
                contentRatings = dexFilters.contentRatings.map { it.copy(enabled = enabled) },
                publicationDemographics = dexFilters.publicationDemographics.map { it.copy(enabled = enabled) },
                statuses = dexFilters.statuses.map { it.copy(enabled = enabled) },
                tags = dexFilters.tags.map { it.copy(enabled = enabled) },
                sort = dexFilters.sort.map { it.copy(enabled = enabled) },
                hasAvailableChapters = dexFilters.hasAvailableChapters.copy(enabled = enabled),
                tagInclusionMode = dexFilters.tagInclusionMode.copy(enabled = enabled),
                tagExclusionMode = dexFilters.tagExclusionMode.copy(enabled = enabled),
            )
        }

        fun disableAll(dexFilters: DexFilters): DexFilters {
            return setEnabledFilter(dexFilters, false)
        }

        fun enableAll(dexFilters: DexFilters): DexFilters {
            return setEnabledFilter(dexFilters, true)
        }

        private fun setEnabledQueryFilters(dexFilters: DexFilters, enabled: Boolean): DexFilters {
            return dexFilters.copy(
                titleQuery = dexFilters.titleQuery.copy(enabled = enabled),
                authorQuery = dexFilters.authorQuery.copy(enabled = enabled),
            )
        }

        fun disableQueries(dexFilters: DexFilters): DexFilters {
            return setEnabledQueryFilters(dexFilters, false)
        }

        fun enableQueries(dexFilters: DexFilters): DexFilters {
            return setEnabledQueryFilters(dexFilters, true)
        }
    }
}

enum class TagMode(val key: String) {
    And(MdConstants.SearchParameters.TagMode.and),
    Or(MdConstants.SearchParameters.TagMode.or),
}

sealed class NewFilter(open val enabled: Boolean) {
    data class TitleQuery(val query: String, override val enabled: Boolean = true) : NewFilter(enabled)
    data class OriginalLanguage(val language: MdLang, val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
    data class ContentRating(val rating: MangaContentRating, val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
    data class PublicationDemographic(val demographic: MangaDemographic, val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
    data class Status(val status: MangaStatus, val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
    data class Tag(val tag: MangaTag, val state: ToggleableState, override val enabled: Boolean = true) : NewFilter(enabled)
    data class HasAvailableChapters(val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
    data class TagInclusionMode(val mode: TagMode = TagMode.And, override val enabled: Boolean = true) : NewFilter(enabled)
    data class TagExclusionMode(val mode: TagMode = TagMode.Or, override val enabled: Boolean = true) : NewFilter(enabled)
    data class AuthorQuery(val query: String, override val enabled: Boolean = true) : NewFilter(enabled)

    data class Sort(val sort: MdSort, val state: MangaConstants.SortState, override val enabled: Boolean = true) : NewFilter(enabled) {
        companion object {
            fun getSortList(sort: MdSort, state: MangaConstants.SortState): List<Sort> {
                return MdSort.values().map {
                    Sort(
                        sort = it,
                        state = if (sort == it) state else MangaConstants.SortState.None,
                    )
                }
            }
        }
    }
}

//author query
// group query



