package org.nekomanga.domain.filter

import eu.kanade.tachiyomi.source.online.utils.MdConstants
import org.nekomanga.domain.manga.MangaContentRating

data class DexFilters(
    val titleQuery: NewFilter.TitleQuery = NewFilter.TitleQuery(""),
    val contentRatings: List<NewFilter.ContentRating>,
)

enum class TagMode(val param: String) {
    And(MdConstants.SearchParameters.TagMode.and),
    Or(MdConstants.SearchParameters.TagMode.or),
}

sealed class NewFilter(val filterParam: FilterParam, open val enabled: Boolean) {
    data class TitleQuery(val query: String, override val enabled: Boolean = true) : NewFilter(FilterParam.Title, enabled)
    data class ContentRating(val rating: MangaContentRating, val state: Boolean, override val enabled: Boolean = true) : NewFilter(FilterParam.ContentRating, enabled)
}

enum class FilterParam(val displayName: String, val queryParamName: String) {
    Title(MdConstants.SearchParameters.Title.display, MdConstants.SearchParameters.Title.param),
    ContentRating(MdConstants.SearchParameters.ContentRating.display, MdConstants.SearchParameters.ContentRating.param)
}

//title query
//author query
// group query
// has available chapters checkbox
//include tag mode drop down
//exlcuded tag mode drop down
//tags tri state for each
//sort bi state only 1 selected
// status, demographic, content rating, original langauge, bi state for each



