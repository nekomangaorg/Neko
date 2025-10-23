package org.nekomanga.presentation.screens.feed

import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.components.dialog.ClearDownloadQueueDialog
import org.nekomanga.presentation.components.dialog.ConfirmationDialog

@Composable
fun ClearHistoryDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.clear_history_confirmation_1),
        body = stringResource(R.string.clear_history_confirmation_2),
        confirmButton = stringResource(id = R.string.clear),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun ClearDownloadsDialog(
    scope: CoroutineScope,
    sheetStateHide: suspend () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ClearDownloadQueueDialog(
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm()
            scope.launch { sheetStateHide() }
        },
    )
}
