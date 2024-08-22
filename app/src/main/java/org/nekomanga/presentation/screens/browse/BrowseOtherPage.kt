package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.DisplayResult
import org.nekomanga.presentation.components.ResultList
import org.nekomanga.presentation.screens.NoResultsEmptyScreen

@Composable
fun BrowseOtherPage(
    results: ImmutableList<DisplayResult>,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (DisplayResult) -> Unit
) {
    if (results.isEmpty()) {
        NoResultsEmptyScreen(contentPaddingValues = contentPadding)
    } else {
        ResultList(
            results = results,
            contentPadding = contentPadding,
            onClick = onClick,
        )
    }
}
