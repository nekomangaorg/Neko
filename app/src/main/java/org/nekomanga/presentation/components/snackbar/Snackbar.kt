package org.nekomanga.presentation.components.snackbar

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.theme.Size

@Composable
fun snackbarHost(
    snackbarHostState: SnackbarHostState,
    actionColor: Color? = null,
): @Composable () -> Unit {
    return {
        SwipeableSnackbarHost(snackbarHostState) { data, modifier ->
            Snackbar(
                modifier = modifier.systemBarsPadding().padding(10.dp),
                dismissAction = {},
                action = {
                    data.visuals.actionLabel?.let {
                        TextButton(onClick = { data.performAction() }) {
                            Text(
                                text = data.visuals.actionLabel!!,
                                color = actionColor ?: MaterialTheme.colorScheme.onSurface,
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                            )
                        }
                    }
                },
                dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.small),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(text = data.visuals.message, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
