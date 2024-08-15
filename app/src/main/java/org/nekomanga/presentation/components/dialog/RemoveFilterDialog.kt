/*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nekomanga.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

*/
/** Simple Dialog to save a filter */
/*

@Composable
fun RemoveFilterDialog(themeColorState: ThemeColorState, currentFilter: String, onDismiss: () -> Unit, onRemove: (String) -> Unit, onDefault: (String) -> Unit) {

    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {

        AlertDialog(
            title = {
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = saveFilterText,
                        onValueChange = { saveFilterText = it },
                        label = { Text(text = stringResource(id = R.string.name)) },
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
                        onConfirm(saveFilterText)
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
*/
