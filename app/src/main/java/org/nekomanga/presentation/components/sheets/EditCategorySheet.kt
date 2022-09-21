package org.nekomanga.presentation.components.sheets

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import java.util.Locale
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.dialog.AddCategoryDialog
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

@Composable
fun EditCategorySheet(
    addingToLibrary: Boolean,
    categories: ImmutableList<CategoryItem>,
    mangaCategories: ImmutableList<CategoryItem> = persistentListOf(),
    themeColorState: ThemeColorState = defaultThemeColorState(),
    cancelClick: () -> Unit,
    addNewCategory: (String) -> Unit,
    confirmClicked: (List<CategoryItem>) -> Unit,
    addToLibraryClick: () -> Unit = {},
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {
        val context = LocalContext.current

        val enabledCategories = remember { mangaCategories.associateBy { it.id }.toMutableMap() }
        val acceptText = remember { mutableStateOf(calculateText(context, mangaCategories, enabledCategories, addingToLibrary)) }

        var showAddCategoryDialog by remember { mutableStateOf(false) }

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .4

        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
            if (showAddCategoryDialog) {
                AddCategoryDialog(themeColorState = themeColorState, currentCategories = categories, onDismiss = { showAddCategoryDialog = false }, onConfirm = { addNewCategory(it) })
            }

            val paddingModifier = Modifier.padding(horizontal = 8.dp)

            Gap(16.dp)
            Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val prefix = if (addingToLibrary) R.string.add_x_to else R.string.move_x_to
                Text(modifier = paddingModifier, text = stringResource(id = prefix, stringResource(id = R.string.manga)), style = MaterialTheme.typography.titleLarge)
                TextButton(modifier = paddingModifier, onClick = { showAddCategoryDialog = true }) {
                    Text(text = stringResource(id = R.string.plus_new_category), style = MaterialTheme.typography.titleSmall.copy(color = themeColorState.buttonColor))
                }
            }
            Gap(16.dp)
            Divider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {
                items(categories) { category: CategoryItem ->
                    var state by remember { mutableStateOf(enabledCategories.contains(category.id)) }

                    Row(
                        modifier = paddingModifier
                            .fillMaxWidth()
                            .clickable {
                                state = !state
                                if (state) {
                                    enabledCategories[category.id] = category
                                } else {
                                    enabledCategories.remove(category.id)
                                }
                                acceptText.value = calculateText(context, mangaCategories, enabledCategories, addingToLibrary)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state,
                            onCheckedChange = {
                                if (it) {
                                    enabledCategories[category.id] = category
                                } else {
                                    enabledCategories.remove(category.id)
                                }
                                state = it
                                acceptText.value = calculateText(context, mangaCategories, enabledCategories, addingToLibrary)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = themeColorState.buttonColor, checkmarkColor = MaterialTheme.colorScheme.surface),
                        )
                        Gap(4.dp)
                        Text(text = category.name, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Divider()
            Gap(4.dp)
            Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = cancelClick, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.cancel), style = MaterialTheme.typography.titleSmall)
                }
                ElevatedButton(
                    onClick = {
                        confirmClicked(enabledCategories.values.toList())
                        addToLibraryClick()
                        cancelClick()
                    },
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColorState.buttonColor),
                ) {
                    Text(text = acceptText.value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.surface)
                }
            }
            Gap(16.dp)
        }
    }
}

private fun calculateText(context: Context, initialMangaCategories: List<CategoryItem>, currentlySelectedCategories: Map<Int, CategoryItem>, addingToLibrary: Boolean): String {
    val initialIds = initialMangaCategories.map { it.id }.toSet()
    val same = currentlySelectedCategories.filter { initialIds.contains(it.key) }.values.toList()
    val difference = currentlySelectedCategories.filterNot { initialIds.contains(it.key) }.values.toList()

    val addingMore = initialMangaCategories.isNotEmpty() && difference.isNotEmpty() && same.size == initialMangaCategories.size
    val nothingChanged = difference.isEmpty() && same.size == initialIds.size
    val removing = same.size < initialIds.size && difference.isEmpty()

    fun quantity(size: Int) = context.resources.getQuantityString(R.plurals.category_plural, size, size)

    return context.getString(
        when {
            addingToLibrary || (addingMore && !nothingChanged) -> R.string.add_to_
            removing -> R.string.remove_from_
            nothingChanged -> R.string.keep_in_
            else -> R.string.move_to_
        },
        when {
            same.size == 1 && nothingChanged -> same.first().name
            difference.isEmpty() && initialIds.isNotEmpty() && initialIds.size == same.size -> quantity(same.size)
            difference.isEmpty() && same.isEmpty() && initialIds.isNotEmpty() -> quantity(initialIds.size)
            same.isEmpty() && difference.isEmpty() -> context.getString(R.string.default_category).lowercase(Locale.ROOT)
            difference.size == 1 && nothingChanged.not() -> difference.first().name
            same.size == 1 && nothingChanged.not() && same.size == initialIds.size -> same.first().name
            difference.size > 1 -> context.resources.getQuantityString(
                R.plurals.category_plural,
                difference.size,
                difference.size,
            )
            else -> context.resources.getQuantityString(
                R.plurals.category_plural,
                initialIds.size - same.size,
                initialIds.size - same.size,
            )
        },
    )
}
