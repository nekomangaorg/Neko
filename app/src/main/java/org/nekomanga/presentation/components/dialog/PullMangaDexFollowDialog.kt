package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.theme.Size

/** Dialog for creating a backup */
@Composable
fun PullMangaDexFollowDialog(onDismiss: () -> Unit, onConfirm: (Set<String>) -> Unit) {
    val followStatusOptions = remember {
        setOf(
            FollowStatus.READING,
            FollowStatus.COMPLETED,
            FollowStatus.ON_HOLD,
            FollowStatus.PLAN_TO_READ,
            FollowStatus.DROPPED,
            FollowStatus.RE_READING,
        )
    }

    var selectedStatuses by remember {
        mutableStateOf(setOf(FollowStatus.READING, FollowStatus.RE_READING))
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.pull_follows_to_library)) },
        text = {
            Column {
                followStatusOptions.forEach { status ->
                    val isChecked = status in selectedStatuses
                    CheckboxRow(
                        checkedState = isChecked,
                        rowText = stringResource(status.stringRes),
                        checkedChange = { checked ->
                            selectedStatuses =
                                when (checked) {
                                    true -> selectedStatuses + status
                                    false -> selectedStatuses - status
                                }
                        },
                    )
                }
                Gap(Size.extraTiny)
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    selectedStatuses.map { status -> status.int }.toSet()

                    onConfirm(selectedStatuses.map { status -> status.int.toString() }.toSet())
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.cancel)) }
        },
    )
}
