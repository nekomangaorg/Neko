package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.ui.feed.FeedScreenState
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import org.nekomanga.R
import org.nekomanga.presentation.components.ButtonGroup
import org.nekomanga.presentation.theme.Size

@Composable
internal fun FeedBottomBar(
    feedScreenState: FeedScreenState,
    showDownloads: Boolean,
    onTabClick: (Any) -> Unit,
) {
    val buttonItems = remember {
        listOf(FeedScreenType.Summary, FeedScreenType.History, FeedScreenType.Updates)
    }

    val downloadButton = "downloads"
    val items: List<Any> =
        if (showDownloads) {
            buttonItems + downloadButton
        } else {
            buttonItems
        }

    val selectedItem: Any =
        when (feedScreenState.showingDownloads) {
            true -> downloadButton
            false -> feedScreenState.feedScreenType
        }

    Box(modifier = Modifier.fillMaxSize()) {
        ButtonGroup(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = Size.tiny),
            items = items,
            selectedItem = selectedItem,
            onItemClick = onTabClick,
        ) { item ->
            when (item) {
                is FeedScreenType -> {
                    val name =
                        when (item) {
                            FeedScreenType.History -> stringResource(R.string.history)
                            FeedScreenType.Updates -> stringResource(R.string.updates)
                            FeedScreenType.Summary -> stringResource(R.string.summary)
                        }
                    Text(
                        text = name,
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Downloading,
                        contentDescription = stringResource(id = R.string.downloads),
                    )
                }
            }
        }
    }
}
