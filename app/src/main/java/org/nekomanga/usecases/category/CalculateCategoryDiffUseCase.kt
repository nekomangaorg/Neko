package org.nekomanga.usecases.category

class CalculateCategoryDiffUseCase {
    data class DiffResult(
        val actionType: CategoryActionType,
        val targetName: String?,
        val count: Int,
    )

    enum class CategoryActionType {
        Add,
        Move,
        Remove,
        Keep,
    }

    operator fun invoke(
        initialCategoryIds: Set<Int>,
        selectedCategoryIds: Set<Int>,
        idToNameMap: Map<Int, String>,
        isAddingToLibrary: Boolean,
    ): DiffResult {
        val sameIds = selectedCategoryIds.intersect(initialCategoryIds)
        val addedIds = selectedCategoryIds - initialCategoryIds
        val removedIds = initialCategoryIds - selectedCategoryIds

        val actionType =
            when {
                isAddingToLibrary ||
                    (initialCategoryIds.isEmpty() && selectedCategoryIds.isNotEmpty()) ->
                    CategoryActionType.Add
                removedIds.isNotEmpty() && addedIds.isEmpty() -> CategoryActionType.Remove
                addedIds.isEmpty() && removedIds.isEmpty() -> CategoryActionType.Keep
                else -> CategoryActionType.Move
            }

        val targetName =
            when {
                sameIds.size == 1 && actionType == CategoryActionType.Keep ->
                    idToNameMap[sameIds.first()]
                addedIds.size == 1 && actionType != CategoryActionType.Remove ->
                    idToNameMap[addedIds.first()]
                removedIds.size == 1 && actionType == CategoryActionType.Remove ->
                    idToNameMap[removedIds.first()]
                else -> null
            }

        val count =
            when (actionType) {
                CategoryActionType.Add -> addedIds.size
                CategoryActionType.Remove -> removedIds.size
                else -> selectedCategoryIds.size
            }

        return DiffResult(actionType, targetName, count)
    }
}
