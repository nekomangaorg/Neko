package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.theme.Size
import org.nekomanga.ui.theme.Themed
import org.nekomanga.ui.theme.ThemedPreviews
import org.nekomanga.ui.theme.withThemes

@Preview
@Composable
private fun TriStateCheckboxRowPreview(
    @PreviewParameter(ToggleableStateProvider::class) themedState: Themed<ToggleableState>
) {
    ThemedPreviews(themeConfig = themedState.themeConfig) {
        Column {
            TriStateCheckboxRow(
                state = themedState.value,
                toggleState = {},
                rowText = "TriState Checkbox: ${themedState.value}",
            )
            Gap(Size.small)
            TriStateCheckboxRow(
                state = themedState.value,
                toggleState = {},
                rowText = "Disabled Checkbox: ${themedState.value}",
                disabled = true,
            )
        }
    }
}

@Preview
@Composable
private fun TriStateFilterChipPreview(
    @PreviewParameter(ToggleableStateProvider::class) themedState: Themed<ToggleableState>
) {
    ThemedPreviews(themeConfig = themedState.themeConfig) {
        Column {
            TriStateFilterChip(
                state = themedState.value,
                toggleState = {},
                name = "Filter Chip: ${themedState.value}",
            )
            Gap(Size.small)
            TriStateFilterChip(
                state = themedState.value,
                toggleState = {},
                name = "Hidden Icons: ${themedState.value}",
                hideIcons = true,
            )
        }
    }
}

private class ToggleableStateProvider : PreviewParameterProvider<Themed<ToggleableState>> {
    override val values: Sequence<Themed<ToggleableState>> =
        sequenceOf(ToggleableState.On, ToggleableState.Off, ToggleableState.Indeterminate)
            .withThemes()
}
