package org.nekomanga.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText

@Preview
@Composable
private fun EmptyViewPreview() {
    EmptyScreen(
        message = UiText.StringResource(R.string.no_results_found),
        actions = listOf(Action(UiText.StringResource(R.string.retry))),
    )
}
