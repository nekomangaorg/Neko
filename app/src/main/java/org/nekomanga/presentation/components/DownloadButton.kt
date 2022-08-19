package org.nekomanga.presentation.components

import CombinedClickableIconButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCirc
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download

private const val size = 24
private const val iconSize = 20
private const val borderSize = 2.5

@Composable
fun DownloadButton(buttonColor: Color, downloadStateProvider: () -> Download.State, downloadProgressProvider: () -> Float, modifier: Modifier = Modifier) {
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
    val veryLowContrast = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.veryLowContrast)
    val errorColor = MaterialTheme.colorScheme.errorContainer
    val errorIconColor = MaterialTheme.colorScheme.onErrorContainer

    val animatedProgress = animateFloatAsState(
        targetValue = downloadProgressProvider(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
    ).value

    var downloadComplete by remember { mutableStateOf(false) }
    var wasDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(downloadStateProvider()) {
        when (downloadStateProvider()) {
            //this reset download complete in case you remove the chapter and want to redownload it
            Download.State.NOT_DOWNLOADED -> downloadComplete = false
            //this signals its downloading, so a future downloaded state triggers the animation
            Download.State.DOWNLOADING -> wasDownloading = true
            Download.State.DOWNLOADED -> {
                //this will run the animation for the check
                if (wasDownloading) {
                    downloadComplete = !downloadComplete
                    wasDownloading = false
                }
            }
            else -> Unit
        }
    }

    val downloadCompletePainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(R.drawable.anim_dl_to_check_to_dl),
        atEnd = !downloadComplete,
    )

    val infinitePulse = rememberInfiniteTransition()
    val (initialState, finalState) = when (downloadStateProvider().isActive() && downloadProgressProvider() != 1f) {
        true -> 0f to NekoColors.disabledAlphaLowContrast
        false -> 1f to 1f
    }

    val alpha = infinitePulse.animateFloat(
        initialValue = initialState,
        targetValue = finalState,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCirc), repeatMode = RepeatMode.Reverse),
    )


    CombinedClickableIconButton(
        modifier = Modifier
            .clip(CircleShape)
            .then(modifier),
    ) {
        val surfaceColor = MaterialTheme.colorScheme.surface
        val download = remember(downloadStateProvider(), downloadProgressProvider(), buttonColor) {
            when (downloadStateProvider()) {
                Download.State.CHECKED -> DownloadHolder(Icons.Filled.Check, surfaceColor, buttonColor, Color.Transparent)
                Download.State.ERROR -> DownloadHolder(Icons.Filled.Cancel, errorIconColor, errorColor, Color.Transparent)
                Download.State.QUEUE -> DownloadHolder(Icons.Filled.ArrowDownward, disabledColor, Color.Transparent, disabledColor)
                Download.State.DOWNLOADED -> DownloadHolder(Icons.Filled.ArrowDownward, surfaceColor, buttonColor, Color.Transparent)
                Download.State.DOWNLOADING -> {
                    if (downloadProgressProvider() >= 1f) {
                        DownloadHolder(Icons.Filled.ArrowDownward, surfaceColor, buttonColor, Color.Transparent)
                    } else {
                        DownloadHolder(Icons.Filled.ArrowDownward, disabledColor, Color.Transparent, buttonColor)
                    }
                }
                Download.State.NOT_DOWNLOADED -> DownloadHolder(Icons.Filled.ArrowDownward, buttonColor, Color.Transparent, Color.Transparent)
            }
        }

        val borderStroke = when (downloadStateProvider() == Download.State.NOT_DOWNLOADED) {
            true -> BorderStroke(borderSize.dp, buttonColor)
            false -> null
        }
        val backgroundColor by animateColorAsState(targetValue = download.backgroundColor)

        Background(color = backgroundColor, borderStroke = borderStroke) {
            DownloadIcon(color = download.iconColor, alpha = alpha.value, icon = if (!downloadComplete) rememberVectorPainter(image = download.icon) else downloadCompletePainter)
            if (downloadStateProvider() == Download.State.QUEUE) {
                DownloadProgress(color = download.progressColor)
            } else if (downloadStateProvider() == Download.State.DOWNLOADING) {
                DownloadProgress(determinate = true, color = veryLowContrast, progress = 1f)
                DownloadProgress(determinate = true, color = download.progressColor, progress = animatedProgress)
            }
        }
    }
}

@Composable
private fun Background(color: Color, borderStroke: BorderStroke? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .size(size.dp),
        shape = CircleShape,
        border = borderStroke,
        color = color,
    ) {
        content()
    }
}

@Composable
private fun DownloadIcon(color: Color, icon: Painter, alpha: Float) {
    Icon(
        painter = icon,
        contentDescription = null,
        modifier = Modifier.requiredSize(iconSize.dp),
        tint = color.copy(alpha = alpha),
    )
}

@Composable
private fun DownloadProgress(determinate: Boolean = false, progress: Float = 0f, color: Color) {
    if (determinate) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier
                .size(size.dp),
            color = color,
            strokeWidth = borderSize.dp,
        )
    } else {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size.dp),
            color = color,
            strokeWidth = borderSize.dp,
        )
    }
}

private data class DownloadHolder(val icon: ImageVector, val iconColor: Color, val backgroundColor: Color, val progressColor: Color)
