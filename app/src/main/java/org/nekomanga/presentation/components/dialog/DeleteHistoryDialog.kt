package org.nekomanga.presentation.components.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

/** Simple Dialog to add a new category */
@Composable
fun DeleteHistoryDialog(
    themeColorState: ThemeColorState,
    onDismiss: () -> Unit,
    name: String,
    @StringRes title: Int,
    @StringRes description: Int,
    onConfirm: () -> Unit,
) {

    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    ) {
        AlertDialog(
            title = { Text(text = stringResource(id = title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(description),
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                MaterialTheme.colorScheme.onSurface
                            ),
                    )
                    Gap(Size.large)
                    Text(
                        text = name,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                color =
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = NekoColors.mediumAlphaHighContrast
                                    )
                            ),
                    )
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.reset))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
