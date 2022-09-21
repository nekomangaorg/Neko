package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.screens.ThemeColorState

/**
 * Simple Dialog to add a new category
 */
@Composable
fun AddCategoryDialog(themeColorState: ThemeColorState, currentCategories: List<CategoryItem>, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    var categoryText by remember { mutableStateOf("") }
    var saveEnabled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        LaunchedEffect(categoryText, currentCategories) {
            if (categoryText.isEmpty()) {
                saveEnabled = false
                errorMessage = ""
            } else if (currentCategories.any { it.name.equals(categoryText, true) }) {
                saveEnabled = false
                errorMessage = context.getString(R.string.category_with_name_exists)
            } else {
                saveEnabled = true
                errorMessage = ""
            }
        }

        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.new_category))
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it },
                        label = { Text(text = stringResource(id = R.string.category)) },
                        singleLine = true,
                        maxLines = 1,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = themeColorState.buttonColor,
                            focusedLabelColor = themeColorState.buttonColor,
                            focusedBorderColor = themeColorState.buttonColor,

                        ),
                    )
                    Gap(2.dp)
                    Text(text = errorMessage, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error))
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(categoryText)
                        onDismiss()
                    },
                    enabled = saveEnabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
