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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.core.util.launchDelayed
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

private const val iconSize = 20
private const val borderSize = 2.5

@Composable
fun DownloadButton(
    modifier: Modifier = Modifier,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    downloadState: Download.State,
    downloadProgress: Int,
    defaultDisableColor: Boolean = false,
    onDownload: (MangaConstants.DownloadAction) -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        var showChapterDropdown by remember { mutableStateOf(false) }

        var downloadComplete by remember { mutableStateOf(false) }
        var wasDownloading by remember { mutableStateOf(false) }

        LaunchedEffect(downloadState) {
            when (downloadState) {
                // this reset download complete in case you remove the chapter and want to
                // redownload it
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

        val downloadButtonModifier =
            Modifier.combinedClickable(
                onClick = {
                    when (downloadState) {
                        Download.State.NOT_DOWNLOADED ->
                            onDownload(MangaConstants.DownloadAction.Download)
                        else -> showChapterDropdown = true
                    }
                },
                onLongClick = {},
            )

        when (downloadState) {
            Download.State.ERROR ->
                NotDownloaded(
                    buttonColor = MaterialTheme.colorScheme.error,
                    modifier = downloadButtonModifier,
                )
            Download.State.NOT_DOWNLOADED ->
                NotDownloaded(
                    buttonColor = themeColorState.buttonColor,
                    modifier = downloadButtonModifier,
                    defaultDisableColor = defaultDisableColor,
                )
            Download.State.QUEUE -> Queued(modifier = downloadButtonModifier)
            Download.State.DOWNLOADED ->
                Downloaded(
                    buttonColor = themeColorState.buttonColor,
                    downloadComplete = downloadComplete,
                    modifier = downloadButtonModifier,
                )
            Download.State.DOWNLOADING ->
                Downloading(
                    buttonColor = themeColorState.buttonColor,
                    modifier = downloadButtonModifier,
                    downloadProgress = downloadProgress,
                )
        }

        val scope = rememberCoroutineScope()
        SimpleDropdownMenu(
            expanded = showChapterDropdown,
            themeColorState = themeColorState,
            onDismiss = { showChapterDropdown = false },
            dropDownItems =
                when (downloadState) {
                    Download.State.DOWNLOADED -> {
                        persistentListOf(
                            SimpleDropDownItem.Action(
                                text = UiText.StringResource(R.string.remove),
                                onClick = {
                                    scope.launchDelayed {
                                        onDownload(MangaConstants.DownloadAction.Remove)
                                    }
                                },
                            )
                        )
                    }
                    else -> {
                        persistentListOf(
                            SimpleDropDownItem.Action(
                                text = UiText.StringResource(R.string.start_downloading_now),
                                onClick = {
                                    scope.launchDelayed {
                                        onDownload(MangaConstants.DownloadAction.ImmediateDownload)
                                    }
                                },
                            ),
                            SimpleDropDownItem.Action(
                                text = UiText.StringResource(R.string.cancel),
                                onClick = {
                                    scope.launchDelayed {
                                        onDownload(MangaConstants.DownloadAction.Cancel)
                                    }
                                },
                            ),
                        )
                    }
                },
        )
    }
}

@Composable
private fun NotDownloaded(
    buttonColor: Color,
    modifier: Modifier,
    defaultDisableColor: Boolean = false,
) {

    val disabledColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)
    val (color, alpha) =
        when (defaultDisableColor) {
            true -> disabledColor to NekoColors.disabledAlphaLowContrast
            false -> buttonColor to 1f
        }

    Background(
        color = Color.Transparent,
        borderStroke = BorderStroke(borderSize.dp, color),
        modifier = modifier,
    ) {
        DownloadIcon(
            color = color,
            alpha = alpha,
            icon = rememberVectorPainter(image = Icons.Filled.ArrowDownward),
        )
    }
}

@Composable
private fun Downloaded(buttonColor: Color, downloadComplete: Boolean, modifier: Modifier) {
    val iconPainter = rememberVectorPainter(image = Icons.Filled.ArrowDownward)

    val animatedPainter =
        rememberAnimatedVectorPainter(
            animatedImageVector =
                AnimatedImageVector.animatedVectorResource(R.drawable.anim_dl_to_check_to_dl),
            atEnd = !downloadComplete,
        )

    val painter =
        when (downloadComplete) {
            true -> animatedPainter
            false -> iconPainter
        }

    Background(color = buttonColor, modifier = modifier) {
        DownloadIcon(color = MaterialTheme.colorScheme.surface, icon = painter)
    }
}

@Composable
private fun Queued(modifier: Modifier) {
    val disabledColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
    val infinitePulse = rememberInfiniteTransition(label = "queuedPulse")
    val (initialState, finalState) = 0f to NekoColors.disabledAlphaLowContrast

    val alpha =
        infinitePulse.animateFloat(
            initialValue = initialState,
            targetValue = finalState,
            animationSpec =
                infiniteRepeatable(
                    tween(1000, easing = EaseInOutCirc),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "queuedAlpha",
        )

    Background(color = Color.Transparent, modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier.size(Size.large),
            color = disabledColor,
            strokeWidth = borderSize.dp,
        )
        DownloadIcon(
            color = disabledColor,
            icon = rememberVectorPainter(image = Icons.Filled.ArrowDownward),
            alpha = alpha.value,
        )
    }
}

@Composable
private fun Downloading(buttonColor: Color, modifier: Modifier, downloadProgress: Int) {
    val (bgColor, iconColor, progressColor) =
        when {
            downloadProgress >= Download.MaxProgress ->
                Triple(buttonColor, MaterialTheme.colorScheme.surface, Color.Transparent)
            else -> {
                val disabledColor =
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = NekoColors.disabledAlphaHighContrast
                    )
                Triple(Color.Transparent, disabledColor, buttonColor)
            }
        }

    val backgroundColor by
        animateColorAsState(targetValue = bgColor, label = "downloadingBackgroundColor")

    val animatedProgress by
        animateFloatAsState(
            targetValue = (downloadProgress / Download.MaxProgress.toFloat()),
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "downloadingProgress",
        )

    val iconPainter = rememberVectorPainter(image = Icons.Filled.ArrowDownward)

    val infinitePulse = rememberInfiniteTransition(label = "downloadPulse")
    val (initialState, finalState) = 0f to NekoColors.disabledAlphaLowContrast

    val alpha =
        infinitePulse.animateFloat(
            initialValue = initialState,
            targetValue = finalState,
            animationSpec =
                infiniteRepeatable(
                    tween(1000, easing = EaseInOutCirc),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "downloadAlphaPulse",
        )

    Background(color = backgroundColor, modifier = modifier) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(Size.large),
            color = progressColor,
            trackColor = progressColor.copy(alpha = .4f),
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
private fun Background(
    color: Color,
    borderStroke: BorderStroke? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.clip(CircleShape).size(Size.huge).then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(Size.large),
            shape = CircleShape,
            border = borderStroke,
            color = color,
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}
