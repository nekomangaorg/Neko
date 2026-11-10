package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.ThemeConfig
import org.nekomanga.presentation.theme.ThemeConfigProvider
import org.nekomanga.presentation.theme.ThemedPreviews

/** State representing follow status options and selections in [PullMangaDexFollowDialog]. */
@Immutable
data class PullMangaDexFollowDialogState(
    val followStatusOptions: Set<FollowStatus> = defaultStatusOptions,
    val selectedStatuses: Set<FollowStatus> = defaultSelectedStatuses,
) {
    val canConfirm: Boolean
        get() = selectedStatuses.isNotEmpty()

    fun isSelected(status: FollowStatus): Boolean = status in selectedStatuses

    companion object {
        val defaultStatusOptions: Set<FollowStatus> =
            setOf(
                FollowStatus.READING,
                FollowStatus.COMPLETED,
                FollowStatus.ON_HOLD,
                FollowStatus.PLAN_TO_READ,
                FollowStatus.DROPPED,
                FollowStatus.RE_READING,
            )

        val defaultSelectedStatuses: Set<FollowStatus> =
            setOf(
                FollowStatus.READING,
                FollowStatus.RE_READING,
            )

        val Saver: Saver<PullMangaDexFollowDialogState, Any> =
            listSaver(
                save = { state ->
                    listOf(
                        state.followStatusOptions.map { it.name },
                        state.selectedStatuses.map { it.name },
                    )
                },
                restore = { list ->
                    val options =
                        (list[0] as List<*>)
                            .filterIsInstance<String>()
                            .map { FollowStatus.valueOf(it) }
                            .toSet()
                    val selected =
                        (list[1] as List<*>)
                            .filterIsInstance<String>()
                            .map { FollowStatus.valueOf(it) }
                            .toSet()
                    PullMangaDexFollowDialogState(
                        followStatusOptions = options,
                        selectedStatuses = selected,
                    )
                },
            )
    }
}

/** Stateless dialog for pulling follows from MangaDex into the library. */
@Composable
fun PullMangaDexFollowDialog(
    state: PullMangaDexFollowDialogState,
    onStatusToggled: (FollowStatus) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        title = { Text(text = stringResource(id = R.string.pull_follows_to_library)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                state.followStatusOptions.forEach { status ->
                    val isChecked = state.isSelected(status)
                    CheckboxRow(
                        checkedState = isChecked,
                        rowText = stringResource(status.stringRes),
                        checkedChange = { onStatusToggled(status) },
                    )
                }
                Gap(Size.extraTiny)
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = state.canConfirm,
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

/**
 * Stateful wrapper around [PullMangaDexFollowDialog] managing status selections with domain types.
 */
@JvmName("PullMangaDexFollowDialogStatuses")
@Composable
fun PullMangaDexFollowDialog(
    onDismiss: () -> Unit,
    onConfirmStatuses: (Set<FollowStatus>) -> Unit,
    initialState: PullMangaDexFollowDialogState = PullMangaDexFollowDialogState(),
) {
    var state by
        rememberSaveable(initialState, stateSaver = PullMangaDexFollowDialogState.Saver) {
            mutableStateOf(initialState)
        }

    PullMangaDexFollowDialog(
        state = state,
        onStatusToggled = { status ->
            val updated =
                if (state.isSelected(status)) {
                    state.selectedStatuses - status
                } else {
                    state.selectedStatuses + status
                }
            state = state.copy(selectedStatuses = updated)
        },
        onDismissRequest = onDismiss,
        onConfirm = {
            onConfirmStatuses(state.selectedStatuses)
            onDismiss()
        },
    )
}

/**
 * Legacy overload for [PullMangaDexFollowDialog] adapting to raw string preference serialization.
 */
@Composable
fun PullMangaDexFollowDialog(
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    initialState: PullMangaDexFollowDialogState = PullMangaDexFollowDialogState(),
) {
    PullMangaDexFollowDialog(
        onDismiss = onDismiss,
        onConfirmStatuses = { statuses -> onConfirm(statuses.map { it.int.toString() }.toSet()) },
        initialState = initialState,
    )
}

@Preview
@Composable
private fun PullMangaDexFollowDialogPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig,
) {
    ThemedPreviews(themeConfig) {
        PullMangaDexFollowDialog(
            state = PullMangaDexFollowDialogState(),
            onStatusToggled = {},
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun PullMangaDexFollowDialogEmptyPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig,
) {
    ThemedPreviews(themeConfig) {
        PullMangaDexFollowDialog(
            state = PullMangaDexFollowDialogState(selectedStatuses = emptySet()),
            onStatusToggled = {},
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
