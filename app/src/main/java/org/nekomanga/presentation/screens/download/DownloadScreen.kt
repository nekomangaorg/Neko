package org.nekomanga.presentation.screens.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.presentation.theme.Size

@Composable
fun DownloadScreen(downloads: ImmutableList<Download>, contentPadding: PaddingValues) {
    val scrollState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = scrollState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        items(downloads, { download -> download.chapterItem.id }) { download ->
            DownloadChapterRow(download)
        }
    }
}
