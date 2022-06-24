package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.Divider
import eu.kanade.presentation.components.VerticalDivider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.dialog.RemoveTrackingDialog
import org.nekomanga.presentation.components.dialog.TrackingChapterDialog
import org.nekomanga.presentation.components.dialog.TrackingScoreDialog
import org.nekomanga.presentation.components.dialog.TrackingStatusDialog
import org.nekomanga.presentation.screens.ThemeColors
import org.nekomanga.presentation.theme.Shapes
import java.text.DateFormat

@Composable
fun TrackingSheet(
    themeColors: ThemeColors,
    services: List<TrackService>,
    tracks: List<Track>,
    dateFormat: DateFormat,
    onLogoClick: (String) -> Unit,
    onSearchTrackClick: (TrackService) -> Unit,
    trackStatusChanged: (Int, Track, TrackService) -> Unit,
    trackScoreChanged: (Int, Track, TrackService) -> Unit,
    trackChapterChanged: (Int, Track, TrackService) -> Unit,
    trackingRemoved: (Boolean, TrackService) -> Unit,
    /*trackingStartDateClick: () -> Unit,
    trackingFinishDateClick: () -> Unit,*/
) {

    var statusDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var scoreDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var removeTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var chapterTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var calendarStartTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var calendarFinishedTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }




    BaseSheet(themeColors = themeColors) {
        if (statusDialog is ShowDialog) {
            val track = (statusDialog as ShowDialog).track
            val service = (statusDialog as ShowDialog).service
            TrackingStatusDialog(
                themeColors = themeColors,
                initialStatus = track.status,
                service = service,
                onDismiss = { statusDialog = HideDialog },
                trackStatusChange = { statusIndex -> trackStatusChanged(statusIndex, track, service) },
            )
        } else if (scoreDialog is ShowDialog) {
            val track = (scoreDialog as ShowDialog).track
            val service = (scoreDialog as ShowDialog).service
            TrackingScoreDialog(
                themeColors = themeColors,
                track = track,
                service = service,
                onDismiss = { scoreDialog = HideDialog },
                trackScoreChange = { scorePosition -> trackScoreChanged(scorePosition, track, service) },
            )
        } else if (removeTrackDialog is ShowDialog) {
            val service = (removeTrackDialog as ShowDialog).service
            RemoveTrackingDialog(
                themeColors = themeColors, name = stringResource(id = service.nameRes()),
                onConfirm = { alsoRemoveFromTracker ->
                    trackingRemoved(alsoRemoveFromTracker, service)
                },
                onDismiss = { removeTrackDialog = HideDialog },
            )
        } else if (chapterTrackDialog is ShowDialog) {
            val service = (chapterTrackDialog as ShowDialog).service
            val track = (chapterTrackDialog as ShowDialog).track
            TrackingChapterDialog(
                themeColors = themeColors, track = track, onDismiss = { chapterTrackDialog = HideDialog },
                trackChapterChanged = {
                    trackChapterChanged(it, track, service)
                },
            )
        } else if (calendarStartTrackDialog is ShowDialog) {
            val service = (calendarStartTrackDialog as ShowDialog).service
            val track = (calendarStartTrackDialog as ShowDialog).track
            TrackingChapterDialog(
                themeColors = themeColors, track = track, onDismiss = { chapterTrackDialog = HideDialog },
                trackChapterChanged = {
                    trackChapterChanged(it, track, service)
                },
            )
        } else if (calendarFinishedTrackDialog is ShowDialog) {
            val service = (chapterTrackDialog as ShowDialog).service
            val track = (chapterTrackDialog as ShowDialog).track
            TrackingChapterDialog(
                themeColors = themeColors, track = track, onDismiss = { chapterTrackDialog = HideDialog },
                trackChapterChanged = {
                    trackChapterChanged(it, track, service)
                },
            )
        }



        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(services) { service ->
                val track = tracks.firstOrNull { it.sync_id == service.id }
                TrackingServiceItem(
                    themeColors = themeColors,
                    service = service,
                    track = track,
                    dateFormat = dateFormat,
                    onLogoClick = onLogoClick,
                    onSearchTrackClick = { onSearchTrackClick(service) },
                    onRemoveTrackClick = {
                        track?.run { removeTrackDialog = ShowDialog(track, service) }
                    },
                    statusClick = {
                        track?.run { statusDialog = ShowDialog(track, service) }
                    },
                    scoreClick = {
                        track?.run { scoreDialog = ShowDialog(track, service) }
                    },
                    chapterClick = {
                        track?.run { chapterTrackDialog = ShowDialog(track, service) }
                    },
                    startDateClick = {
                        track?.run { calendarStartTrackDialog = ShowDialog(track, service) }
                    },
                    finishDateClick = {
                        track?.run { calendarFinishedTrackDialog = ShowDialog(track, service) }
                    },
                )
            }
        }
    }
}

private sealed class Dialog
private object HideDialog : Dialog()
private class ShowDialog(val track: Track, val service: TrackService) : Dialog()

@Composable
private fun TrackingServiceItem(
    themeColors: ThemeColors,
    service: TrackService,
    track: Track?,
    dateFormat: DateFormat,
    onLogoClick: (String) -> Unit,
    onSearchTrackClick: () -> Unit,
    onRemoveTrackClick: () -> Unit,
    statusClick: () -> Unit,
    scoreClick: () -> Unit,
    chapterClick: () -> Unit,
    startDateClick: () -> Unit,
    finishDateClick: () -> Unit,
) {

    OutlinedCard(
        shape = RoundedCornerShape(Shapes.sheetRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (track == null) {
                NoTrack(themeColors = themeColors, service = service, onLogoClick, onSearchTrackClick)
            } else {
                TrackRowOne(themeColors = themeColors, service = service, track = track, onLogoClick = onLogoClick, searchTrackerClick = onSearchTrackClick, onRemoveClick = onRemoveTrackClick)
                Divider()
                TrackRowTwo(service = service, track = track, statusClick, scoreClick, chapterClick)
                if (service.supportsReadingDates) {
                    Divider()
                    TrackRowThree(track = track, dateFormat = dateFormat, startDateClick = startDateClick, finishDateClick = finishDateClick)
                }
            }
        }
    }
}

@Composable
private fun NoTrack(themeColors: ThemeColors, service: TrackService, onLogoClick: (String) -> Unit, searchTrackerClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable { searchTrackerClick() },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Logo(service = service, track = null, onClick = onLogoClick)
        Text(text = stringResource(id = R.string.add_tracking), color = themeColors.buttonColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrackRowOne(themeColors: ThemeColors, service: TrackService, track: Track, onLogoClick: (String) -> Unit = {}, searchTrackerClick: () -> Unit, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .conditional(!service.isMdList()) {
                clickable { searchTrackerClick() }
            },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Logo(service = service, track = track, onClick = onLogoClick)
        if (service.isMdList()) {
            Text(
                text = track.title, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        } else {
            Text(text = track.title, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(4.dp))
            IconButton(onClick = onRemoveClick) {
                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(24.dp), tint = themeColors.buttonColor)
            }
        }
    }
}

@Composable
private fun TrackRowTwo(service: TrackService, track: Track, statusClick: () -> Unit, scoreClick: () -> Unit, chapterClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackingBox(clickable = statusClick) {
            Text(
                service.getStatus(track.status),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        VerticalDivider()

        if (service.isMdList().not()) {
            TrackingBox(clickable = chapterClick) {
                val chapterText = when {
                    track.total_chapters > 0 && track.last_chapter_read.toInt() == track.total_chapters -> stringResource(
                        R.string.all_chapters_read,
                    )
                    track.total_chapters > 0 -> stringResource(
                        R.string.chapter_x_of_y,
                        track.last_chapter_read.toInt(),
                        track.total_chapters,
                    )
                    track.last_chapter_read > 0 -> stringResource(
                        R.string.chapter_,
                        track.last_chapter_read.toInt().toString(),
                    )
                    else -> stringResource(R.string.not_started)
                }

                Text(text = chapterText, style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = (-.3f).sp))

            }
            VerticalDivider()
        }

        TrackingBox(clickable = scoreClick) {
            when (track.score == 0f) {
                true -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(id = R.string.score),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast),
                        )
                    }
                }
                else -> Text(service.displayScore(track))
            }
        }
    }
}

@Composable
fun TrackRowThree(track: Track, dateFormat: DateFormat, startDateClick: () -> Unit = {}, finishDateClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        TrackingBox(clickable = startDateClick) {
            val (startText, startColor) = when (track.started_reading_date != 0L) {
                true -> dateFormat.format(track.started_reading_date) to MaterialTheme.colorScheme.onSurface
                false -> stringResource(id = R.string.started_reading_date) to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
            }
            Text(text = startText, color = startColor, style = MaterialTheme.typography.bodyMedium)
        }

        VerticalDivider()

        TrackingBox(clickable = finishDateClick) {

            val (endText, endColor) = when (track.finished_reading_date != 0L) {
                true -> dateFormat.format(track.finished_reading_date) to MaterialTheme.colorScheme.onSurface
                false -> stringResource(id = R.string.finished_reading_date) to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
            }
            Text(text = endText, color = endColor, style = MaterialTheme.typography.bodyMedium)
        }

    }
}

@Composable
private fun Logo(service: TrackService, track: Track?, onClick: (String) -> Unit = {}) {
    Box(
        modifier = Modifier
            .background(color = Color(service.getLogoColor()))
            .size(56.dp)
            .conditional(track != null) {
                clickable {
                    onClick(track!!.tracking_url)
                }
            },
    ) {
        Image(painter = painterResource(id = service.getLogo()), contentDescription = null, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun RowScope.TrackingBox(clickable: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .clickable { clickable() }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        modifier.invoke(this)
    } else {
        this
    }
}
