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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.Divider
import org.nekomanga.presentation.components.dialog.AddEditCategoryDialog
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.usecases.category.CalculateCategoryDiffUseCase
import org.nekomanga.usecases.category.CalculateCategoryDiffUseCase.CategoryActionType
import org.nekomanga.usecases.category.CalculateCategoryDiffUseCase.DiffResult
import org.nekomanga.usecases.category.ValidateCategoryNameUseCase

@Composable
fun EditCategorySheet(
    addingToLibrary: Boolean,
    categories: List<CategoryItem>,
    selectedCategoryIds: Set<Int>,
    confirmButtonText: String,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    cancelClick: () -> Unit,
    addNewCategory: (String) -> Unit,
    confirmClicked: () -> Unit,
    onCategoryToggled: (categoryId: Int, isChecked: Boolean) -> Unit,
    addToLibraryClick: () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
        var newCategoryText by rememberSaveable { mutableStateOf("") }
        val validateCategoryName = remember { ValidateCategoryNameUseCase() }

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .4

        BaseSheet(themeColor = themeColorState) {
            if (showAddCategoryDialog) {
                val validationResult =
                    remember(newCategoryText, categories) {
                        validateCategoryName(
                            name = newCategoryText,
                            existingNames = categories.map { it.name },
                        )
                    }
                val errorText =
                    when (validationResult) {
                        is ValidateCategoryNameUseCase.ValidationResult.Duplicate ->
                            stringResource(R.string.category_with_name_exists)
                        else -> null
                    }
                AddEditCategoryDialog(
                    themeColorState = themeColorState,
                    title = stringResource(R.string.new_category),
                    text = newCategoryText,
                    errorText = errorText,
                    canConfirm =
                        validationResult is ValidateCategoryNameUseCase.ValidationResult.Valid,
                    onTextChanged = { newCategoryText = it },
                    onDismiss = {
                        showAddCategoryDialog = false
                        newCategoryText = ""
                    },
                    onConfirm = {
                        addNewCategory(newCategoryText.trim())
                        showAddCategoryDialog = false
                        newCategoryText = ""
                    },
                )
            }

            val paddingModifier = Modifier.padding(horizontal = Size.small)

            Gap(Size.medium)
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
                TextButton(
                    modifier = paddingModifier,
                    onClick = { showAddCategoryDialog = true },
                ) {
                    Text(
                        text = stringResource(id = R.string.plus_new_category),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                color = themeColorState.primaryColor
                            ),
                    )
                }
            }
            Gap(Size.medium)
            Divider()

            LazyColumn(
                modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp)
            ) {
                items(items = categories, key = { category -> category.id }) {
                    category: CategoryItem ->
                    val isChecked = selectedCategoryIds.contains(category.id)
                    CheckboxRow(
                        modifier = Modifier.fillMaxWidth(),
                        checkedState = isChecked,
                        checkedChange = { newState -> onCategoryToggled(category.id, newState) },
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
                        ButtonDefaults.textButtonColors(
                            contentColor = themeColorState.primaryColor
                        ),
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                ElevatedButton(
                    onClick = {
                        confirmClicked()
                        addToLibraryClick()
                        cancelClick()
                    },
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = themeColorState.primaryColor
                        ),
                ) {
                    Text(
                        text = confirmButtonText,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}

/** Stateful overload that retains selected category state and calculates button diff text. */
@Composable
fun EditCategorySheet(
    addingToLibrary: Boolean,
    categories: List<CategoryItem>,
    mangaCategories: List<CategoryItem> = listOf(),
    themeColorState: ThemeColorState = defaultThemeColorState(),
    cancelClick: () -> Unit,
    addNewCategory: (String) -> Unit,
    confirmClicked: (List<CategoryItem>) -> Unit,
    addToLibraryClick: () -> Unit = {},
) {
    val initialCategoryIds = remember(mangaCategories) { mangaCategories.map { it.id }.toSet() }
    var selectedCategoryIds by
        rememberSaveable(mangaCategories) { mutableStateOf(initialCategoryIds) }
    val calculateCategoryDiff = remember { CalculateCategoryDiffUseCase() }
    val idToNameMap = remember(categories) { categories.associate { it.id to it.name } }
    val context = LocalContext.current

    val confirmButtonText =
        remember(selectedCategoryIds, initialCategoryIds, idToNameMap, addingToLibrary) {
            val diff =
                calculateCategoryDiff(
                    initialCategoryIds = initialCategoryIds,
                    selectedCategoryIds = selectedCategoryIds,
                    idToNameMap = idToNameMap,
                    isAddingToLibrary = addingToLibrary,
                )
            diff.toButtonText(context)
        }

    EditCategorySheet(
        addingToLibrary = addingToLibrary,
        categories = categories,
        selectedCategoryIds = selectedCategoryIds,
        confirmButtonText = confirmButtonText,
        themeColorState = themeColorState,
        cancelClick = cancelClick,
        addNewCategory = addNewCategory,
        confirmClicked = {
            val selectedCategories = categories.filter { selectedCategoryIds.contains(it.id) }
            confirmClicked(selectedCategories)
        },
        onCategoryToggled = { categoryId, isChecked ->
            selectedCategoryIds =
                if (isChecked) {
                    selectedCategoryIds + categoryId
                } else {
                    selectedCategoryIds - categoryId
                }
        },
        addToLibraryClick = addToLibraryClick,
    )
}

/** Resolves the localized button label for category operations from a domain diff result. */
private fun DiffResult.toButtonText(context: Context): String {
    val stringRes =
        when (actionType) {
            CategoryActionType.Add -> R.string.add_to_
            CategoryActionType.Remove -> R.string.remove_from_
            CategoryActionType.Keep -> R.string.keep_in_
            CategoryActionType.Move -> R.string.move_to_
        }
    val arg =
        when {
            targetName != null -> targetName
            count == 0 -> context.getString(R.string.default_category).lowercase(Locale.ROOT)
            else -> context.resources.getQuantityString(R.plurals.category_plural, count, count)
        }
    return context.getString(stringRes, arg)
}
