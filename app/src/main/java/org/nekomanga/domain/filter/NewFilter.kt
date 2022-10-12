package org.nekomanga.domain.filter

import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdLang
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaDemographic

data class DexFilters(
    val titleQuery: NewFilter.TitleQuery = NewFilter.TitleQuery(""),
    val originalLanguage: List<NewFilter.OriginalLanguage> = MdLang.values().map { NewFilter.OriginalLanguage(it, false) },
    val contentRatings: List<NewFilter.ContentRating>,
    val publicationDemographics: List<NewFilter.PublicationDemographic> = MangaDemographic.getOrdered().map { NewFilter.PublicationDemographic(it, false) },
)

enum class TagMode(val param: String) {
    And(MdConstants.SearchParameters.TagMode.and),
    Or(MdConstants.SearchParameters.TagMode.or),
}

sealed class NewFilter(open val enabled: Boolean) {
    data class TitleQuery(val query: String, override val enabled: Boolean = true) : NewFilter(enabled)
    data class OriginalLanguage(val language: MdLang, val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
    data class ContentRating(val rating: MangaContentRating, val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
    data class PublicationDemographic(val demographic: MangaDemographic, val state: Boolean, override val enabled: Boolean = true) : NewFilter(enabled)
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



