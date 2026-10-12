package org.nekomanga.presentation.components.snackbar

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CancellationException
import org.nekomanga.domain.snackbar.SnackbarState

/**
 * Shows [event] and runs its action or its dismiss action, depending on how the snackbar ends. A
 * snackbar cancelled because its screen was closed counts as dismissed, so work held back for the
 * undo window, such as deleting downloads, still runs.
 */
suspend fun SnackbarHostState.showSnackbarEvent(
    event: SnackbarState,
    message: String,
    actionLabel: String?,
) {
    val result =
        try {
            showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = event.snackbarDuration,
                withDismissAction = true,
            )
        } catch (e: CancellationException) {
            event.dismissAction?.invoke()
            throw e
        }
    when (result) {
        SnackbarResult.ActionPerformed -> event.action?.invoke()
        SnackbarResult.Dismissed -> event.dismissAction?.invoke()
    }
}
