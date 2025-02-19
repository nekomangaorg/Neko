package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

/** Used to */
@Composable
fun LazyListScope.SearchTermWidget(searchTerm: SearchTerm) {
    Text(
        text = searchTerm.title,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = MaterialTheme.typography.titleMedium,
    )
}

data class SearchTerm(val title: String, val subtitle: String? = null)
