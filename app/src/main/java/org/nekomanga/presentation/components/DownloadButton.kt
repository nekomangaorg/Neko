package org.nekomanga.presentation.components

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download

private const val size = 24
private const val iconSize = 20
private const val borderSize = 2.5

@Composable
fun DownloadButton(buttonColor: Color, downloadStateProvider: () -> Download.State, downloadProgressProvider: () -> Float, modifier: Modifier = Modifier) {
    var downloadComplete by remember { mutableStateOf(false) }
    var wasDownloading by remember { mutableStateOf(false) }

    val downloadState = downloadStateProvider()

    LaunchedEffect(downloadState) {
        when (downloadState) {
            // this reset download complete in case you remove the chapter and want to redownload it
            Download.State.NOT_DOWNLOADED -> downloadComplete = false
            // this signals its downloading, so a future downloaded state triggers the animation
            Download.State.DOWNLOADING -> wasDownloading = true
            Download.State.DOWNLOADED -> {
                // this will run the animation for the check
                if (wasDownloading) {
                    downloadComplete = true
                    wasDownloading = false
                }
            }
            else -> Unit
        }
    }

    when (downloadState) {
        Download.State.ERROR -> NotDownloaded(MaterialTheme.colorScheme.error, modifier)
        Download.State.NOT_DOWNLOADED -> NotDownloaded(buttonColor, modifier)
        Download.State.QUEUE -> Queued(modifier)
        Download.State.DOWNLOADED -> Downloaded(buttonColor, downloadComplete, modifier)
        Download.State.DOWNLOADING -> Downloading(buttonColor, modifier, downloadProgressProvider)
        else -> Unit
    }
}

@Composable
private fun NotDownloaded(buttonColor: Color, modifier: Modifier) {
    Background(color = Color.Transparent, borderStroke = BorderStroke(borderSize.dp, buttonColor), modifier = modifier) {
        DownloadIcon(color = buttonColor, icon = rememberVectorPainter(image = Icons.Filled.ArrowDownward))
    }
}

@Composable
private fun Downloaded(buttonColor: Color, downloadComplete: Boolean, modifier: Modifier) {
    val iconPainter = rememberVectorPainter(image = Icons.Filled.ArrowDownward)

    val animatedPainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(R.drawable.anim_dl_to_check_to_dl),
        atEnd = !downloadComplete,
    )

    val painter = when (downloadComplete) {
        true -> animatedPainter
        false -> iconPainter
    }

    Background(color = buttonColor, modifier = modifier) {
        DownloadIcon(color = MaterialTheme.colorScheme.surface, icon = painter)
    }
}

@Composable
private fun Queued(modifier: Modifier) {
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
    val infinitePulse = rememberInfiniteTransition()
    val (initialState, finalState) = 0f to NekoColors.disabledAlphaLowContrast

    val alpha = infinitePulse.animateFloat(
        initialValue = initialState,
        targetValue = finalState,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCirc), repeatMode = RepeatMode.Reverse),
    )

    Background(color = Color.Transparent, modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size.dp),
            color = disabledColor,
            strokeWidth = borderSize.dp,
        )
        DownloadIcon(color = disabledColor, icon = rememberVectorPainter(image = Icons.Filled.ArrowDownward), alpha = alpha.value)
    }
}

@Composable
private fun Downloading(buttonColor: Color, modifier: Modifier, downloadProgressProvider: () -> Float) {
    val downloadProgress = downloadProgressProvider()
    val (bgColor, iconColor, progressColor) = when {
        downloadProgress >= 1f -> Triple(buttonColor, MaterialTheme.colorScheme.surface, Color.Transparent)
        else -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast), buttonColor)
    }

    val backgroundColor by animateColorAsState(targetValue = bgColor)

    val animatedProgress = animateFloatAsState(
        targetValue = downloadProgressProvider(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
    ).value

    val iconPainter = rememberVectorPainter(image = Icons.Filled.ArrowDownward)

    val infinitePulse = rememberInfiniteTransition()
    val (initialState, finalState) = 0f to NekoColors.disabledAlphaLowContrast

    val alpha = infinitePulse.animateFloat(
        initialValue = initialState,
        targetValue = finalState,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutCirc), repeatMode = RepeatMode.Reverse),
    )

    Background(color = backgroundColor, modifier = modifier) {
        CircularProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .size(size.dp),
            color = progressColor,
            strokeWidth = borderSize.dp,
        )
        DownloadIcon(color = iconColor, icon = iconPainter, alpha = alpha.value)
    }
}

@Composable
private fun DownloadIcon(color: Color, icon: Painter, alpha: Float = 1f) {
    Icon(
        painter = icon,
        contentDescription = null,
        modifier = Modifier.requiredSize(iconSize.dp),
        tint = color.copy(alpha = alpha),
    )
}

@Composable
private fun Background(color: Color, borderStroke: BorderStroke? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .size(48.dp)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
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
}
