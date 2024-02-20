package org.nekomanga.presentation.screens.download

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.data.download.model.Download
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoSwipeToDismiss
import org.nekomanga.presentation.theme.Size

@Composable
fun DownloadChapterRow(download: Download) {
    val dismissState = rememberDismissState(initialValue = DismissValue.Default)
    NekoSwipeToDismiss(
        state = dismissState,
        modifier = Modifier.padding(vertical = Dp(1f)),
        background = {
            val alignment =
                when (dismissState.dismissDirection) {
                    DismissDirection.EndToStart -> Alignment.CenterEnd
                    DismissDirection.StartToEnd -> Alignment.CenterStart
                    else -> null
                }
            Background(alignment!!)
        },
        dismissContent = { Text(text = "Hello") },
    )
    when {
        dismissState.isDismissed(DismissDirection.EndToStart) -> Unit
        //  Reset(dismissState = dismissState, action = onRead)
        dismissState.isDismissed(DismissDirection.StartToEnd) -> Unit
    //   Reset(dismissState = dismissState, action = onBookmark)
    }
}

@Composable
private fun Background(alignment: Alignment) {
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = Size.large),
        contentAlignment = alignment,
    ) {
        Column(modifier = Modifier.align(alignment)) {
            Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = stringResource(id = R.string.remove_download),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
