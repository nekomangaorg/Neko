package org.nekomanga.usecases.library

import eu.kanade.tachiyomi.ui.library.LibraryGroup
import eu.kanade.tachiyomi.ui.library.LibrarySort
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem

class GroupLibraryMangaUseCaseTest {

    private val useCase = GroupLibraryMangaUseCase()

    private fun createMangaItem(
        id: Long,
        title: String = "Manga $id",
        language: List<String> = emptyList(),
        category: Int = 0,
        author: List<String> = emptyList(),
        genre: List<String> = emptyList(),
        status: List<String> = emptyList(),
        contentRating: List<String> = emptyList(),
    ): LibraryMangaItem {
        val displayManga =
            DisplayManga(
                mangaId = id,
                inLibrary = true,
                currentArtwork = Artwork(mangaId = id, inLibrary = true),
                url = "",
                originalTitle = title,
                userTitle = "",
            )

        return LibraryMangaItem(
            displayManga = displayManga,
            language = language,
            category = category,
            author = author,
            genre = genre,
            status = status,
            contentRating = contentRating,
            userCover = null,
            dynamicCover = null,
        )
    }

    @Test
    fun `groupByDynamic groups by language correctly`() {
        val manga1 = createMangaItem(1, language = listOf("English"))
        val manga2 = createMangaItem(2, language = listOf("Japanese"))
        val manga3 = createMangaItem(3, language = listOf("English"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2, manga3),
                currentLibraryGroup = LibraryGroup.ByLanguage,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        assertEquals(2, result.size)
        assertEquals("English", result[0].categoryItem.name)
        assertEquals(2, result[0].libraryItems.size)
        assertEquals("Japanese", result[1].categoryItem.name)
        assertEquals(1, result[1].libraryItems.size)
    }

    @Test
    fun `groupByDynamic groups by multiple languages correctly`() {
        val manga1 = createMangaItem(1, language = listOf("English", "Japanese"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1),
                currentLibraryGroup = LibraryGroup.ByLanguage,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        assertEquals(2, result.size)
        assertEquals("English", result[0].categoryItem.name)
        assertEquals("Japanese", result[1].categoryItem.name)
        assertEquals(1, result[0].libraryItems.size)
        assertEquals(1, result[1].libraryItems.size)
    }

    @Test
    fun `groupByDynamic groups by track status correctly`() {
        val manga1 = createMangaItem(1)
        val manga2 = createMangaItem(2)
        val manga3 = createMangaItem(3)

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

        assertEquals(2, result.size)
        assertEquals("Reading", result[0].categoryItem.name)
        assertEquals(2, result[0].libraryItems.size)
        assertEquals("Completed", result[1].categoryItem.name)
        assertEquals(1, result[1].libraryItems.size)
    }

    @Test
    fun `groupByDynamic handles untracked status correctly`() {
        val manga1 = createMangaItem(1)
        val trackStatus = emptyMap<Long, List<String>>()

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1),
                currentLibraryGroup = LibraryGroup.ByTrackStatus,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = trackStatus,
            )

        assertEquals(1, result.size)
        assertEquals("Not tracked", result[0].categoryItem.name)
    }

    @Test
    fun `groupByDynamic groups by author correctly and applies sorting`() {
        val manga1 = createMangaItem(1, author = listOf("Oda"))
        val manga2 = createMangaItem(2, author = listOf("Kishimoto"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2),
                currentLibraryGroup = LibraryGroup.ByAuthor,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        assertEquals(2, result.size)
        assertEquals("Kishimoto", result[0].categoryItem.name)
        assertEquals("Oda", result[1].categoryItem.name)
    }

    @Test
    fun `groupByDynamic groups by content rating correctly and applies custom sorting`() {
        val manga1 = createMangaItem(1, contentRating = listOf("Safe"))
        val manga2 = createMangaItem(2, contentRating = listOf("Suggestive"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga2),
                currentLibraryGroup = LibraryGroup.ByContent,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        assertEquals(2, result.size)
        assertEquals("Safe", result[0].categoryItem.name)
        assertEquals("Suggestive", result[1].categoryItem.name)
    }

    @Test
    fun `groupByUngrouped returns all items in one category`() {
        val manga1 = createMangaItem(1)
        val manga2 = createMangaItem(2)

        val result =
            useCase.groupByUngrouped(
                libraryMangaList = listOf(manga1, manga2),
                sortOrder = LibrarySort.Title,
                isAscending = true,
            )

        assertEquals(1, result.size)
        assertEquals(CategoryItem.ALL_CATEGORY, result[0].categoryItem.name)
        assertEquals(2, result[0].libraryItems.size)
    }

    @Test
    fun `groupByCategory groups by category correctly`() {
        val manga1 = createMangaItem(1, category = 1)
        val manga2 = createMangaItem(2, category = 2)
        val manga3 = createMangaItem(3, category = 1)

        val category1 = CategoryItem(id = 1, name = "Category 1", sortOrder = LibrarySort.Title)
        val category2 = CategoryItem(id = 2, name = "Category 2", sortOrder = LibrarySort.Title)

        val result =
            useCase.groupByCategory(
                libraryMangaList = listOf(manga1, manga2, manga3),
                categoryList = listOf(category1, category2),
            )

        assertEquals(2, result.size)
        assertEquals("Category 1", result[0].categoryItem.name)
        assertEquals(2, result[0].libraryItems.size)
        assertEquals("Category 2", result[1].categoryItem.name)
        assertEquals(1, result[1].libraryItems.size)
    }

    @Test
    fun `groupByCategory excludes empty system categories`() {
        val manga1 = createMangaItem(1, category = 1)
        val category1 = CategoryItem(id = 1, name = "Category 1", sortOrder = LibrarySort.Title)
        val systemCategory =
            CategoryItem(id = 0, name = CategoryItem.SYSTEM_CATEGORY, sortOrder = LibrarySort.Title)

        val result =
            useCase.groupByCategory(
                libraryMangaList = listOf(manga1),
                categoryList = listOf(category1, systemCategory),
            )

        assertEquals(1, result.size)
        assertEquals("Category 1", result[0].categoryItem.name)
    }

    @Test
    fun `groupByCategory includes non-empty system categories`() {
        val manga1 = createMangaItem(1, category = 0)
        val systemCategory =
            CategoryItem(id = 0, name = CategoryItem.SYSTEM_CATEGORY, sortOrder = LibrarySort.Title)

        val result =
            useCase.groupByCategory(
                libraryMangaList = listOf(manga1),
                categoryList = listOf(systemCategory),
            )

        assertEquals(1, result.size)
        assertEquals(CategoryItem.SYSTEM_CATEGORY, result[0].categoryItem.name)
    }

    @Test
    fun `groupByCategory returns empty list if library is empty`() {
        val result =
            useCase.groupByCategory(
                libraryMangaList = emptyList(),
                categoryList =
                    listOf(CategoryItem(id = 1, name = "Cat", sortOrder = LibrarySort.Title)),
            )

        assertEquals(0, result.size)
    }

    @Test
    fun `groupByDynamic deduplicates manga before grouping`() {
        val manga1 = createMangaItem(1, language = listOf("English"))
        val manga1Duplicate = createMangaItem(1, language = listOf("English"))

        val result =
            useCase.groupByDynamic(
                libraryMangaList = listOf(manga1, manga1Duplicate),
                currentLibraryGroup = LibraryGroup.ByLanguage,
                sortOrder = LibrarySort.Title,
                sortAscending = true,
                loggedInTrackStatus = emptyMap(),
            )

        assertEquals(1, result.size)
        assertEquals(1, result[0].libraryItems.size)
    }
}
