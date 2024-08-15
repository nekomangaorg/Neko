package org.nekomanga.domain.filter

import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.source.model.MangaTag
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdSort
import eu.kanade.tachiyomi.util.lang.isUUID
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaDemographic
import org.nekomanga.domain.manga.MangaStatus

@kotlinx.serialization.Serializable
data class DexFilters(
    val queryMode: QueryType = QueryType.Title,
    val query: Filter.Query = Filter.Query("", QueryType.Title),
    val originalLanguage: List<Filter.OriginalLanguage> =
        MdLang.values().map { Filter.OriginalLanguage(it, false) },
    val contentRatings: List<Filter.ContentRating>,
    val contentRatingVisible: Boolean = true,
    val publicationDemographics: List<Filter.PublicationDemographic> =
        MangaDemographic.getOrdered().map { Filter.PublicationDemographic(it, false) },
    val statuses: List<Filter.Status> =
        MangaStatus.getMangaDexStatus().map { Filter.Status(it, false) },
    val tags: List<Filter.Tag> = MangaTag.values().map { Filter.Tag(it, ToggleableState.Off) },
    val sort: List<Filter.Sort> = Filter.Sort.getSortList(),
    val hasAvailableChapters: Filter.HasAvailableChapters = Filter.HasAvailableChapters(),
    val tagInclusionMode: Filter.TagInclusionMode = Filter.TagInclusionMode(),
    val tagExclusionMode: Filter.TagExclusionMode = Filter.TagExclusionMode(),
    val authorId: Filter.AuthorId = Filter.AuthorId(),
    val groupId: Filter.GroupId = Filter.GroupId(),
)

enum class TagMode(val key: String) {
    And(MdConstants.SearchParameters.TagMode.and),
    Or(MdConstants.SearchParameters.TagMode.or),
}

@kotlinx.serialization.Serializable
sealed class Filter {
    @kotlinx.serialization.Serializable
    data class Query(val text: String, val type: QueryType) : Filter()

    @kotlinx.serialization.Serializable
    data class OriginalLanguage(val language: MdLang, val state: Boolean) : Filter()

    @kotlinx.serialization.Serializable
    data class ContentRating(val rating: MangaContentRating, val state: Boolean) : Filter()

    @kotlinx.serialization.Serializable
    data class PublicationDemographic(val demographic: MangaDemographic, val state: Boolean) :
        Filter()

    @kotlinx.serialization.Serializable
    data class Status(val status: MangaStatus, val state: Boolean) : Filter()

    @kotlinx.serialization.Serializable
    data class Tag(val tag: MangaTag, val state: ToggleableState) : Filter()

    @kotlinx.serialization.Serializable
    data class HasAvailableChapters(val state: Boolean = false) : Filter()

    @kotlinx.serialization.Serializable
    data class TagInclusionMode(val mode: TagMode = TagMode.And) : Filter()

    @kotlinx.serialization.Serializable
    data class TagExclusionMode(val mode: TagMode = TagMode.Or) : Filter()

    @kotlinx.serialization.Serializable
    data class AuthorId(val uuid: String = "") : Filter() {
        fun isNotBlankAndInvalidUUID(): Boolean {
            return uuid.isNotBlank() && !uuid.isUUID()
        }
    }

    @kotlinx.serialization.Serializable
    data class GroupId(val uuid: String = "") : Filter() {
        fun isNotBlankAndInvalidUUID(): Boolean {
            return uuid.isNotBlank() && !uuid.isUUID()
        }
    }

    @kotlinx.serialization.Serializable
    data class Sort(val sort: MdSort, val state: Boolean) : Filter() {
        companion object {
            fun getSortList(mdSortToEnable: MdSort = MdSort.Best) =
                MdSort.values().map { Sort(it, it == mdSortToEnable) }
        }
    }
}

enum class QueryType {
    Title,
    Author,
    Group,
    List,
}
