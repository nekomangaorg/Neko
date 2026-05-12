package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.ui.library.LibraryCategoryItem
import eu.kanade.tachiyomi.ui.library.LibraryGroup
import eu.kanade.tachiyomi.ui.library.LibrarySort
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem

class GroupLibraryMangaUseCaseTest {

    private val useCase = GroupLibraryMangaUseCase()

    private fun createMockManga(
        id: Long,
        language: List<String> = emptyList(),
        author: List<String> = emptyList(),
        genre: List<String> = emptyList(),
        status: List<String> = emptyList(),
        contentRating: List<String> = emptyList(),
        category: Int = 0,
    ): LibraryMangaItem {
        val manga = mockk<LibraryMangaItem>(relaxed = true)
        val displayManga = mockk<DisplayManga>(relaxed = true)
        every { displayManga.mangaId } returns id
        every { manga.displayManga } returns displayManga
        every { manga.language } returns language
        every { manga.author } returns author
        every { manga.genre } returns genre
        every { manga.status } returns status
        every { manga.contentRating } returns contentRating
        every { manga.category } returns category
        return manga
    }

    private fun createExpectedDynamicCategory(
        id: Int,
        name: String,
        items: List<LibraryMangaItem>,
        sortOrder: LibrarySort = LibrarySort.Title,
        isAscending: Boolean = true,
    ): LibraryCategoryItem {
        return LibraryCategoryItem(
            categoryItem =
                CategoryItem(
                    id = id,
                    name = name,
                    sortOrder = sortOrder,
                    isAscending = isAscending,
                    isDynamic = true,
                    isHidden = false,
                ),
            libraryItems = items.toPersistentList(),
        )
    }

    @Test
    fun `groupByDynamic groups by language correctly and sorts categories`() {
        val manga1 = createMockManga(1, language = listOf("Japanese"))
        val manga2 = createMockManga(2, language = listOf("English"))

        // Input in non-sorted order (Japanese then English)
        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2),
                currentLibraryGroup = LibraryGroup.ByLanguage,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        val expected =
            listOf(
                createExpectedDynamicCategory(0, "English", listOf(manga2)),
                createExpectedDynamicCategory(1, "Japanese", listOf(manga1)),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByDynamic groups by multiple languages correctly and sorts categories`() {
        val manga1 = createMockManga(1, language = listOf("Japanese", "English"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1),
                currentLibraryGroup = LibraryGroup.ByLanguage,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        val expected =
            listOf(
                createExpectedDynamicCategory(0, "English", listOf(manga1)),
                createExpectedDynamicCategory(1, "Japanese", listOf(manga1)),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByDynamic groups by track status correctly`() {
        val manga1 = createMockManga(1)
        val manga2 = createMockManga(2)
        val manga3 = createMockManga(3)

        val trackStatus =
            mapOf(1L to listOf("Reading"), 2L to listOf("Completed"), 3L to listOf("Reading"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2, manga3),
                currentLibraryGroup = LibraryGroup.ByTrackStatus,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = trackStatus,
            )

        val expected =
            listOf(
                createExpectedDynamicCategory(0, "Reading", listOf(manga1, manga3)),
                createExpectedDynamicCategory(1, "Completed", listOf(manga2)),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByDynamic handles untracked status correctly`() {
        val manga1 = createMockManga(1)
        val trackStatus = emptyMap<Long, List<String>>()

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1),
                currentLibraryGroup = LibraryGroup.ByTrackStatus,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = trackStatus,
            )

        val expected = listOf(createExpectedDynamicCategory(0, "Not tracked", listOf(manga1)))

        assertEquals(expected, result)
    }

    @Test
    fun `groupByDynamic groups by author correctly and applies sorting`() {
        val manga1 = createMockManga(1, author = listOf("Oda"))
        val manga2 = createMockManga(2, author = listOf("Kishimoto"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2),
                currentLibraryGroup = LibraryGroup.ByAuthor,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        val expected =
            listOf(
                createExpectedDynamicCategory(0, "Kishimoto", listOf(manga2)),
                createExpectedDynamicCategory(1, "Oda", listOf(manga1)),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByDynamic groups by status correctly`() {
        val manga1 = createMockManga(1, status = listOf("Ongoing"))
        val manga2 = createMockManga(2, status = listOf("Completed"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2),
                currentLibraryGroup = LibraryGroup.ByStatus,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        val expected =
            listOf(
                createExpectedDynamicCategory(0, "Completed", listOf(manga2)),
                createExpectedDynamicCategory(1, "Ongoing", listOf(manga1)),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByDynamic groups by tag correctly`() {
        val manga1 = createMockManga(1, genre = listOf("Action"))
        val manga2 = createMockManga(2, genre = listOf("Comedy"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2),
                currentLibraryGroup = LibraryGroup.ByTag,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        val expected =
            listOf(
                createExpectedDynamicCategory(0, "Action", listOf(manga1)),
                createExpectedDynamicCategory(1, "Comedy", listOf(manga2)),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByDynamic groups by content rating correctly and applies custom sorting`() {
        val manga1 = createMockManga(1, contentRating = listOf("suggestive"))
        val manga2 = createMockManga(2, contentRating = listOf("safe"))

        // Reverse input order to ensure sorting logic is verified
        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2),
                currentLibraryGroup = LibraryGroup.ByContent,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        val expected =
            listOf(
                createExpectedDynamicCategory(0, "safe", listOf(manga2)),
                createExpectedDynamicCategory(1, "suggestive", listOf(manga1)),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByUngrouped returns all items in one category`() {
        val manga1 = createMockManga(1)
        val manga2 = createMockManga(2)

        val result =
            useCase.groupByUngrouped(
                libraryMangaList = listOf(manga1, manga2),
                sortOrder = LibrarySort.Title,
                isAscending = true,
            )

        val expectedCategoryItem =
            CategoryItem(
                id = -1,
                name = CategoryItem.ALL_CATEGORY,
                order = -1,
                sortOrder = LibrarySort.Title,
                isAscending = true,
                isDynamic = true,
            )
        val expected =
            listOf(
                LibraryCategoryItem(
                    categoryItem = expectedCategoryItem,
                    libraryItems = persistentListOf(manga1, manga2),
                )
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByCategory groups by category correctly and respects category list order`() {
        val manga1 = createMockManga(1, category = 1)
        val manga2 = createMockManga(2, category = 2)
        val manga3 = createMockManga(3, category = 1)

        val category1 = CategoryItem(id = 1, name = "Category 1", sortOrder = LibrarySort.Title)
        val category2 = CategoryItem(id = 2, name = "Category 2", sortOrder = LibrarySort.Title)

        // Pass categories in non-sorted order (2 then 1) to verify it respects list order
        val result =
            useCase.groupByCategory(
                libraryMangaList = listOf(manga1, manga2, manga3),
                categoryList = listOf(category2, category1),
            )

        val expected =
            listOf(
                LibraryCategoryItem(
                    categoryItem = category2,
                    libraryItems = persistentListOf(manga2),
                ),
                LibraryCategoryItem(
                    categoryItem = category1,
                    libraryItems = persistentListOf(manga1, manga3),
                ),
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByCategory does not deduplicate manga`() {
        val manga1 = createMockManga(1, category = 1)
        val manga1Duplicate = createMockManga(1, category = 1)
        val category1 = CategoryItem(id = 1, name = "Category 1", sortOrder = LibrarySort.Title)

        val result =
            useCase.groupByCategory(
                libraryMangaList = listOf(manga1, manga1Duplicate),
                categoryList = listOf(category1),
            )

        assertEquals(1, result.size)
        assertEquals(2, result[0].libraryItems.size)
        assertEquals(manga1, result[0].libraryItems[0])
        assertEquals(manga1Duplicate, result[0].libraryItems[1])
    }

    @Test
    fun `groupByCategory excludes empty system categories`() {
        val manga1 = createMockManga(1, category = 1)
        val category1 = CategoryItem(id = 1, name = "Category 1", sortOrder = LibrarySort.Title)
        val systemCategory =
            CategoryItem(id = 0, name = CategoryItem.SYSTEM_CATEGORY, sortOrder = LibrarySort.Title)

        val result =
            useCase.groupByCategory(
                libraryMangaList = listOf(manga1),
                categoryList = listOf(category1, systemCategory),
            )

        val expected =
            listOf(
                LibraryCategoryItem(
                    categoryItem = category1,
                    libraryItems = persistentListOf(manga1),
                )
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByCategory includes non-empty system categories`() {
        val manga1 = createMockManga(1, category = 0)
        val systemCategory =
            CategoryItem(id = 0, name = CategoryItem.SYSTEM_CATEGORY, sortOrder = LibrarySort.Title)

        val result =
            useCase.groupByCategory(
                libraryMangaList = listOf(manga1),
                categoryList = listOf(systemCategory),
            )

        val expected =
            listOf(
                LibraryCategoryItem(
                    categoryItem = systemCategory,
                    libraryItems = persistentListOf(manga1),
                )
            )

        assertEquals(expected, result)
    }

    @Test
    fun `groupByCategory returns empty list if library is empty`() {
        val result =
            useCase.groupByCategory(
                libraryMangaList = emptyList(),
                categoryList =
                    listOf(CategoryItem(id = 1, name = "Cat", sortOrder = LibrarySort.Title)),
            )

        assertEquals(emptyList<LibraryCategoryItem>(), result)
    }

    @Test
    fun `groupByDynamic deduplicates manga before grouping`() {
        val manga1 = createMockManga(1, language = listOf("English"))
        val manga1Duplicate = createMockManga(1, language = listOf("English"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga1Duplicate),
                currentLibraryGroup = LibraryGroup.ByLanguage,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        val expected = listOf(createExpectedDynamicCategory(0, "English", listOf(manga1)))

        assertEquals(expected, result)
    }
}
