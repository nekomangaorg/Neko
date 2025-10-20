package org.nekomanga.presentation.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseInOutCirc
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import kotlin.math.sin
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

private const val iconSize = 24
private const val borderSize = 2.0

@Composable
fun DownloadButton(
    modifier: Modifier = Modifier,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    downloadState: Download.State,
    downloadProgress: Int,
    onClick: (MangaConstants.DownloadAction) -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        var showDropdown by remember { mutableStateOf(false) }

        var wasDownloading by remember { mutableStateOf(false) }
        var isDraining by remember { mutableStateOf(false) }
        var isDownloaded by remember { mutableStateOf(downloadState == Download.State.DOWNLOADED) }

        LaunchedEffect(downloadState) {
            when (downloadState) {
                Download.State.DOWNLOADING -> {
                    wasDownloading = true
                    isDraining = false
                    isDownloaded = false
                }
                Download.State.DOWNLOADED -> {
                    if (wasDownloading) {
                        isDownloaded = true
                    }
                    isDraining = false
                    wasDownloading = false
                }
                Download.State.NOT_DOWNLOADED -> {
                    if (wasDownloading || isDownloaded) {
                        isDraining = true
                    }
                    wasDownloading = false
                    isDownloaded = false
                }
                else -> {
                    isDraining = false
                    wasDownloading = false
                }
            }
        }

        val downloadButtonModifier =
            Modifier.combinedClickable(
                onClick = {
                    when (downloadState) {
                        Download.State.NOT_DOWNLOADED ->
                            onClick(MangaConstants.DownloadAction.Download)
                        else -> showDropdown = true
                    }
                }
            )

        when {
            isDraining -> {
                Draining(
                    themeColorState = themeColorState,
                    modifier = downloadButtonModifier,
                    onAnimationComplete = { isDraining = false },
                )
            }
            downloadState == Download.State.ERROR -> {
                NotDownloaded(
                    themeColorState =
                        themeColorState.copy(primaryColor = MaterialTheme.colorScheme.error),
                    modifier = downloadButtonModifier,
                )
            }
            downloadState == Download.State.NOT_DOWNLOADED -> {
                NotDownloaded(themeColorState = themeColorState, modifier = downloadButtonModifier)
            }
            downloadState == Download.State.QUEUE -> {
                Queued(themeColorState = themeColorState, modifier = downloadButtonModifier)
            }
            downloadState == Download.State.DOWNLOADED -> {
                Downloaded(
                    themeColorState = themeColorState,
                    isDownloaded = isDownloaded,
                    modifier = downloadButtonModifier,
                    onAnimationComplete = { isDownloaded = false },
                )
            }
            downloadState == Download.State.DOWNLOADING -> {
                Downloading(
                    themeColorState = themeColorState,
                    modifier = downloadButtonModifier,
                    progress = downloadProgress,
                )
            }
        }

        SimpleDropdownMenu(
            expanded = showDropdown,
            themeColorState = themeColorState,
            onDismiss = { showDropdown = false },
            dropDownItems =
                if (downloadState == Download.State.DOWNLOADED) {
                    persistentListOf(
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.remove),
                            onClick = {
                                onClick(MangaConstants.DownloadAction.Remove)
                                showDropdown = false
                            },
                        )
                    )
                } else if (
                    downloadState == Download.State.QUEUE ||
                        downloadState == Download.State.DOWNLOADING
                ) {
                    persistentListOf(
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.start_downloading_now),
                            onClick = {
                                onClick(MangaConstants.DownloadAction.ImmediateDownload)
                                showDropdown = false
                            },
                        ),
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.cancel),
                            onClick = {
                                onClick(MangaConstants.DownloadAction.Cancel)
                                showDropdown = false
                            },
                        ),
                    )
                } else {
                    persistentListOf()
                },
        )
    }
}

@Composable
private fun NotDownloaded(themeColorState: ThemeColorState, modifier: Modifier) {
    Background(modifier = modifier, themeColorState = themeColorState) {
        DownloadIcon(
            color = themeColorState.primaryColor,
            icon = rememberVectorPainter(image = Icons.Filled.ArrowDownward),
        )
    }
}

@Composable
private fun Downloaded(
    themeColorState: ThemeColorState,
    isDownloaded: Boolean,
    modifier: Modifier,
    onAnimationComplete: () -> Unit,
) {
    val transition = rememberTransition(targetState = isDownloaded, label = "downloadedTransition")

    LaunchedEffect(isDownloaded) {
        if (isDownloaded) {
            onAnimationComplete()
        }
    }

    val rotation by
        transition.animateFloat(
            label = "rotation",
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 1000, easing = LinearEasing)
                } else {
                    tween(durationMillis = 1000, easing = LinearEasing, delayMillis = 500)
                }
            },
        ) { downloaded ->
            when (downloaded) {
                true -> 360f
                false -> 0f
            }
        }

    val icon =
        when {
            transition.isRunning || isDownloaded -> Icons.Filled.Check
            else -> Icons.Filled.ArrowDownward
        }

    Background(
        modifier = modifier.rotate(rotation),
        themeColorState = themeColorState,
        filled = true,
    ) {
        DownloadIcon(
            color = MaterialTheme.colorScheme.surface,
            icon = rememberVectorPainter(image = icon),
        )
    }
}

@Composable
private fun Queued(themeColorState: ThemeColorState, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "queuedPulse")

    val surfaceAlpha by
        infiniteTransition.animateFloat(
            initialValue = .25f,
            targetValue = .5f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutCirc),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "surfaceAlpha",
        )

    val iconAlpha by
        infiniteTransition.animateFloat(
            initialValue = .5f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutCirc),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "iconAlpha",
        )

    Background(
        modifier = modifier,
        themeColorState = themeColorState,
        forceColor = themeColorState.primaryColor.copy(alpha = surfaceAlpha),
    ) {
        DownloadIcon(
            color = themeColorState.primaryColor,
            icon = rememberVectorPainter(image = Icons.Filled.ArrowDownward),
            alpha = iconAlpha,
        )
    }
}

@Composable
private fun Downloading(themeColorState: ThemeColorState, modifier: Modifier, progress: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "downloadingRotation")

    val rotation by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rotation",
        )

    Background(
        modifier = modifier.rotate(rotation),
        themeColorState = themeColorState,
        filled = false,
    ) {
        Sloshing(progress = progress, themeColorState = themeColorState)
    }
}

@Composable
private fun Draining(
    themeColorState: ThemeColorState,
    modifier: Modifier,
    onAnimationComplete: () -> Unit,
) {
    var startDraining by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { startDraining = true }

    val transition = rememberTransition(targetState = startDraining, label = "draining")

    val progress by
        transition.animateFloat(
            label = "progress",
            transitionSpec = { tween(durationMillis = 500) },
        ) { draining ->
            if (draining) 0f else 100f
        }

    val rotation by
        transition.animateFloat(
            label = "rotation",
            transitionSpec = { tween(durationMillis = 500) },
        ) { draining ->
            if (draining) -360f else 0f
        }

    val showArrow by
        transition.animateColor(
            label = "arrowColor",
            transitionSpec = { tween(durationMillis = 250, delayMillis = 250) },
        ) { draining ->
            if (draining) Color.Transparent else themeColorState.primaryColor
        }

    if (!transition.isRunning && transition.currentState) {
        LaunchedEffect(Unit) { onAnimationComplete() }
    }

    Background(
        modifier = modifier.rotate(rotation),
        themeColorState = themeColorState,
        filled = false,
    ) {
        Sloshing(progress = progress.toInt(), themeColorState = themeColorState)
        DownloadIcon(
            color = showArrow,
            icon = rememberVectorPainter(image = Icons.Filled.ArrowDownward),
        )
    }
}

@Composable
private fun Sloshing(progress: Int, themeColorState: ThemeColorState) {
    val infiniteTransition = rememberInfiniteTransition(label = "sloshing")
    val waveOffset by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "wave",
        )

    val color = themeColorState.primaryColor

    Canvas(modifier = Modifier.size(Size.ExtraLarge)) {
        val width = size.width
        val height = size.height
        val fillHeight = height * (progress / 100f)

        val path = Path()
        path.moveTo(0f, height)
        path.lineTo(0f, height - fillHeight)

        if (progress > 0) {
            val amplitude = 10f
            val frequency = 2.5f
            for (x in 0..width.toInt()) {
                val y =
                    (amplitude * sin((x / width) * frequency * Math.PI + waveOffset * Math.PI))
                        .toFloat() + (height - fillHeight)
                path.lineTo(x.toFloat(), y)
            }
        }

        path.lineTo(width, height)
        path.close()

        clipRect { drawPath(path = path, color = color) }
    }
}

@Composable
private fun DownloadIcon(
    color: Color,
    icon: androidx.compose.ui.graphics.painter.Painter,
    alpha: Float = 1f,
) {
    Icon(
        painter = icon,
        contentDescription = null,
        modifier = Modifier.requiredSize(iconSize.dp),
        tint = color.copy(alpha = alpha),
    )
}

@Composable
private fun Background(
    modifier: Modifier,
    themeColorState: ThemeColorState,
    filled: Boolean = false,
    forceColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val color = forceColor ?: if (filled) themeColorState.primaryColor else Color.Transparent

    Box(modifier = Modifier.size(Size.ExtraLarge + 4.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = modifier.clip(MaterialTheme.shapes.extraLarge).size(Size.ExtraLarge),
            shape = MaterialTheme.shapes.extraLarge,
            border =
                if (!filled) {
                    androidx.compose.foundation.BorderStroke(
                        borderSize.dp,
                        themeColorState.primaryColor,
                    )
                } else {
                    null
                },
            color = color,
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}
