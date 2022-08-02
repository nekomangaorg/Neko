package org.nekomanga.presentation.components.snackbar

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.extensions.surfaceColorAtElevation

@Composable
fun snackbarHost(snackbarHostState: SnackbarHostState): @Composable () -> Unit {
    return {
        SwipeableSnackbarHost(snackbarHostState) { data, modifier ->
            Snackbar(
                modifier = modifier
                    .systemBarsPadding()
                    .padding(10.dp),
                dismissAction = { },
                action = {
                    data.visuals.actionLabel?.let {
                        TextButton(
                            onClick = { data.performAction() },
                        ) {
                            Text(
                                text = data.visuals.actionLabel!!,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = data.visuals.message,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
