package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun RemoveTrackingDialog(themeColorState: ThemeColorState, name: String, canRemoveFromTracker: Boolean, onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        var removeFromTracker by remember { mutableStateOf(true) }

        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.remove_tracking))
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { removeFromTracker = !removeFromTracker },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (canRemoveFromTracker) {
                            Checkbox(
                                checked = removeFromTracker,
                                onCheckedChange = {
                                    removeFromTracker = !removeFromTracker
                                },
                                colors = CheckboxDefaults.colors(checkedColor = themeColorState.buttonColor, checkmarkColor = MaterialTheme.colorScheme.surface),
                            )
                            Gap(4.dp)
                            Text(text = stringResource(id = R.string.remove_tracking_from_, name), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                        }
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
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.remove))
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
