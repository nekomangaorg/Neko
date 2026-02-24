package org.nekomanga.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.nekomanga.ui.theme.Themed
import org.nekomanga.ui.theme.ThemedPreviews
import org.nekomanga.ui.theme.withThemes

private data class ExpandableRowState(
    val rowText: String,
    val isExpanded: Boolean,
    val disabled: Boolean,
)

private class ExpandableRowStateProvider : PreviewParameterProvider<Themed<ExpandableRowState>> {
    override val values: Sequence<Themed<ExpandableRowState>> =
        sequenceOf(
                ExpandableRowState("General Settings", true, false),
                ExpandableRowState("Advanced Settings", false, false),
                ExpandableRowState("Debug Mode", false, true),
            )
            .withThemes()
}

@Preview
@Composable
private fun ExpandableRowPreview(
    @PreviewParameter(ExpandableRowStateProvider::class) themedState: Themed<ExpandableRowState>
) {
    ThemedPreviews(themedState.themeConfig) {
        val state = themedState.value
        ExpandableRow(
            rowText = state.rowText,
            isExpanded = state.isExpanded,
            disabled = state.disabled,
            onClick = {},
        )
    }
}
