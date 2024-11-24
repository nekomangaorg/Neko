package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.components.Divider
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.launch
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnScope.SearchFooter(
    modifier: Modifier = Modifier,
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
    val bringIntoViewRequester = BringIntoViewRequester()
    val scope = rememberCoroutineScope()

    if (showDivider) {
        Divider()
        Gap(Size.tiny)
    }

    OutlinedTextField(
        modifier =
            modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent {
                    if (it.isFocused || it.hasFocus) {
                        scope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                }
                .padding(horizontal = Size.small),
        value = title,
        enabled = enabled,
        singleLine = true,
        label = { Text(text = labelText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = {
            if (isError) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            } else if (title.isNotEmpty()) {
                IconButton(onClick = { textChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        tint = themeColorState.buttonColor,
                    )
                }
            }
        },
        isError = isError,
        onValueChange = { textChanged(it) },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(
                onSearch = {
                    if (!isError) {
                        focusManager.clearFocus()
                        search(title)
                    }
                }
            ),
    )
}
