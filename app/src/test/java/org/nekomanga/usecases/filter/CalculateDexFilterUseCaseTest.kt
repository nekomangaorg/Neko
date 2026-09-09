package org.nekomanga.usecases.filter

import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.source.model.MangaTag
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.source.online.utils.MdSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.filter.QueryType
import org.nekomanga.domain.filter.TagMode
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaDemographic
import org.nekomanga.domain.manga.MangaStatus

class CalculateDexFilterUseCaseTest {

    private val useCase = CalculateDexFilterUseCase()

    private fun createDefaultDexFilters(
        contentRatings: List<Filter.ContentRating> =
            listOf(
                Filter.ContentRating(MangaContentRating.Safe, true),
                Filter.ContentRating(MangaContentRating.Suggestive, false),
                Filter.ContentRating(MangaContentRating.Erotica, false),
                Filter.ContentRating(MangaContentRating.Pornographic, false),
            )
    ): DexFilters {
        return DexFilters(contentRatings = contentRatings)
    }

    @Test
    fun `when toggling content rating, updates target rating state`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.ContentRating(MangaContentRating.Suggestive, true)

        val result = useCase(current, newFilter)

        val suggestive = result.contentRatings.first { it.rating == MangaContentRating.Suggestive }
        assertTrue(suggestive.state)
    }

    @Test
    fun `when all content ratings become deselected, falls back to Safe rating enabled`() {
        val current = createDefaultDexFilters()
        val deselectSafe = Filter.ContentRating(MangaContentRating.Safe, false)

        val result = useCase(current, deselectSafe)

        val safe = result.contentRatings.first { it.rating == MangaContentRating.Safe }
        assertTrue(safe.state)
    }

    @Test
    fun `when original language is toggled, updates matching language state`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.OriginalLanguage(MdLang.Japanese, true)

        val result = useCase(current, newFilter)

        val japanese = result.originalLanguage.first { it.language == MdLang.Japanese }
        assertTrue(japanese.state)
    }

    @Test
    fun `when publication demographic is toggled, updates matching demographic state`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.PublicationDemographic(MangaDemographic.Shounen, true)

        val result = useCase(current, newFilter)

        val shounen =
            result.publicationDemographics.first { it.demographic == MangaDemographic.Shounen }
        assertTrue(shounen.state)
    }

    @Test
    fun `when status is toggled, updates matching status state`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.Status(MangaStatus.Completed, true)

        val result = useCase(current, newFilter)

        val completed = result.statuses.first { it.status == MangaStatus.Completed }
        assertTrue(completed.state)
    }

    @Test
    fun `when tag is updated, updates matching tag state`() {
        val current = createDefaultDexFilters()
        val targetTag = MangaTag.Action
        val newFilter = Filter.Tag(targetTag, ToggleableState.On)

        val result = useCase(current, newFilter)

        val actionTag = result.tags.first { it.tag == targetTag }
        assertEquals(ToggleableState.On, actionTag.state)
    }

    @Test
    fun `when sort is selected with true state, enables the chosen sort option`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.Sort(MdSort.Rating, true)

        val result = useCase(current, newFilter)

        val activeSort = result.sort.first { it.state }
        assertEquals(MdSort.Rating, activeSort.sort)
    }

    @Test
    fun `when sort is unselected with false state, falls back to MdSort Best`() {
        val current =
            createDefaultDexFilters().copy(sort = Filter.Sort.getSortList(MdSort.Rating).toList())
        val newFilter = Filter.Sort(MdSort.Rating, false)

        val result = useCase(current, newFilter)

        val activeSort = result.sort.first { it.state }
        assertEquals(MdSort.Best, activeSort.sort)
    }

    @Test
    fun `when hasAvailableChapters is updated, copies filter`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.HasAvailableChapters(true)

        val result = useCase(current, newFilter)

        assertTrue(result.hasAvailableChapters.state)
    }

    @Test
    fun `when tag inclusion mode is updated, copies filter`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.TagInclusionMode(TagMode.Or)

        val result = useCase(current, newFilter)

        assertEquals(TagMode.Or, result.tagInclusionMode.mode)
    }

    @Test
    fun `when tag exclusion mode is updated, copies filter`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.TagExclusionMode(TagMode.And)

        val result = useCase(current, newFilter)

        assertEquals(TagMode.And, result.tagExclusionMode.mode)
    }

    @Test
    fun `when query is Title, updates queryMode to Title and updates query text`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.Query("Naruto", QueryType.Title)

        val result = useCase(current, newFilter)

        assertEquals(QueryType.Title, result.queryMode)
        assertEquals("Naruto", result.query.text)
        assertEquals(QueryType.Title, result.query.type)
    }

    @Test
    fun `when query is Author, updates queryMode to Author and updates query text`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.Query("Oda", QueryType.Author)

        val result = useCase(current, newFilter)

        assertEquals(QueryType.Author, result.queryMode)
        assertEquals("Oda", result.query.text)
        assertEquals(QueryType.Author, result.query.type)
    }

    @Test
    fun `when query is Group, updates queryMode to Group and updates query text`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.Query("ScanlationGroup", QueryType.Group)

        val result = useCase(current, newFilter)

        assertEquals(QueryType.Group, result.queryMode)
        assertEquals("ScanlationGroup", result.query.text)
        assertEquals(QueryType.Group, result.query.type)
    }

    @Test
    fun `when query is List, updates queryMode to List and updates query text`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.Query("MyCustomList", QueryType.List)

        val result = useCase(current, newFilter)

        assertEquals(QueryType.List, result.queryMode)
        assertEquals("MyCustomList", result.query.text)
        assertEquals(QueryType.List, result.query.type)
    }

    @Test
    fun `when authorId is updated, copies filter`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.AuthorId("123e4567-e89b-12d3-a456-426614174000")

        val result = useCase(current, newFilter)

        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.authorId.uuid)
    }

    @Test
    fun `when groupId is updated, copies filter`() {
        val current = createDefaultDexFilters()
        val newFilter = Filter.GroupId("123e4567-e89b-12d3-a456-426614174000")

        val result = useCase(current, newFilter)

        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.groupId.uuid)
    }

    @Test
    fun `when item to replace does not exist in the list, returns list unchanged`() {
        // Construct a filter list that does not contain English
        val customLanguages = listOf(Filter.OriginalLanguage(MdLang.Japanese, false))
        val current = createDefaultDexFilters().copy(originalLanguage = customLanguages)
        val newFilter = Filter.OriginalLanguage(MdLang.English, true)

        val result = useCase(current, newFilter)

        assertEquals(customLanguages, result.originalLanguage)
    }
}
