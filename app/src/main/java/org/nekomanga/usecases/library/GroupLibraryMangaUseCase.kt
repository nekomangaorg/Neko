package org.nekomanga.usecases.library

import org.nekomanga.presentation.screens.library.LibraryCategoryItem
import org.nekomanga.presentation.screens.library.LibraryGroup
import org.nekomanga.presentation.screens.library.LibrarySort
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.CategoryItem.Companion.ALL_CATEGORY
import org.nekomanga.domain.manga.LibraryMangaItem

class GroupLibraryMangaUseCase {

    fun groupByDynamic(
        libraryMangaList: List<LibraryMangaItem>,
        currentLibraryGroup: LibraryGroup,
        sortOrder: LibrarySort,
        sortAscending: Boolean,
        loggedInTrackStatus: Map<Long, List<String>>,
    ): List<LibraryCategoryItem> {
        val groupedMap = mutableMapOf<String, MutableList<LibraryMangaItem>>()
        val notTrackedList = listOf("Not tracked")

        val distinctMangaList = libraryMangaList.distinctBy { it.displayManga.mangaId }

        for (libraryMangaItem in distinctMangaList) {
            val groupingKeys =
                when (currentLibraryGroup) {
                    LibraryGroup.ByAuthor -> libraryMangaItem.author
                    LibraryGroup.ByContent -> libraryMangaItem.contentRating
                    LibraryGroup.ByLanguage -> libraryMangaItem.language
                    LibraryGroup.ByStatus -> libraryMangaItem.status
                    LibraryGroup.ByTag -> libraryMangaItem.genre
                    LibraryGroup.ByTrackStatus -> {
                        loggedInTrackStatus[libraryMangaItem.displayManga.mangaId] ?: notTrackedList
                    }
                    else -> libraryMangaItem.language
                }.distinct()

            for (key in groupingKeys) {
                groupedMap.getOrPut(key) { mutableListOf() }.add(libraryMangaItem)
            }
        }
        val keyComparator = currentLibraryGroup.keyComparator

        return groupedMap.entries
            .sortedWith(compareBy(keyComparator) { it.key })
            .mapIndexed { index, entry ->
                val categoryName = entry.key
                val items = entry.value
                val categoryItem =
                    CategoryItem(
                        id = index,
                        sortOrder = sortOrder,
                        isAscending = sortAscending,
                        name = categoryName,
                        isHidden = false,
                        isDynamic = true,
                    )
                LibraryCategoryItem(
                    categoryItem = categoryItem,
                    libraryItems = items.toList(),
                )
            }
            .toList()
    }

    fun groupByUngrouped(
        libraryMangaList: List<LibraryMangaItem>,
        sortOrder: LibrarySort,
        isAscending: Boolean,
    ): List<LibraryCategoryItem> {

        val allCategoryItem =
            CategoryItem(
                id = -1,
                name = ALL_CATEGORY,
                order = -1,
                sortOrder = sortOrder,
                isAscending = isAscending,
                isDynamic = true,
            )

        val distinctList =
            libraryMangaList.distinctBy { it.displayManga.mangaId }.toList()

        return listOf(
            LibraryCategoryItem(categoryItem = allCategoryItem, libraryItems = distinctList)
        )
    }

    fun groupByCategory(
        libraryMangaList: List<LibraryMangaItem>,
        categoryList: List<CategoryItem>,
        showEmptyCategories: Boolean = false,
        hasMangaInDefaultCategory: Boolean,
        isSearching: Boolean = false,
    ): List<LibraryCategoryItem> {
        if (libraryMangaList.isEmpty() && !showEmptyCategories) {
            return emptyList()
        }

        val mangaMap = libraryMangaList.groupBy { it.category }

        return categoryList.mapNotNull { categoryItem ->
            val unsortedMangaList = mangaMap[categoryItem.id] ?: emptyList()

            if (categoryItem.isSystemCategory) {
                if (!hasMangaInDefaultCategory) {
                    return@mapNotNull null
                }
                if (unsortedMangaList.isEmpty() && !showEmptyCategories && !isSearching) {
                    return@mapNotNull null
                }
            }

            LibraryCategoryItem(
                categoryItem = categoryItem,
                libraryItems = unsortedMangaList.toList(),
            )
        }
    }
}
