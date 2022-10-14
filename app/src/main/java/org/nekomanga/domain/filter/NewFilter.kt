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

@kotlinx.serialization.Serializable
data class DexFilters(
    val queryMode: QueryType = QueryType.Title,
    val query: NewFilter.Query = NewFilter.Query("", QueryType.Title),
    val originalLanguage: List<NewFilter.OriginalLanguage> = MdLang.values().map { NewFilter.OriginalLanguage(it, false) },
    val contentRatings: List<NewFilter.ContentRating>,
    val publicationDemographics: List<NewFilter.PublicationDemographic> = MangaDemographic.getOrdered().map { NewFilter.PublicationDemographic(it, false) },
    val statuses: List<NewFilter.Status> = MangaStatus.getMangaDexStatus().map { NewFilter.Status(it, false) },
    val tags: List<NewFilter.Tag> = MangaTag.values().map { NewFilter.Tag(it, ToggleableState.Off) },
    val sort: List<NewFilter.Sort> = NewFilter.Sort.getSortList(MdSort.relevance, MangaConstants.SortState.Descending),
    val hasAvailableChapters: NewFilter.HasAvailableChapters = NewFilter.HasAvailableChapters(false),
    val tagInclusionMode: NewFilter.TagInclusionMode = NewFilter.TagInclusionMode(),
    val tagExclusionMode: NewFilter.TagExclusionMode = NewFilter.TagExclusionMode(),
    val authorId: NewFilter.AuthorId = NewFilter.AuthorId(),
    val groupId: NewFilter.GroupId = NewFilter.GroupId(),

    )

enum class TagMode(val key: String) {
    And(MdConstants.SearchParameters.TagMode.and),
    Or(MdConstants.SearchParameters.TagMode.or),
}

@kotlinx.serialization.Serializable
sealed class NewFilter {
    @kotlinx.serialization.Serializable
    data class Query(val text: String, val type: QueryType) : NewFilter()

    @kotlinx.serialization.Serializable
    data class OriginalLanguage(val language: MdLang, val state: Boolean) : NewFilter()

    @kotlinx.serialization.Serializable
    data class ContentRating(val rating: MangaContentRating, val state: Boolean) : NewFilter()

    @kotlinx.serialization.Serializable
    data class PublicationDemographic(val demographic: MangaDemographic, val state: Boolean) : NewFilter()

    @kotlinx.serialization.Serializable
    data class Status(val status: MangaStatus, val state: Boolean) : NewFilter()

    @kotlinx.serialization.Serializable
    data class Tag(val tag: MangaTag, val state: ToggleableState) : NewFilter()

    @kotlinx.serialization.Serializable
    data class HasAvailableChapters(val state: Boolean) : NewFilter()

    @kotlinx.serialization.Serializable
    data class TagInclusionMode(val mode: TagMode = TagMode.And) : NewFilter()

    @kotlinx.serialization.Serializable
    data class TagExclusionMode(val mode: TagMode = TagMode.Or) : NewFilter()

    @kotlinx.serialization.Serializable
    data class AuthorId(val uuid: String = "") : NewFilter()

    @kotlinx.serialization.Serializable
    data class GroupId(val uuid: String = "") : NewFilter()

    @kotlinx.serialization.Serializable
    data class Sort(val sort: MdSort, val state: MangaConstants.SortState) : NewFilter() {
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

enum class QueryType {
    Title,
    Author,
    Group
}

