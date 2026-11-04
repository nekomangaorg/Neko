package org.nekomanga.usecases.category

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test

class CalculateCategoryDiffUseCaseTest {

    private lateinit var useCase: CalculateCategoryDiffUseCase

    private val idToNameMap =
        mapOf(
            1 to "Action",
            2 to "Comedy",
            3 to "Drama",
            4 to "Romance",
        )

    @Before
    fun setup() {
        useCase = CalculateCategoryDiffUseCase()
    }

    @Test
    fun `given adding to library with no category selected when calculating diff then returns Add with count 0`() {
        val result =
            useCase(
                initialCategoryIds = emptySet(),
                selectedCategoryIds = emptySet(),
                idToNameMap = idToNameMap,
                isAddingToLibrary = true,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe null
        result.count shouldBe 0
    }

    @Test
    fun `given adding to library with single category selected when calculating diff then returns Add with target name`() {
        val result =
            useCase(
                initialCategoryIds = emptySet(),
                selectedCategoryIds = setOf(1),
                idToNameMap = idToNameMap,
                isAddingToLibrary = true,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe "Action"
        result.count shouldBe 1
    }

    @Test
    fun `given adding to library with multiple categories selected when calculating diff then returns Add with null target name`() {
        val result =
            useCase(
                initialCategoryIds = emptySet(),
                selectedCategoryIds = setOf(1, 2),
                idToNameMap = idToNameMap,
                isAddingToLibrary = true,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe null
        result.count shouldBe 2
    }

    @Test
    fun `given existing category and adding one more when calculating diff then returns Add with new category name`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1),
                selectedCategoryIds = setOf(1, 2),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe "Comedy"
        result.count shouldBe 1
    }

    @Test
    fun `given existing category and adding multiple more when calculating diff then returns Add with null target name`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1),
                selectedCategoryIds = setOf(1, 2, 3),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe null
        result.count shouldBe 2
    }

    @Test
    fun `given no changes and single category when calculating diff then returns Keep with category name`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1),
                selectedCategoryIds = setOf(1),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Keep
        result.targetName shouldBe "Action"
        result.count shouldBe 1
    }

    @Test
    fun `given no changes and multiple categories when calculating diff then returns Keep with null target name`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1, 2),
                selectedCategoryIds = setOf(1, 2),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Keep
        result.targetName shouldBe null
        result.count shouldBe 2
    }

    @Test
    fun `given removing one category when calculating diff then returns Remove with count 1`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1, 2),
                selectedCategoryIds = setOf(1),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Remove
        result.targetName shouldBe null
        result.count shouldBe 1
    }

    @Test
    fun `given removing all categories when calculating diff then returns Remove with total count`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1, 2),
                selectedCategoryIds = emptySet(),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Remove
        result.targetName shouldBe null
        result.count shouldBe 2
    }

    @Test
    fun `given replacing one category with another when calculating diff then returns Move with new category name`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1),
                selectedCategoryIds = setOf(2),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Move
        result.targetName shouldBe "Comedy"
        result.count shouldBe 1
    }

    @Test
    fun `given replacing one category with multiple when calculating diff then returns Move with count of selected`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1),
                selectedCategoryIds = setOf(2, 3),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Move
        result.targetName shouldBe null
        result.count shouldBe 2
    }

    @Test
    fun `given partially retained and partially replaced with single new category when calculating diff then returns Move with new name`() {
        // Initial has 1, 2. User removes 2, keeps 1, and adds 3.
        val result =
            useCase(
                initialCategoryIds = setOf(1, 2),
                selectedCategoryIds = setOf(1, 3),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Move
        result.targetName shouldBe "Drama"
        result.count shouldBe 2
    }

    @Test
    fun `given partially retained and partially replaced with multiple new categories when calculating diff then returns Move with null target name`() {
        // Initial has 1, 2. User removes 2, keeps 1, and adds 3 and 4.
        val result =
            useCase(
                initialCategoryIds = setOf(1, 2),
                selectedCategoryIds = setOf(1, 3, 4),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Move
        result.targetName shouldBe null
        result.count shouldBe 3
    }

    @Test
    fun `given no initial categories and none selected when not adding to library then returns Keep with zero count`() {
        val result =
            useCase(
                initialCategoryIds = emptySet(),
                selectedCategoryIds = emptySet(),
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Keep
        result.targetName shouldBe null
        result.count shouldBe 0
    }

    @Test
    fun `given initial categories and none selected when adding to library then returns Add with zero count`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1, 2),
                selectedCategoryIds = emptySet(),
                idToNameMap = idToNameMap,
                isAddingToLibrary = true,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe null
        result.count shouldBe 0
    }

    @Test
    fun `given category ID not present in idToNameMap when calculating diff then target name is null`() {
        val result =
            useCase(
                initialCategoryIds = setOf(1),
                selectedCategoryIds = setOf(1, 999), // 999 is missing in idToNameMap
                idToNameMap = idToNameMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe null
        result.count shouldBe 1
    }

    @Test
    fun `given negative or zero category IDs when calculating diff then diffs are calculated correctly`() {
        val customMap = mapOf(0 to "Default", -1 to "Dynamic")
        val result =
            useCase(
                initialCategoryIds = setOf(0),
                selectedCategoryIds = setOf(0, -1),
                idToNameMap = customMap,
                isAddingToLibrary = false,
            )

        result.actionType shouldBe CalculateCategoryDiffUseCase.CategoryActionType.Add
        result.targetName shouldBe "Dynamic"
        result.count shouldBe 1
    }
}
