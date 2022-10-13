package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.Divider
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun ColumnScope.SearchFooter(
    themeColorState: ThemeColorState,
    title: String,
    labelText: String,
    enabled: Boolean = true,
    isError: Boolean = false,
    showDivider: Boolean = true,
    textChanged: (String) -> Unit,
    search: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    if (showDivider) {
        Divider()
        Gap(4.dp)
    }

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        value = title,
        enabled = enabled,
        singleLine = true,
        label = {
            Text(text = labelText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingIcon = {
            if (title.isNotEmpty()) {
                IconButton(onClick = { textChanged("") }) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = themeColorState.buttonColor)
                }
            }
        },
        isError = isError,
        onValueChange = { textChanged(it) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedLabelColor = themeColorState.buttonColor,
            focusedBorderColor = themeColorState.buttonColor,
            cursorColor = themeColorState.buttonColor,
        ),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                search(title)
            },
        ),
    )
}
