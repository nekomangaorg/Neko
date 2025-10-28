package org.nekomanga.presentation.components.snackbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.presentation.theme.Size

@Composable
fun snackbarHost(
    snackbarHostState: SnackbarHostState,
    snackBarColor: SnackbarColor? = null,
): @Composable () -> Unit {
    return {
        SwipeableSnackbarHost(snackbarHostState) { data ->
            Snackbar(
                dismissAction = {},
                action = {
                    data.visuals.actionLabel?.let {
                        TextButton(onClick = { data.performAction() }) {
                            Text(
                                text = data.visuals.actionLabel!!,
                                color =
                                    snackBarColor?.actionColor
                                        ?: MaterialTheme.colorScheme.onSurface,
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                            )
                        }
                    }
                },
                dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
                containerColor =
                    snackBarColor?.containerColor
                        ?: MaterialTheme.colorScheme.surfaceColorAtElevation(Size.small),
                contentColor = snackBarColor?.contentColor ?: MaterialTheme.colorScheme.onSurface,
            ) {
                Text(text = data.visuals.message)
            }
        }
    }
}
