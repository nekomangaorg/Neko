package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem

/** Returns a string of categories name for settings subtitle */
@ReadOnlyComposable
@Composable
fun getCategoriesLabel(
    allCategories: ImmutableList<CategoryItem>,
    included: Set<String>,
    excluded: Set<String>,
): String {

    val includedCategories =
        included
            .mapNotNull { id -> allCategories.find { it.id.toLong() == id.toLong() } }
            .sortedBy { it.order }
    val excludedCategories =
        excluded
            .mapNotNull { id -> allCategories.find { it.id.toLong() == id.toLong() } }
            .sortedBy { it.order }
    val allExcluded = excludedCategories.size == allCategories.size

    val includedItemsText =
        when {
            // Some selected, but not all
            includedCategories.isNotEmpty() && includedCategories.size != allCategories.size ->
                includedCategories.joinToString { it.name }
            // All explicitly selected
            includedCategories.size == allCategories.size -> stringResource(R.string.all)
            allExcluded -> stringResource(R.string.none)
            else -> stringResource(R.string.all)
        }
    val excludedItemsText =
        when {
            excludedCategories.isEmpty() -> stringResource(R.string.none)
            allExcluded -> stringResource(R.string.all)
            else -> excludedCategories.joinToString { it.name }
        }
    return stringResource(R.string.include_, includedItemsText) +
        "\n" +
        stringResource(R.string.exclude_, excludedItemsText)
}
