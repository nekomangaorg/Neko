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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.nekomanga.R
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.runOnEnterKeyPressed
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.ThemeConfig
import org.nekomanga.presentation.theme.ThemeConfigProvider
import org.nekomanga.presentation.theme.ThemedPreviews

/** Simple stateless Dialog to add or edit a category */
@Composable
fun AddEditCategoryDialog(
    themeColorState: ThemeColorState = defaultThemeColorState(),
    title: String,
    text: String,
    errorText: String? = null,
    canConfirm: Boolean = false,
    onTextChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        val saveAction = {
            if (canConfirm) {
                onConfirm()
            }
        }

        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
            title = { Text(text = title) },
            text = {
                Column {
                    OutlinedTextField(
                        modifier = Modifier.runOnEnterKeyPressed(saveAction),
                        value = text,
                        onValueChange = onTextChanged,
                        singleLine = true,
                        maxLines = 1,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                cursorColor = themeColorState.primaryColor,
                                focusedLabelColor = themeColorState.primaryColor,
                                focusedBorderColor = themeColorState.primaryColor,
                            ),
                        isError = errorText != null,
                        supportingText = {
                            if (errorText != null) {
                                Text(text = errorText)
                            }
                        },
                    )
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = saveAction,
                    enabled = canConfirm,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = themeColorState.primaryColor
                        ),
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = themeColorState.primaryColor
                        ),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}

@Preview
@Composable
private fun AddEditCategoryDialogDefaultPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        AddEditCategoryDialog(
            title = "New category",
            text = "",
            canConfirm = false,
            onTextChanged = {},
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun AddEditCategoryDialogEditingPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        AddEditCategoryDialog(
            title = "Edit category",
            text = "Action",
            canConfirm = true,
            onTextChanged = {},
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun AddEditCategoryDialogErrorPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        AddEditCategoryDialog(
            title = "New category",
            text = "Existing Category",
            errorText = "A category with this name already exists",
            canConfirm = false,
            onTextChanged = {},
            onDismiss = {},
            onConfirm = {},
        )
    }
}
