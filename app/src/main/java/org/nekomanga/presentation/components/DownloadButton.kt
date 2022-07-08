package org.nekomanga.presentation.components

import CombinedClickableIconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download

@Composable
fun DownloadButton(buttonColor: Color, state: Download.State, progress: Float, modifier: Modifier = Modifier) {
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
    val errorColor = MaterialTheme.colorScheme.errorContainer
    val errorIconColor = MaterialTheme.colorScheme.onErrorContainer

    CombinedClickableIconButton(modifier = modifier) {

        val download = when (state) {
            Download.State.CHECKED -> DownloadHolder(Icons.Filled.Check, MaterialTheme.colorScheme.surface, buttonColor, Color.Transparent)
            Download.State.ERROR -> DownloadHolder(Icons.Filled.Cancel, errorIconColor, errorColor, Color.Transparent)
            Download.State.QUEUE -> DownloadHolder(Icons.Filled.ArrowDownward, disabledColor, Color.Transparent, disabledColor)
            Download.State.DOWNLOADED -> DownloadHolder(Icons.Filled.ArrowDownward, MaterialTheme.colorScheme.surface, buttonColor, Color.Transparent)
            Download.State.DOWNLOADING -> DownloadHolder(Icons.Filled.ArrowDownward, disabledColor, Color.Transparent, buttonColor)
            Download.State.NOT_DOWNLOADED -> DownloadHolder(Icons.Filled.ArrowDownward, buttonColor, Color.Transparent, Color.Transparent)
        }

        val borderStroke = when (state == Download.State.NOT_DOWNLOADED) {
            true -> BorderStroke(2.dp, buttonColor)
            false -> null
        }

        Surface(
            modifier = Modifier
                .size(28.dp),
            shape = CircleShape,
            border = borderStroke,
            color = download.backgroundColor,
        ) {
            Icon(
                imageVector = download.icon, contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.Transparent),
                tint = download.iconColor,
            )
        }
        if (state == Download.State.NOT_DOWNLOADED) {
            if (progress < 0) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp),
                    color = download.progressColor, strokeWidth = 3.dp,
                )
            } else {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .size(28.dp),
                    color = download.progressColor, strokeWidth = 3.dp,
                )
            }
        } else if (state != Download.State.DOWNLOADED && state != Download.State.ERROR && state != Download.State.CHECKED) {
            if (progress != 0f) {
                if (progress < 0) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp),
                        color = download.progressColor, strokeWidth = 3.dp,
                    )
                } else {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .size(28.dp),
                        color = download.progressColor, strokeWidth = 3.dp,
                    )
                }
            }
        }
    }
}

private data class DownloadHolder(val icon: ImageVector, val iconColor: Color, val backgroundColor: Color, val progressColor: Color)
