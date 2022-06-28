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
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.ReadingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingDate
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
    onSearchTrackClick: (TrackService, Long) -> Unit,
    trackStatusChanged: (Int, TrackAndService) -> Unit,
    trackScoreChanged: (Int, TrackAndService) -> Unit,
    trackChapterChanged: (Int, TrackAndService) -> Unit,
    trackingRemoved: (Boolean, TrackService) -> Unit,
    trackingStartDateClick: (TrackAndService, TrackingDate) -> Unit,
    trackingFinishDateClick: (TrackAndService, TrackingDate) -> Unit,
) {

    var statusDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var scoreDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var removeTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var chapterTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var calendarStartTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var calendarFinishedTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }




    BaseSheet(themeColors = themeColors) {
        if (statusDialog is ShowDialog) {
            val trackAndService = (statusDialog as ShowDialog).trackAndService
            TrackingStatusDialog(
                themeColors = themeColors,
                initialStatus = trackAndService.track.status,
                service = trackAndService.service,
                onDismiss = { statusDialog = HideDialog },
                trackStatusChange = { statusIndex -> trackStatusChanged(statusIndex, trackAndService) },
            )
        } else if (scoreDialog is ShowDialog) {
            val trackAndService = (scoreDialog as ShowDialog).trackAndService
            TrackingScoreDialog(
                themeColors = themeColors,
                trackAndService = trackAndService,
                onDismiss = { scoreDialog = HideDialog },
                trackScoreChange = { scorePosition -> trackScoreChanged(scorePosition, trackAndService) },
            )
        } else if (removeTrackDialog is ShowDialog) {
            val trackAndService = (removeTrackDialog as ShowDialog).trackAndService
            RemoveTrackingDialog(
                themeColors = themeColors,
                name = stringResource(id = trackAndService.service.nameRes()),
                onConfirm = { alsoRemoveFromTracker ->
                    trackingRemoved(alsoRemoveFromTracker, trackAndService.service)
                },
                onDismiss = { removeTrackDialog = HideDialog },
            )
        } else if (chapterTrackDialog is ShowDialog) {
            val trackAndService = (chapterTrackDialog as ShowDialog).trackAndService
            TrackingChapterDialog(
                themeColors = themeColors,
                track = trackAndService.track,
                onDismiss = { chapterTrackDialog = HideDialog },
                trackChapterChanged = {
                    trackChapterChanged(it, trackAndService)
                },
            )
        } else if (calendarStartTrackDialog is ShowDialog) {
            val trackAndService = (calendarStartTrackDialog as ShowDialog).trackAndService
            trackingStartDateClick(
                trackAndService,
                TrackingDate(readingDate = ReadingDate.Start, currentDate = trackAndService.track.started_reading_date, dateFormat = dateFormat),
            )
        } else if (calendarFinishedTrackDialog is ShowDialog) {
            val trackAndService = (calendarFinishedTrackDialog as ShowDialog).trackAndService
            trackingFinishDateClick(trackAndService, TrackingDate(readingDate = ReadingDate.Finish, trackAndService.track.finished_reading_date, dateFormat = dateFormat))
        }



        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(services) { service ->
                val track = tracks.firstOrNull { it.sync_id == service.id }

                val trackAndService = when (track != null) {
                    true -> TrackAndService(track, service)
                    false -> null
                }
                TrackingServiceItem(
                    themeColors = themeColors,
                    service = service,
                    trackAndService = trackAndService,
                    dateFormat = dateFormat,
                    onLogoClick = onLogoClick,
                    onSearchTrackClick = { mediaId -> onSearchTrackClick(service, mediaId) },
                    onRemoveTrackClick = {
                        trackAndService?.run { removeTrackDialog = ShowDialog(trackAndService) }
                    },
                    statusClick = {
                        trackAndService?.run { statusDialog = ShowDialog(trackAndService) }
                    },
                    scoreClick = {
                        trackAndService?.run { scoreDialog = ShowDialog(trackAndService) }
                    },
                    chapterClick = {
                        trackAndService?.run { chapterTrackDialog = ShowDialog(trackAndService) }
                    },
                    startDateClick = {
                        trackAndService?.run { calendarStartTrackDialog = ShowDialog(trackAndService) }
                    },
                    finishDateClick = {
                        trackAndService?.run { calendarFinishedTrackDialog = ShowDialog(trackAndService) }
                    },
                )
            }
        }
    }
}

private sealed class Dialog
private object HideDialog : Dialog()
private class ShowDialog(val trackAndService: TrackAndService) : Dialog()

@Composable
private fun TrackingServiceItem(
    themeColors: ThemeColors,
    service: TrackService,
    trackAndService: TrackAndService?,
    dateFormat: DateFormat,
    onLogoClick: (String) -> Unit,
    onSearchTrackClick: (Long) -> Unit,
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
            if (trackAndService == null) {
                NoTrack(themeColors = themeColors, service = service, onLogoClick, onSearchTrackClick)
            } else {
                TrackRowOne(
                    themeColors = themeColors,
                    track = trackAndService.track,
                    service = trackAndService.service,
                    onLogoClick = onLogoClick,
                    searchTrackerClick = onSearchTrackClick,
                    onRemoveClick = onRemoveTrackClick,
                )
                Divider()
                TrackRowTwo(track = trackAndService.track, service = trackAndService.service, statusClick, scoreClick, chapterClick)
                if (service.supportsReadingDates) {
                    Divider()
                    TrackRowThree(track = trackAndService.track, dateFormat = dateFormat, startDateClick = startDateClick, finishDateClick = finishDateClick)
                }
            }
        }
    }
}

@Composable
private fun NoTrack(themeColors: ThemeColors, service: TrackService, onLogoClick: (String) -> Unit, searchTrackerClick: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable { searchTrackerClick(0L) },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Logo(service = service, track = null, onClick = onLogoClick)
        Text(text = stringResource(id = R.string.add_tracking), color = themeColors.buttonColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrackRowOne(themeColors: ThemeColors, track: Track, service: TrackService, onLogoClick: (String) -> Unit = {}, searchTrackerClick: (Long) -> Unit, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .conditional(!service.isMdList()) {
                clickable { searchTrackerClick(track.media_id) }
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
private fun TrackRowTwo(track: Track, service: TrackService, statusClick: () -> Unit, scoreClick: () -> Unit, chapterClick: () -> Unit) {
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
