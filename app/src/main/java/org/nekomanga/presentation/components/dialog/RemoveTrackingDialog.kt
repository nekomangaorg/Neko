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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun RemoveTrackingDialog(
    themeColorState: ThemeColorState,
    name: String,
    canRemoveFromTracker: Boolean,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        var removeFromTracker by remember { mutableStateOf(true) }

        AlertDialog(
            title = { Text(text = stringResource(id = R.string.remove_tracking)) },
            text = {
                if (canRemoveFromTracker) {
                    Column {
                        CheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            checkedState = removeFromTracker,
                            checkedChange = { removeFromTracker = !removeFromTracker },
                            themeColorState = themeColorState,
                            rowText = stringResource(id = R.string.remove_tracking_from_, name),
                        )
                    }
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(removeFromTracker && canRemoveFromTracker)
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor),
                ) {
                    Text(text = stringResource(id = R.string.remove))
                }
            },
            dismissButton = {
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
