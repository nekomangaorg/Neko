package org.nekomanga.presentation.screens.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.feed.MoveDownloadDirection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import me.saket.swipe.SwipeAction
import org.nekomanga.R
import org.nekomanga.domain.download.DownloadItem
import org.nekomanga.presentation.components.ChapterSwipe
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun DownloadChapterRow(
    modifier: Modifier = Modifier,
    first: Boolean,
    last: Boolean,
    chapter: DownloadItem,
    downloadSwiped: () -> Unit,
    moveDownloadClicked: (MoveDownloadDirection) -> Unit,
    moveSeriesClicked: (MoveDownloadDirection) -> Unit,
    cancelSeriesClicked: () -> Unit,
) {
    val swipeAction =
        SwipeAction(
            icon = rememberVectorPainter(Icons.Filled.DeleteForever),
            background = MaterialTheme.colorScheme.secondary,
            onSwipe = downloadSwiped,
        )

    ChapterSwipe(
        modifier = modifier.padding(vertical = Dp(1f)),
        startSwipeActions = listOf(swipeAction),
        endSwipeActions = listOf(swipeAction),
    ) {
        ChapterRow(
            first,
            last,
            chapter,
            moveDownloadClicked,
            moveSeriesClicked,
            cancelSeriesClicked,
        )
    }
}

@Composable
private fun ChapterRow(
    first: Boolean,
    last: Boolean,
    download: DownloadItem,
    moveDownloadClicked: (MoveDownloadDirection) -> Unit,
    moveSeriesClicked: (MoveDownloadDirection) -> Unit,
    cancelSeriesClicked: () -> Unit,
) {

    var dropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Size.small, bottom = Size.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier.align(Alignment.CenterVertically)
                    .padding(horizontal = Size.medium)
                    .fillMaxWidth(.9f),
            verticalArrangement = Arrangement.spacedBy(Size.tiny),
        ) {
            Text(
                text = download.mangaItem.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = download.chapterItem.chapter.name,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = NekoColors.mediumAlphaLowContrast
                            )
                    ),
                maxLines = 1,
            )
            val strokeWidth = with(LocalDensity.current) { Size.tiny.toPx() }
            val stroke =
                remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
            when (download.chapterItem.downloadState == Download.State.QUEUE) {
                true ->
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        progress = { download.chapterItem.downloadProgress.toFloat() / 100 },
                        trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.medium),
                        stroke = stroke,
                        trackStroke = stroke,
                    )

                false -> {
                    val animatedProgress by
                        animateFloatAsState(
                            targetValue = download.chapterItem.downloadProgress.toFloat() / 100,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        )
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        progress = { animatedProgress },
                        trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.medium),
                        stroke = stroke,
                        trackStroke = stroke,
                    )
                }
            }
        }
        IconButton(
            onClick = { dropdown = !dropdown },
            modifier = Modifier.padding(end = Size.medium),
        ) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            SimpleDropdownMenu(
                expanded = dropdown,
                themeColorState = defaultThemeColorState(),
                onDismiss = { dropdown = false },
                dropDownItems =
                    getDropDownItems(
                        first = first,
                        last = last,
                        moveToTopClicked = { moveDownloadClicked(MoveDownloadDirection.Top) },
                        moveSeriesToTopClicked = { moveSeriesClicked(MoveDownloadDirection.Top) },
                        moveToBottomClicked = { moveDownloadClicked(MoveDownloadDirection.Bottom) },
                        moveSeriesToBottomClicked = {
                            moveSeriesClicked(MoveDownloadDirection.Bottom)
                        },
                        cancelSeriesClicked = cancelSeriesClicked,
                    ),
            )
        }
    }
}

@Composable
private fun getDropDownItems(
    first: Boolean,
    last: Boolean,
    moveToTopClicked: () -> Unit,
    moveSeriesToTopClicked: () -> Unit,
    moveToBottomClicked: () -> Unit,
    moveSeriesToBottomClicked: () -> Unit,
    cancelSeriesClicked: () -> Unit,
): PersistentList<SimpleDropDownItem> {
    return listOf(
            SimpleDropDownItem.Parent(
                text = UiText.StringResource(R.string.move_download),
                children =
                    listOf(
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.to_top),
                            onClick = moveToTopClicked,
                            enabled = !first,
                        ),
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.to_bottom),
                            onClick = moveToBottomClicked,
                            enabled = !last,
                        ),
                    ),
            ),
            SimpleDropDownItem.Parent(
                text = UiText.StringResource(R.string.move_series),
                children =
                    listOf(
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.to_top),
                            onClick = moveSeriesToTopClicked,
                        ),
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.to_bottom),
                            onClick = moveSeriesToBottomClicked,
                        ),
                    ),
            ),
            SimpleDropDownItem.Action(
                text = UiText.StringResource(R.string.cancel_all_for_series),
                onClick = cancelSeriesClicked,
            ),
        )
        .toPersistentList()
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
