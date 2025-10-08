package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

/** Simple Dialog to add a new category */
@Composable
fun AddEditCategoryDialog(
    themeColorState: ThemeColorState = defaultThemeColorState(),
    categorySelected: String = "",
    currentCategories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var categoryText by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(categorySelected))
        }
    var validCategory by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
            title = {
                Text(
                    text =
                        if (categorySelected.isBlank()) stringResource(id = R.string.new_category)
                        else stringResource(R.string.edit_category)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { newCategory ->
                            categoryText = newCategory
                            validCategory =
                                currentCategories.none { it.name.equals(categoryText.text, true) }
                        },
                        singleLine = true,
                        maxLines = 1,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                cursorColor = themeColorState.primaryColor,
                                focusedLabelColor = themeColorState.primaryColor,
                                focusedBorderColor = themeColorState.primaryColor,
                            ),
                        isError =
                            !categoryText.text.isBlank() &&
                                (!validCategory && categorySelected.isBlank() ||
                                    (!validCategory && categoryText.text != categorySelected)),
                        supportingText = {
                            if (
                                !categoryText.text.isBlank() &&
                                    (!validCategory && categorySelected.isBlank() ||
                                        (!validCategory && categoryText.text != categorySelected))
                            ) {
                                Text(text = stringResource(R.string.category_with_name_exists))
                            }
                        },
                    )
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(categoryText.text)
                        onDismiss()
                    },
                    enabled =
                        validCategory &&
                            categoryText.text.isNotBlank() &&
                            (categorySelected.isBlank() || categorySelected != categoryText.text),
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor),
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
