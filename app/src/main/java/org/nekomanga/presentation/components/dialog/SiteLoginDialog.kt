package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import eu.kanade.tachiyomi.ui.setting.MergeLoginEvent
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

@Composable
fun LoginDialog(
    sourceName: String,
    requiresCredential: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    loginEvent: SharedFlow<MergeLoginEvent>,
) {

    var username by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var password by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var url by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    var showLoginError by rememberSaveable { mutableStateOf(false) }

    var showLoading by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loginEvent.collect { event ->
            when (event) {
                is MergeLoginEvent.Success -> onDismiss()

                is MergeLoginEvent.Error -> {
                    showLoginError = true
                    showLoading = false
                }
            }
        }
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.sign_in_to_, sourceName)) },
        text = {
            Column(
                modifier = Modifier.padding(horizontal = Size.small),
                verticalArrangement = Arrangement.spacedBy(Size.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    label = { Text(stringResource(R.string.username)) },
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                var passwordVisible by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(
                    label = { Text(stringResource(R.string.password)) },
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation =
                        if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        if (password.text.isNotBlank()) {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveRedEye,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    label = { Text(stringResource(R.string.url)) },
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showLoginError) {
                    Text(
                        stringResource(R.string.could_not_sign_in),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (showLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(Size.medium))
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled =
                    !showLoading &&
                        url.text.isNotEmpty() &&
                        (!requiresCredential ||
                            username.text.isNotBlank() && password.text.isNotBlank()),
                onClick = {
                    onConfirm(username.text, password.text, url.text)
                    showLoading = true
                    showLoginError = false
                },
            ) {
                Text(text = stringResource(id = R.string.sign_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
