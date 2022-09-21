package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun TrackingSwitchDialog(themeColorState: ThemeColorState, name: String, oldName: String, newName: String, onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.remove_previous_tracker))
            },
            text = {
                Column {
                    TextButton(
                        onClick = { onConfirm(true) },
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.remove_x_from_service_and_add_y, oldName, name, newName))
                    }

                    Gap(8.dp)
                    TextButton(
                        onClick = { onConfirm(false) },
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.keep_both_on_service, name))
                    }
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
