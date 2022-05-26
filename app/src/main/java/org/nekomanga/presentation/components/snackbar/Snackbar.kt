package org.nekomanga.presentation.components.snackbar

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun snackbarHost(snackbarHostState: SnackbarHostState): @Composable () -> Unit {
    return {
        SwipeableSnackbarHost(snackbarHostState) { data, modifier ->
            Snackbar(
                modifier = modifier
                    .systemBarsPadding()
                    .padding(10.dp),
                dismissAction = { },
                dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    text = data.visuals.message,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}
