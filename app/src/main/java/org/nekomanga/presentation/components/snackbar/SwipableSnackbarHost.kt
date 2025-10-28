package org.nekomanga.presentation.components.snackbar

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.theme.Size

@Composable
fun SwipeableSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) },
) {

    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            val dismissState = rememberSwipeToDismissBoxState()

            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                    hostState.currentSnackbarData?.dismiss()
                }
            }

            // We must reset the dismiss state when a new snackbar appears
            LaunchedEffect(snackbarData) { dismissState.reset() }

            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.padding(horizontal = Size.medium),
                backgroundContent = {},
                content = { snackbar(snackbarData) },
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
            )
        },
    )
}
