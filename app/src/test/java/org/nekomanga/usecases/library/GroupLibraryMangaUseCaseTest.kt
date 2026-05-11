package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.ui.library.LibraryGroup
import eu.kanade.tachiyomi.ui.library.LibrarySort
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem

class GroupLibraryMangaUseCaseTest {

    private val useCase = GroupLibraryMangaUseCase()

    private fun mockLibraryMangaItem(
        mangaId: Long,
        language: List<String> = emptyList(),
        category: Int = 0,
        author: List<String> = emptyList(),
        contentRating: List<String> = emptyList(),
        status: List<String> = emptyList(),
        genre: List<String> = emptyList(),
    ): LibraryMangaItem {
        val mockDisplayManga = mockk<DisplayManga>()
        every { mockDisplayManga.mangaId } returns mangaId

        val mockItem = mockk<LibraryMangaItem>()
        every { mockItem.displayManga } returns mockDisplayManga
        every { mockItem.language } returns language
        every { mockItem.category } returns category
        every { mockItem.author } returns author
        every { mockItem.contentRating } returns contentRating
        every { mockItem.status } returns status
        every { mockItem.genre } returns genre
        return mockItem
    }

    @Test
    fun `groupByDynamic groups items correctly by language`() {
        // Arrange
        val englishManga1 = mockLibraryMangaItem(mangaId = 1L, language = listOf("en"))
        val englishManga2 = mockLibraryMangaItem(mangaId = 2L, language = listOf("en"))
        val japaneseManga = mockLibraryMangaItem(mangaId = 3L, language = listOf("ja"))

        val mangaList = listOf(englishManga1, englishManga2, japaneseManga)
        val currentGroup = LibraryGroup.ByLanguage
        val sortOrder = LibrarySort.Title
        val sortAscending = true

        // Act
        val result =
            useCase.groupByDynamic(
                libraryMangaList = mangaList,
                currentLibraryGroup = currentGroup,
                sortOrder = sortOrder,
                sortAscending = sortAscending,
                loggedInTrackStatus = emptyMap(),
            )

        // Assert
        assertEquals(2, result.size)

        // Items are sorted by key based on the String.CASE_INSENSITIVE_ORDER comparator of
        // ByLanguage
        val enCategory = result.first { it.categoryItem.name == "en" }
        assertEquals(2, enCategory.libraryItems.size)

        val jaCategory = result.first { it.categoryItem.name == "ja" }
        assertEquals(1, jaCategory.libraryItems.size)
    }

    @Test
    fun `groupByDynamic groups items correctly by track status including fallbacks`() {
        // Arrange
        val trackedManga = mockLibraryMangaItem(mangaId = 1L)
        val untrackedManga = mockLibraryMangaItem(mangaId = 2L)

        val mangaList = listOf(trackedManga, untrackedManga)
        val currentGroup = LibraryGroup.ByTrackStatus
        val sortOrder = LibrarySort.Title
        val sortAscending = true
        val trackStatusMap = mapOf(1L to listOf("Reading"))

        // Act
        val result =
            useCase.groupByDynamic(
                libraryMangaList = mangaList,
                currentLibraryGroup = currentGroup,
                sortOrder = sortOrder,
                sortAscending = sortAscending,
                loggedInTrackStatus = trackStatusMap,
            )

        // Assert
        assertEquals(2, result.size)

        val trackedCategory = result.find { it.categoryItem.name == "Reading" }
        assertNotNull(trackedCategory)
        assertEquals(1, trackedCategory?.libraryItems?.size)
        assertEquals(1L, trackedCategory?.libraryItems?.first()?.displayManga?.mangaId)

        val untrackedCategory = result.find { it.categoryItem.name == "Not tracked" }
        assertNotNull(untrackedCategory)
        assertEquals(1, untrackedCategory?.libraryItems?.size)
        assertEquals(2L, untrackedCategory?.libraryItems?.first()?.displayManga?.mangaId)
    }

    @Test
    fun `groupByUngrouped groups distinct items under a single all category`() {
        // Arrange
        val manga1 = mockLibraryMangaItem(mangaId = 1L)
        // Duplicate manga item
        val manga1Dup = mockLibraryMangaItem(mangaId = 1L)
        val manga2 = mockLibraryMangaItem(mangaId = 2L)

        val mangaList = listOf(manga1, manga1Dup, manga2)
        val sortOrder = LibrarySort.Title
        val sortAscending = true

        // Act
        val result =
            useCase.groupByUngrouped(
                libraryMangaList = mangaList,
                sortOrder = sortOrder,
                isAscending = sortAscending,
            )

        // Assert
        assertEquals(1, result.size)

        val categoryGroup = result.first()
        assertEquals(CategoryItem.ALL_CATEGORY, categoryGroup.categoryItem.name)
        // Should distinct by mangaId, so 2 items total
        assertEquals(2, categoryGroup.libraryItems.size)
        assertEquals(1L, categoryGroup.libraryItems[0].displayManga.mangaId)
        assertEquals(2L, categoryGroup.libraryItems[1].displayManga.mangaId)
    }

    @Test
    fun `groupByCategory groups items correctly and skips empty system categories`() {
        // Arrange
        val manga1 = mockLibraryMangaItem(mangaId = 1L, category = 100)
        val manga2 = mockLibraryMangaItem(mangaId = 2L, category = 200)

        val mangaList = listOf(manga1, manga2)

        val category100 = CategoryItem(id = 100, name = "100", sortOrder = LibrarySort.Title)

        val category200 = CategoryItem(id = 200, name = "200", sortOrder = LibrarySort.Title)

        // A system category that has no matching manga
        val systemCategoryEmpty =
            CategoryItem(id = 0, name = CategoryItem.SYSTEM_CATEGORY, sortOrder = LibrarySort.Title)

        val categories = listOf(category100, category200, systemCategoryEmpty)

        // Act
        val result =
            useCase.groupByCategory(libraryMangaList = mangaList, categoryList = categories)

        // Assert
        assertEquals(2, result.size)

        val group100 = result.find { it.categoryItem.id == 100 }
        assertNotNull(group100)
        assertEquals(1, group100?.libraryItems?.size)

        val group200 = result.find { it.categoryItem.id == 200 }
        assertNotNull(group200)
        assertEquals(1, group200?.libraryItems?.size)

        // System category with no manga should be filtered out
        val group0 = result.find { it.categoryItem.id == 0 }
        assertEquals(null, group0)
    }
}
