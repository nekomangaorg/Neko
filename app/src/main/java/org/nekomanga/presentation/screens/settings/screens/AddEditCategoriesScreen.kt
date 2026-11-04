package org.nekomanga.presentation.screens.settings.editCategoryscreens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.dialog.AddEditCategoryDialog
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.category.AddEditCategoryTopBar
import org.nekomanga.presentation.theme.Size
import org.nekomanga.usecases.category.ValidateCategoryNameUseCase
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

internal class AddEditCategoriesScreen(
    val onNavigationIconClick: (() -> Unit)?,
    val categories: List<CategoryItem>,
    val addUpdateCategory: (String, Int?) -> Unit,
    val onChangeOrder: (CategoryItem, Int) -> Unit,
    val deleteCategory: (Int) -> Unit,
) {

    @Composable
    fun Content() {
        val lazyListState = rememberLazyListState()
        var isAddingCategory by rememberSaveable { mutableStateOf(false) }
        var editingCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
        var deleteCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
        var categoryDialogText by rememberSaveable { mutableStateOf("") }
        val validateCategoryName = remember { ValidateCategoryNameUseCase() }
        val hapticFeedback = LocalHapticFeedback.current

        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        val nonSystemCategories =
            remember(categories) { categories.filterNot { it.isSystemCategory } }
        val categoriesState = remember(categories) { nonSystemCategories.toMutableStateList() }
        val reorderableState =
            rememberReorderableLazyListState(lazyListState) { from, to ->
                val item = categoriesState.removeAt(from.index)
                categoriesState.add(to.index, item)
                onChangeOrder(item, to.index)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }

        LaunchedEffect(categories) {
            if (!reorderableState.isAnyItemDragging) {
                categoriesState.clear()
                categoriesState.addAll(nonSystemCategories)
            }
        }

        val isDialogVisible = isAddingCategory || editingCategoryId != null
        if (isDialogVisible) {
            val isEditing = editingCategoryId != null
            val currentCategory =
                remember(editingCategoryId, nonSystemCategories) {
                    editingCategoryId?.let { id -> nonSystemCategories.firstOrNull { it.id == id } }
                }
            val currentName = currentCategory?.name.orEmpty()
            val validationResult =
                remember(categoryDialogText, categories, currentName, isEditing) {
                    validateCategoryName(
                        name = categoryDialogText,
                        existingNames = categories.map { it.name },
                        currentName = if (isEditing) currentName else "",
                    )
                }
            val errorText =
                when (validationResult) {
                    is ValidateCategoryNameUseCase.ValidationResult.Duplicate ->
                        stringResource(R.string.category_with_name_exists)
                    else -> null
                }

            AddEditCategoryDialog(
                title =
                    stringResource(
                        id = if (isEditing) R.string.edit_category else R.string.new_category
                    ),
                text = categoryDialogText,
                errorText = errorText,
                canConfirm = validationResult is ValidateCategoryNameUseCase.ValidationResult.Valid,
                onTextChanged = { categoryDialogText = it },
                onDismiss = {
                    isAddingCategory = false
                    editingCategoryId = null
                    categoryDialogText = ""
                },
                onConfirm = {
                    addUpdateCategory(categoryDialogText.trim(), editingCategoryId)
                    isAddingCategory = false
                    editingCategoryId = null
                    categoryDialogText = ""
                },
            )
        }

        val categoryToDelete =
            remember(deleteCategoryId, nonSystemCategories) {
                deleteCategoryId?.let { id -> nonSystemCategories.firstOrNull { it.id == id } }
            }

        if (categoryToDelete != null) {
            ConfirmationDialog(
                title = stringResource(R.string.confirm_category_deletion),
                body =
                    stringResource(
                        R.string.confirm_category_deletion_message_in,
                        categoryToDelete.name,
                    ),
                confirmButton = stringResource(R.string.delete),
                onDismiss = { deleteCategoryId = null },
                onConfirm = {
                    deleteCategory(categoryToDelete.id)
                    deleteCategoryId = null
                },
            )
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        ChildScreenScaffold(
            scrollBehavior = scrollBehavior,
            topBar = {
                AddEditCategoryTopBar(
                    onNavigationIconClicked = onNavigationIconClick,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(Size.small),
                ) {
                    items(items = categoriesState, key = { category -> category.id }) { category ->
                        // skip the default category
                        if (category.id != 0) {
                            ReorderableItem(reorderableState, category.id) { isDragging ->
                                val interactionSource = remember { MutableInteractionSource() }
                                ElevatedCard(
                                    modifier =
                                        Modifier.fillMaxWidth().padding(horizontal = Size.medium),
                                    onClick = {},
                                    interactionSource = interactionSource,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(Size.small),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconButton(
                                            modifier =
                                                Modifier.draggableHandle(
                                                    onDragStarted = {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType
                                                                .GestureThresholdActivate
                                                        )
                                                    },
                                                    onDragStopped = {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.GestureEnd
                                                        )
                                                    },
                                                    interactionSource = interactionSource,
                                                ),
                                            onClick = {},
                                        ) {
                                            Icon(
                                                Icons.Rounded.DragHandle,
                                                contentDescription = "Reorder",
                                            )
                                        }
                                        Text(text = category.name, modifier = Modifier.weight(1f))

                                        IconButton(
                                            onClick = {
                                                editingCategoryId = category.id
                                                categoryDialogText = category.name
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription =
                                                    stringResource(id = R.string.edit),
                                            )
                                        }
                                        IconButton(onClick = { deleteCategoryId = category.id }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription =
                                                    stringResource(id = R.string.delete),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ExtendedFloatingActionButton(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(
                                bottom = contentPadding.calculateBottomPadding(),
                                end = Size.small,
                            ),
                    onClick = {
                        categoryDialogText = ""
                        isAddingCategory = true
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(text = stringResource(R.string.add)) },
                )
            }
        }
    }
}
