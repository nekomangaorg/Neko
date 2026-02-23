package org.nekomanga.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.nekomanga.ui.theme.ThemedPreviews

private data class ExpandableRowState(
    val rowText: String,
    val isExpanded: Boolean,
    val disabled: Boolean,
)

private class ExpandableRowStateProvider : PreviewParameterProvider<ExpandableRowState> {
    override val values: Sequence<ExpandableRowState> =
        sequenceOf(
            ExpandableRowState("General Settings", true, false),
            ExpandableRowState("Advanced Settings", false, false),
            ExpandableRowState("Debug Mode", false, true),
        )
}

@Preview
@Composable
private fun ExpandableRowPreview(
    @PreviewParameter(ExpandableRowStateProvider::class) state: ExpandableRowState
) {
    ThemedPreviews {
        ExpandableRow(
            rowText = state.rowText,
            isExpanded = state.isExpanded,
            disabled = state.disabled,
            onClick = {},
        )
    }
}
