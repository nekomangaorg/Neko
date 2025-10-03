package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.domain.DisplayResult
import org.nekomanga.presentation.theme.Size

@Composable
fun ResultList(
    results: PersistentList<DisplayResult>,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (String) -> Unit = {},
) {
    val scrollState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
        state = scrollState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        items(results) { displayResult ->
            ResultRow(displayResult = displayResult, onClick = { onClick(displayResult.uuid) })
        }
    }
}

@Composable
private fun ResultRow(displayResult: DisplayResult, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = displayResult.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (displayResult.information.isNotBlank()) {
                Text(
                    text = displayResult.information,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = NekoColors.mediumAlphaLowContrast
                        ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
