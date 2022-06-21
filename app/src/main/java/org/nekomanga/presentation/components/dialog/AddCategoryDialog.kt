package org.nekomanga.presentation.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R

/**
 * Simple Dialog to add a new category
 */
@Composable
fun AddCategoryDialog(onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var categoryText by remember { mutableStateOf("") }

    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.new_category))
        },
        text = {
            OutlinedTextField(value = categoryText, onValueChange = { categoryText = it }, label = { Text(text = stringResource(id = R.string.category)) })
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirm(categoryText) }) {
                Text(text = stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}
