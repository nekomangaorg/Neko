package org.nekomanga.presentation.screens.settings.editCategoryscreens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.dialog.AddEditCategoryDialog
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.theme.Size
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

internal class AddEditCategoriesScreen(
    val onNavigationIconClick: () -> Unit,
    val categories: ImmutableList<CategoryItem>,
    val addUpdateCategory: (String, Int?) -> Unit,
    val onChangeOrder: (CategoryItem, Int) -> Unit,
    val deleteCategory: (Int) -> Unit,
) {

    @Composable
    fun Content() {
        val lazyListState = rememberLazyListState()
        var showAddDialog by rememberSaveable { mutableStateOf(false) }
        var editCategoryName by rememberSaveable { mutableStateOf("") }
        var deleteCategoryName by rememberSaveable { mutableStateOf("") }
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

        if (showAddDialog) {
            AddEditCategoryDialog(
                currentCategories = categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { category -> addUpdateCategory(category, null) },
            )
        }

        if (editCategoryName.isNotEmpty()) {
            AddEditCategoryDialog(
                categorySelected = editCategoryName,
                currentCategories = categories,
                onDismiss = { editCategoryName = "" },
                onConfirm = { categoryName ->
                    val id = nonSystemCategories.first { it.name.equals(editCategoryName, true) }.id
                    addUpdateCategory(categoryName, id)
                },
            )
        }

        if (deleteCategoryName.isNotEmpty()) {

            ConfirmationDialog(
                title = stringResource(R.string.confirm_category_deletion),
                body =
                    stringResource(
                        R.string.confirm_category_deletion_message_in,
                        deleteCategoryName,
                    ),
                confirmButton = stringResource(R.string.delete),
                onDismiss = { deleteCategoryName = "" },
                onConfirm = {
                    val category =
                        nonSystemCategories.firstOrNull { it.name.equals(deleteCategoryName, true) }
                    if (category != null) {
                        deleteCategory(category.id)
                    }
                },
            )
        }

        NekoScaffold(
            type = NekoScaffoldType.Title,
            title = stringResource(R.string.edit_categories),
            onNavigationIconClicked = onNavigationIconClick,
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(Size.small),
                ) {
                    items(items = categoriesState, key = { category -> category.name }) { category
                        ->
                        // skip the default category
                        if (category.id != 0) {
                            ReorderableItem(reorderableState, category.name) { isDragging ->
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
                                        Text(text = category.name)

                                        Spacer(modifier = Modifier.weight(1f))

                                        IconButton(onClick = { editCategoryName = category.name }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = null,
                                            )
                                        }
                                        IconButton(
                                            onClick = { deleteCategoryName = category.name }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = null,
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
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(text = stringResource(R.string.add)) },
                )
            }
        }
    }
}
