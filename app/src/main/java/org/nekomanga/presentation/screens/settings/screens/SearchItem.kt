package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

interface SearchTermProvider {
    @Composable fun getSearchTerms(): PersistentList<SearchTerm>
}
