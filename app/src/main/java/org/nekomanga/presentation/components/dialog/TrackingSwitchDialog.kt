package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun TrackingSwitchDialog(
    themeColorState: ThemeColorState,
    name: String,
    oldName: String,
    newName: String,
    onConfirm: (Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.remove_previous_tracker)) },
            text = {
                Column {
                    val isReplacing = oldName != newName
                    TextButton(
                        onClick = { onConfirm(true, isReplacing) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text =
                                if (isReplacing) {
                                    stringResource(
                                        id = R.string.remove_x_from_service_and_add_y,
                                        oldName,
                                        name,
                                        newName,
                                    )
                                } else {
                                    stringResource(
                                        id = R.string.remove_x_from_service,
                                        oldName,
                                        name,
                                    )
                                }
                        )
                    }
                    Gap(Size.small)
                    TextButton(
                        onClick = { onConfirm(false, isReplacing) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text =
                                if (isReplacing) {
                                    stringResource(id = R.string.keep_both_on_service, name)
                                } else {
                                    stringResource(id = R.string.keep_on_service, name)
                                }
                        )
                    }
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
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
