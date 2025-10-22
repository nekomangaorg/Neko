package org.nekomanga.presentation.components.sheets

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LocalRippleConfiguration
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
import java.util.Locale
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.Divider
import org.nekomanga.presentation.components.dialog.AddEditCategoryDialog
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun EditCategorySheet(
    addingToLibrary: Boolean,
    categories: PersistentList<CategoryItem>,
    mangaCategories: PersistentList<CategoryItem> = persistentListOf(),
    themeColorState: ThemeColorState = defaultThemeColorState(),
    cancelClick: () -> Unit,
    addNewCategory: (String) -> Unit,
    confirmClicked: (List<CategoryItem>) -> Unit,
    addToLibraryClick: () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        val context = LocalContext.current

        val enabledCategories = remember { mangaCategories.associateBy { it.id }.toMutableMap() }
        val acceptText = remember {
            mutableStateOf(
                calculateText(context, mangaCategories, enabledCategories, addingToLibrary)
            )
        }

        var showAddCategoryDialog by remember { mutableStateOf(false) }

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .4

        BaseSheet(themeColor = themeColorState) {
            if (showAddCategoryDialog) {
                AddEditCategoryDialog(
                    themeColorState = themeColorState,
                    currentCategories = categories,
                    onDismiss = { showAddCategoryDialog = false },
                    onConfirm = { category -> addNewCategory(category) },
                )
            }

            val paddingModifier = Modifier.padding(horizontal = Size.small)

            Gap(16.dp)
            Row(
                modifier = paddingModifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val stringRes = if (addingToLibrary) R.string.add_x_to else R.string.move_x_to
                Text(
                    modifier = paddingModifier,
                    text = stringResource(stringRes),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(modifier = paddingModifier, onClick = { showAddCategoryDialog = true }) {
                    Text(
                        text = stringResource(id = R.string.plus_new_category),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                color = themeColorState.primaryColor
                            ),
                    )
                }
            }
            Gap(16.dp)
            Divider()

            LazyColumn(
                modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp)
            ) {
                items(items = categories, key = { category -> category.id }) {
                    category: CategoryItem ->
                    var state by remember {
                        mutableStateOf(enabledCategories.contains(category.id))
                    }
                    CheckboxRow(
                        modifier = Modifier.fillMaxWidth(),
                        checkedState = state,
                        checkedChange = { newState ->
                            state = newState
                            if (state) {
                                enabledCategories[category.id] = category
                            } else {
                                enabledCategories.remove(category.id)
                            }
                            acceptText.value =
                                calculateText(
                                    context,
                                    mangaCategories,
                                    enabledCategories,
                                    addingToLibrary,
                                )
                        },
                        rowText = category.name,
                        themeColorState = themeColorState,
                    )
                }
            }

            Divider()
            Gap(Size.tiny)
            Row(
                modifier = paddingModifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = cancelClick,
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor),
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                ElevatedButton(
                    onClick = {
                        confirmClicked(enabledCategories.values.toList())
                        addToLibraryClick()
                        cancelClick()
                    },
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = themeColorState.primaryColor
                        ),
                ) {
                    Text(
                        text = acceptText.value,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}

private fun calculateText(
    context: Context,
    initialMangaCategories: List<CategoryItem>,
    currentlySelectedCategories: Map<Int, CategoryItem>,
    addingToLibrary: Boolean,
): String {
    val initialIds = initialMangaCategories.map { it.id }.toSet()
    val same = currentlySelectedCategories.filter { initialIds.contains(it.key) }.values.toList()
    val difference =
        currentlySelectedCategories.filterNot { initialIds.contains(it.key) }.values.toList()

    val addingMore =
        initialMangaCategories.isNotEmpty() &&
            difference.isNotEmpty() &&
            same.size == initialMangaCategories.size
    val nothingChanged = difference.isEmpty() && same.size == initialIds.size
    val removing = same.size < initialIds.size && difference.isEmpty()

    fun quantity(size: Int) =
        context.resources.getQuantityString(R.plurals.category_plural, size, size)

    return context.getString(
        when {
            addingToLibrary || (addingMore && !nothingChanged) -> R.string.add_to_
            removing -> R.string.remove_from_
            nothingChanged -> R.string.keep_in_
            else -> R.string.move_to_
        },
        when {
            same.size == 1 && nothingChanged -> same.first().name
            difference.isEmpty() && initialIds.isNotEmpty() && initialIds.size == same.size ->
                quantity(same.size)
            difference.isEmpty() && same.isEmpty() && initialIds.isNotEmpty() ->
                quantity(initialIds.size)
            same.isEmpty() && difference.isEmpty() ->
                context.getString(R.string.default_category).lowercase(Locale.ROOT)
            difference.size == 1 && !nothingChanged -> difference.first().name
            same.size == 1 && !nothingChanged && same.size == initialIds.size -> same.first().name
            difference.size > 1 ->
                context.resources.getQuantityString(
                    R.plurals.category_plural,
                    difference.size,
                    difference.size,
                )
            else ->
                context.resources.getQuantityString(
                    R.plurals.category_plural,
                    initialIds.size - same.size,
                    initialIds.size - same.size,
                )
        },
    )
}
