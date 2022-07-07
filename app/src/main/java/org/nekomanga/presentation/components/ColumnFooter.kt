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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun ColumnScope.SearchFooter(themeColorState: ThemeColorState, title: String, search: (String) -> Unit) {

    val focusManager = LocalFocusManager.current
    var searchText by remember { mutableStateOf(title) }

    Divider()
    Gap(4.dp)

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        value = searchText,
        label = {
            Text(text = stringResource(id = R.string.title))
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { searchText = "" }) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = themeColorState.buttonColor)
                }
            }
        },
        onValueChange = { searchText = it },
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
                search(searchText)
            },
        ),
    )
}
