package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

interface SearchTermProvider {
    @Composable fun getSearchTerms(): ImmutableList<SearchTerm>
}
