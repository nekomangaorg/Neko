package org.nekomanga.usecases.stats

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.manga.MangaStatus
import org.nekomanga.domain.manga.MangaType
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatManga
import org.nekomanga.presentation.screens.stats.StatsConstants.Sort

class AggregateDetailedStatsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = AggregateDetailedStatsUseCase(defaultDispatcher = testDispatcher)

    private fun createManga(
        id: Long,
        title: String = "Manga $id",
        type: MangaType = MangaType.Manga,
        status: MangaStatus = MangaStatus.Ongoing,
        contentRating: MangaContentRating = MangaContentRating.Safe,
        categories: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        readChapters: Int = 10,
        totalChapters: Int = 20,
        readDuration: Long = 1000L,
        startYear: Int? = 2022,
    ): DetailedStatManga {
        return DetailedStatManga(
            id = id,
            title = title,
            status = status,
            contentRating = contentRating,
            type = type,
            totalChapters = totalChapters,
            readChapters = readChapters,
            bookmarkedChapters = 0,
            unavailableChapters = 0,
            readDuration = readDuration,
            rating = 8.5,
            userScore = 8.0,
            startYear = startYear,
            trackers = emptyList(),
            tags = tags,
            categories = categories,
        )
    }

    @Test
    fun `given empty manga list when invoked then returns empty distribution`() =
        runTest(testDispatcher) {
            val result =
                useCase(
                    mangaList = emptyList(),
                    categories = listOf("Action", "Romance"),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            assertTrue(result.categoryDistribution.groups.isEmpty())
            assertEquals(0, result.categoryDistribution.totalCount)
            assertTrue(result.typeDistribution.groups.isEmpty())
            assertTrue(result.statusDistribution.groups.isEmpty())
            assertTrue(result.contentRatingDistribution.groups.isEmpty())
            assertTrue(result.tagDistribution.groups.isEmpty())
            assertTrue(result.startYearDistribution.groups.isEmpty())
        }

    @Test
    fun `given manga list when calculating category distribution then maps inverted index accurately`() =
        runTest(testDispatcher) {
            val manga1 =
                createManga(
                    id = 1,
                    categories = listOf("Action", "Fantasy"),
                    readChapters = 5,
                    totalChapters = 10,
                    readDuration = 500L,
                )
            val manga2 =
                createManga(
                    id = 2,
                    categories = listOf("Action"),
                    readChapters = 15,
                    totalChapters = 20,
                    readDuration = 1500L,
                )
            val manga3 =
                createManga(
                    id = 3,
                    categories = listOf("Romance"),
                    readChapters = 2,
                    totalChapters = 5,
                    readDuration = 200L,
                )

            val result =
                useCase(
                    mangaList = listOf(manga1, manga2, manga3),
                    categories = listOf("Action", "Fantasy", "Romance", "EmptyCat"),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            val catDist = result.categoryDistribution
            val actionGroup = catDist.groups.first { it.key == "Action" }
            val fantasyGroup = catDist.groups.first { it.key == "Fantasy" }
            val romanceGroup = catDist.groups.first { it.key == "Romance" }
            val emptyGroup = catDist.groups.first { it.key == "EmptyCat" }

            assertEquals(2, actionGroup.count)
            assertEquals(20, actionGroup.readChapters)
            assertEquals(30, actionGroup.totalChapters)
            assertEquals(2000L, actionGroup.readDuration)

            assertEquals(1, fantasyGroup.count)
            assertEquals(5, fantasyGroup.readChapters)

            assertEquals(1, romanceGroup.count)
            assertEquals(0, emptyGroup.count)

            // Total count is sum of counts across groups (2 + 1 + 1 + 0 = 4)
            assertEquals(4, catDist.totalCount)
            assertEquals(2700L, catDist.totalReadDuration)
        }

    @Test
    fun `given empty default category when calculating category distribution then filters out default category`() =
        runTest(testDispatcher) {
            val manga1 = createManga(id = 1, categories = listOf("Action"))

            val result =
                useCase(
                    mangaList = listOf(manga1),
                    categories = listOf("Action"),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            assertTrue(result.categoryDistribution.groups.none { it.key == "Default" })
        }

    @Test
    fun `given non-empty default category when calculating category distribution then includes default category`() =
        runTest(testDispatcher) {
            val manga1 = createManga(id = 1, categories = listOf("Default"))

            val result =
                useCase(
                    mangaList = listOf(manga1),
                    categories = listOf("Action"),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            assertTrue(result.categoryDistribution.groups.any { it.key == "Default" })
        }

    @Test
    fun `given tags with content rating prefix when calculating tag distribution then ignores content rating tags`() =
        runTest(testDispatcher) {
            val manga1 =
                createManga(
                    id = 1,
                    tags = listOf("Action", "content rating: safe", "Fantasy"),
                    readChapters = 10,
                    totalChapters = 20,
                    readDuration = 1000L,
                )

            val result =
                useCase(
                    mangaList = listOf(manga1),
                    categories = emptyList(),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            val tagDist = result.tagDistribution
            assertEquals(2, tagDist.groups.size)
            assertTrue(tagDist.groups.any { it.key == "Action" })
            assertTrue(tagDist.groups.any { it.key == "Fantasy" })
            assertTrue(
                tagDist.groups.none { it.key.contains("content rating:", ignoreCase = true) },
            )
        }

    @Test
    fun `given manga with start years and null start year when calculating start year distribution then groups correctly`() =
        runTest(testDispatcher) {
            val manga1 = createManga(id = 1, startYear = 2021)
            val manga2 = createManga(id = 2, startYear = 2022)
            val manga3 = createManga(id = 3, startYear = null)

            val result =
                useCase(
                    mangaList = listOf(manga1, manga2, manga3),
                    categories = emptyList(),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            val yearDist = result.startYearDistribution
            assertEquals(3, yearDist.groups.size)
            assertTrue(yearDist.groups.any { it.key == "2021" && it.count == 1 })
            assertTrue(yearDist.groups.any { it.key == "2022" && it.count == 1 })
            assertTrue(yearDist.groups.any { it.key == "" && it.count == 1 })
        }

    @Test
    fun `given distribution when sorting by entries, chapters, and duration then sorts in descending order`() =
        runTest(testDispatcher) {
            val manga1 =
                createManga(
                    id = 1,
                    type = MangaType.Manhwa,
                    readChapters = 50,
                    totalChapters = 50,
                    readDuration = 100L,
                )
            val manga2 =
                createManga(
                    id = 2,
                    type = MangaType.Manga,
                    readChapters = 10,
                    totalChapters = 10,
                    readDuration = 2000L,
                )
            val manga3 =
                createManga(
                    id = 3,
                    type = MangaType.Manga,
                    readChapters = 10,
                    totalChapters = 10,
                    readDuration = 500L,
                )

            val result =
                useCase(
                    mangaList = listOf(manga1, manga2, manga3),
                    categories = emptyList(),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            val typeDist = result.typeDistribution

            val sortedByEntries = typeDist.sortedBy(Sort.Entries)
            assertEquals("Manga", sortedByEntries[0].key) // count 2
            assertEquals("Manhwa", sortedByEntries[1].key) // count 1

            val sortedByChapters = typeDist.sortedBy(Sort.Chapters)
            assertEquals("Manhwa", sortedByChapters[0].key) // 50 chapters
            assertEquals("Manga", sortedByChapters[1].key) // 20 chapters

            val sortedByDuration = typeDist.sortedBy(Sort.Duration)
            assertEquals("Manga", sortedByDuration[0].key) // 2500L duration
            assertEquals("Manhwa", sortedByDuration[1].key) // 100L duration
        }

    @Test
    fun `given unsorted start years with null when calculating start year distribution then sorts chronologically with unstarted at the end`() =
        runTest(testDispatcher) {
            val manga1 = createManga(id = 1, startYear = 2024)
            val manga2 = createManga(id = 2, startYear = 1998)
            val manga3 = createManga(id = 3, startYear = null)
            val manga4 = createManga(id = 4, startYear = 2015)
            val manga5 = createManga(id = 5, startYear = null)
            val manga6 = createManga(id = 6, startYear = 2005)

            val result =
                useCase(
                    mangaList = listOf(manga1, manga2, manga3, manga4, manga5, manga6),
                    categories = emptyList(),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            val yearGroups = result.startYearDistribution.groups
            assertEquals(5, yearGroups.size)
            assertEquals("1998", yearGroups[0].key)
            assertEquals("2005", yearGroups[1].key)
            assertEquals("2015", yearGroups[2].key)
            assertEquals("2024", yearGroups[3].key)
            assertEquals("", yearGroups[4].key)
            assertEquals(2, yearGroups[4].count)
            assertEquals(6, result.startYearDistribution.totalCount)
        }

    @Test
    fun `given manga with category not present in input categories when calculating category distribution then dynamically includes category`() =
        runTest(testDispatcher) {
            val manga1 =
                createManga(id = 1, categories = listOf("CustomCategory", "ExistingCategory"))

            val result =
                useCase(
                    mangaList = listOf(manga1),
                    categories = listOf("ExistingCategory"),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            val catGroups = result.categoryDistribution.groups
            assertTrue(catGroups.any { it.key == "CustomCategory" && it.count == 1 })
            assertTrue(catGroups.any { it.key == "ExistingCategory" && it.count == 1 })
            assertEquals(2, result.categoryDistribution.totalCount)
        }

    @Test
    fun `given manga assigned to multiple categories when calculating category distribution then aggregates sums in each category independently`() =
        runTest(testDispatcher) {
            val manga1 =
                createManga(
                    id = 1,
                    categories = listOf("Action", "Sci-Fi", "Comedy"),
                    readChapters = 10,
                    totalChapters = 20,
                    readDuration = 500L,
                )
            val manga2 =
                createManga(
                    id = 2,
                    categories = listOf("Action"),
                    readChapters = 5,
                    totalChapters = 15,
                    readDuration = 250L,
                )

            val result =
                useCase(
                    mangaList = listOf(manga1, manga2),
                    categories = listOf("Action", "Sci-Fi", "Comedy"),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            val catDist = result.categoryDistribution
            val actionGroup = catDist.groups.first { it.key == "Action" }
            val sciFiGroup = catDist.groups.first { it.key == "Sci-Fi" }
            val comedyGroup = catDist.groups.first { it.key == "Comedy" }

            assertEquals(2, actionGroup.count)
            assertEquals(15, actionGroup.readChapters)
            assertEquals(35, actionGroup.totalChapters)
            assertEquals(750L, actionGroup.readDuration)

            assertEquals(1, sciFiGroup.count)
            assertEquals(10, sciFiGroup.readChapters)
            assertEquals(20, sciFiGroup.totalChapters)
            assertEquals(500L, sciFiGroup.readDuration)

            assertEquals(1, comedyGroup.count)
            assertEquals(10, comedyGroup.readChapters)
            assertEquals(20, comedyGroup.totalChapters)
            assertEquals(500L, comedyGroup.readDuration)

            // Total count should be 2 + 1 + 1 = 4
            assertEquals(4, catDist.totalCount)
            assertEquals(1750L, catDist.totalReadDuration)
        }

    @Test
    fun `given manga list with various statuses and content ratings when calculating distributions then aggregates each status and rating correctly`() =
        runTest(testDispatcher) {
            val manga1 =
                createManga(
                    id = 1,
                    status = MangaStatus.Ongoing,
                    contentRating = MangaContentRating.Safe,
                    readChapters = 10,
                    totalChapters = 15,
                    readDuration = 300L,
                )
            val manga2 =
                createManga(
                    id = 2,
                    status = MangaStatus.Completed,
                    contentRating = MangaContentRating.Suggestive,
                    readChapters = 20,
                    totalChapters = 20,
                    readDuration = 600L,
                )
            val manga3 =
                createManga(
                    id = 3,
                    status = MangaStatus.Ongoing,
                    contentRating = MangaContentRating.Erotica,
                    readChapters = 5,
                    totalChapters = 10,
                    readDuration = 100L,
                )

            val result =
                useCase(
                    mangaList = listOf(manga1, manga2, manga3),
                    categories = emptyList(),
                    defaultCategoryName = "Default",
                    typeStringResolver = { it.name },
                    statusStringResolver = { it.name },
                )

            // Status distribution checks
            val statusDist = result.statusDistribution
            assertEquals(2, statusDist.groups.size)
            val ongoingGroup = statusDist.groups.first { it.key == MangaStatus.Ongoing.name }
            val completedGroup = statusDist.groups.first { it.key == MangaStatus.Completed.name }

            assertEquals(2, ongoingGroup.count)
            assertEquals(15, ongoingGroup.readChapters)
            assertEquals(25, ongoingGroup.totalChapters)
            assertEquals(400L, ongoingGroup.readDuration)

            assertEquals(1, completedGroup.count)
            assertEquals(20, completedGroup.readChapters)
            assertEquals(20, completedGroup.totalChapters)
            assertEquals(600L, completedGroup.readDuration)

            // Content rating distribution checks
            val ratingDist = result.contentRatingDistribution
            assertEquals(3, ratingDist.groups.size)
            assertTrue(
                ratingDist.groups.any {
                    it.key == MangaContentRating.Safe.prettyPrint() && it.count == 1
                },
            )
            assertTrue(
                ratingDist.groups.any {
                    it.key == MangaContentRating.Suggestive.prettyPrint() && it.count == 1
                },
            )
            assertTrue(
                ratingDist.groups.any {
                    it.key == MangaContentRating.Erotica.prettyPrint() && it.count == 1
                },
            )
            assertEquals(3, ratingDist.totalCount)
            assertEquals(1000L, ratingDist.totalReadDuration)
        }
}
