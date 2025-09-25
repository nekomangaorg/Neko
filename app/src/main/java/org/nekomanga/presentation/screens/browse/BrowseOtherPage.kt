package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.R
import org.nekomanga.domain.DisplayResult
import org.nekomanga.presentation.components.ResultList
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.EmptyScreen

@Composable
fun BrowseOtherPage(
    results: ImmutableList<DisplayResult>,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (String) -> Unit,
) {
    if (results.isEmpty()) {
        EmptyScreen(
            message = UiText.StringResource(resourceId = R.string.no_results_found),
            contentPadding = contentPadding,
        )
    } else {
        ResultList(results = results, contentPadding = contentPadding, onClick = onClick)
    }
}
