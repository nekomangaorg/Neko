package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaTag
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import java.util.Locale
import uy.kohesive.injekt.injectLazy

class FilterHandler {

    val preferencesHelper: PreferencesHelper by injectLazy()

    internal fun getMDFilterList(): FilterList {
        val filters = mutableListOf(
            HasAvailableChaptersFilter("Has available chapters"),
            OriginalLanguageList(getOriginalLanguage()),
            DemographicList(getDemographics()),
            StatusList(getStatus()),
            SortFilter(sortableList.map { it.first }.toTypedArray()),
            TagList(getTags()),
            TagInclusionMode(),
            TagExclusionMode(),
        ).toMutableList()

        if (preferencesHelper.showContentRatingFilter()) {
            val set = preferencesHelper.contentRatingSelections()
            val contentRating = listOf(
                ContentRating("Safe").apply {
                    state = set.contains(MdConstants.ContentRating.safe)
                },
                ContentRating("Suggestive").apply {
                    state = set.contains(MdConstants.ContentRating.suggestive)
                },
                ContentRating("Erotica").apply {
                    state = set.contains(MdConstants.ContentRating.erotica)
                },
                ContentRating("Pornographic").apply {
                    state = set.contains(MdConstants.ContentRating.pornographic)
                },
            )

            filters.add(2, ContentRatingList(contentRating))
        }

        return FilterList(list = filters.toList())
    }

    private class HasAvailableChaptersFilter(hasAvailableChapters: String) : Filter.CheckBox(hasAvailableChapters)

    private class Demographic(name: String) : Filter.CheckBox(name)
    private class DemographicList(demographics: List<Demographic>) :
        Filter.Group<Demographic>("Publication Demographic", demographics)

    private fun getDemographics() = listOf(
        Demographic("None"),
        Demographic("Shounen"),
        Demographic("Shoujo"),
        Demographic("Seinen"),
        Demographic("Josei"),
    )

    private class Status(name: String) : Filter.CheckBox(name)
    private class StatusList(status: List<Status>) :
        Filter.Group<Status>("Status", status)

    private fun getStatus() = listOf(
        Status("Ongoing"),
        Status("Completed"),
        Status("Hiatus"),
        Status("Abandoned"),
    )

    private class ContentRating(name: String) : Filter.CheckBox(name)
    private class ContentRatingList(contentRating: List<ContentRating>) :
        Filter.Group<ContentRating>("Content Rating", contentRating)

    private class OriginalLanguage(name: String, val isoCode: String) : Filter.CheckBox(name)
    private class OriginalLanguageList(originalLanguage: List<OriginalLanguage>) :
        Filter.Group<OriginalLanguage>("Original language", originalLanguage)

    private fun getOriginalLanguage() = listOf(
        OriginalLanguage("Japanese (Manga)", "ja"),
        OriginalLanguage("Chinese (Manhua)", "zh"),
        OriginalLanguage("Korean (Manhwa)", "ko"),
    )

    class Tag(val id: String, name: String) : Filter.TriState(name)
    class TagList(tags: List<Tag>) : Filter.Group<Tag>("Tags", tags)

    internal fun getTags() = MangaTag.toTagList()

    private class TagInclusionMode :
        Filter.Select<String>("Included tags mode", arrayOf("And", "Or"), 0)

    private class TagExclusionMode :
        Filter.Select<String>("Excluded tags mode", arrayOf("And", "Or"), 1)

    val sortableList = listOf(
        Pair("Latest Uploaded chapter (Any language)", "latestUploadedChapter"),
        Pair("Relevance", "relevance"),
        Pair("Number of follows", "followedCount"),
        Pair("Created at", "createdAt"),
        Pair("Manga info updated", "updatedAt"),
        Pair("Title", "title"),
        Pair("Rating", "rating"),
    )

    class SortFilter(sortables: Array<String>) : Filter.Sort("Sort", sortables, Selection(1, false))

    fun getQueryMap(filters: FilterList): Map<String, Any> {
        val queryMap = mutableMapOf<String, Any>()

        val originalLanguageList = mutableListOf<String>() // originalLanguage[]
        val contentRatingList = mutableListOf<String>() // contentRating[]
        val demographicList = mutableListOf<String>() // publicationDemographic[]
        val statusList = mutableListOf<String>() // status[]
        val includeTagList = mutableListOf<String>() // includedTags[]
        val excludeTagList = mutableListOf<String>() // excludedTags[]
        val hasAvailableChapterLangs = mutableListOf<String>() // availableTranslatedLanguage[]

        // add filters
        filters.forEach { filter ->
            when (filter) {
                is HasAvailableChaptersFilter -> {
                    if (filter.state) {
                        hasAvailableChapterLangs += MdUtil.getLangsToShow(preferencesHelper)
                    }
                }
                is OriginalLanguageList -> {
                    filter.state.filter { lang -> lang.state }
                        .forEach { lang ->
                            if (lang.isoCode == "zh") {
                                originalLanguageList.add("zh-hk")
                            }
                            originalLanguageList.add(lang.isoCode)
                        }
                }
                is ContentRatingList -> {
                    filter.state.filter { rating -> rating.state }
                        .forEach { rating ->
                            contentRatingList.add(rating.name.lowercase(Locale.US))
                        }
                }
                is DemographicList -> {
                    filter.state.filter { demographic -> demographic.state }
                        .forEach { demographic ->
                            demographicList.add(demographic.name.lowercase(Locale.US))
                        }
                }
                is StatusList -> {
                    filter.state.filter { status -> status.state }
                        .forEach { status ->
                            statusList.add(status.name.lowercase(Locale.US))
                        }
                }
                is SortFilter -> {
                    if (filter.state != null) {
                        val query = sortableList[filter.state!!.index].second
                        val value = when (filter.state!!.ascending) {
                            true -> "asc"
                            false -> "desc"
                        }
                        queryMap["order[$query]"] = value
                    }
                }
                is TagList -> {
                    filter.state.forEach { tag ->
                        if (tag.isIncluded()) {
                            includeTagList.add(tag.id)
                        } else if (tag.isExcluded()) {
                            excludeTagList.add(tag.id)
                        }
                    }
                }
                is TagInclusionMode -> {
                    queryMap["includedTagsMode"] =
                        filter.values[filter.state].uppercase(Locale.US)
                }
                is TagExclusionMode -> {
                    queryMap["excludedTagsMode"] =
                        filter.values[filter.state].uppercase(Locale.US)
                }
                else -> Unit
            }
        }
        if (hasAvailableChapterLangs.isNotEmpty()) {
            queryMap["hasAvailableChapters"] = true
            queryMap["availableTranslatedLanguage[]"] = hasAvailableChapterLangs
        }
        if (originalLanguageList.isNotEmpty()) {
            queryMap["originalLanguage[]"] = originalLanguageList
        }
        if (contentRatingList.isNotEmpty()) {
            queryMap["contentRating[]"] = contentRatingList
        }
        if (demographicList.isNotEmpty()) {
            queryMap["publicationDemographic[]"] = demographicList
        }
        if (statusList.isNotEmpty()) {
            queryMap["status[]"] = statusList
        }
        if (includeTagList.isNotEmpty()) {
            queryMap["includedTags[]"] = includeTagList
        }
        if (excludeTagList.isNotEmpty()) {
            queryMap["excludedTags[]"] = excludeTagList
        }

        return queryMap
    }
}
