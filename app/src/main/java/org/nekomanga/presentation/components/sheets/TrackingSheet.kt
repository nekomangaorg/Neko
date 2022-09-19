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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.Divider
import eu.kanade.presentation.components.VerticalDivider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.ReadingDate
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackAndService
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackingDate
import java.text.DateFormat
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.dialog.RemoveTrackingDialog
import org.nekomanga.presentation.components.dialog.TrackingChapterDialog
import org.nekomanga.presentation.components.dialog.TrackingScoreDialog
import org.nekomanga.presentation.components.dialog.TrackingStatusDialog
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Shapes

@Composable
fun TrackingSheet(
    themeColor: ThemeColorState,
    inLibrary: Boolean,
    servicesProvider: () -> ImmutableList<TrackServiceItem>,
    tracksProvider: () -> ImmutableList<TrackItem>,
    dateFormat: DateFormat,
    onLogoClick: (String, String) -> Unit,
    onSearchTrackClick: (TrackServiceItem, TrackItem?) -> Unit,
    trackStatusChanged: (Int, TrackAndService) -> Unit,
    trackScoreChanged: (Int, TrackAndService) -> Unit,
    trackChapterChanged: (Int, TrackAndService) -> Unit,
    trackingRemoved: (Boolean, TrackServiceItem) -> Unit,
    trackingStartDateClick: (TrackAndService, TrackingDate) -> Unit,
    trackingFinishDateClick: (TrackAndService, TrackingDate) -> Unit,
) {
    var statusDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var scoreDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var removeTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var chapterTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var calendarStartTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }
    var calendarFinishedTrackDialog by remember { mutableStateOf<Dialog>(HideDialog) }

    BaseSheet(themeColor = themeColor) {
        if (statusDialog is ShowDialog) {
            val trackAndService = (statusDialog as ShowDialog).trackAndService
            TrackingStatusDialog(
                themeColorState = themeColor,
                initialStatus = trackAndService.track.status,
                service = trackAndService.service,
                onDismiss = { statusDialog = HideDialog },
                trackStatusChange = { statusIndex -> trackStatusChanged(statusIndex, trackAndService) },
            )
        } else if (scoreDialog is ShowDialog) {
            val trackAndService = (scoreDialog as ShowDialog).trackAndService
            TrackingScoreDialog(
                themeColorState = themeColor,
                trackAndService = trackAndService,
                onDismiss = { scoreDialog = HideDialog },
                trackScoreChange = { scorePosition -> trackScoreChanged(scorePosition, trackAndService) },
            )
        } else if (removeTrackDialog is ShowDialog) {
            val trackAndService = (removeTrackDialog as ShowDialog).trackAndService
            RemoveTrackingDialog(
                themeColorState = themeColor,
                name = stringResource(id = trackAndService.service.nameRes),
                canRemoveFromTracker = trackAndService.service.canRemoveFromService,
                onConfirm = { alsoRemoveFromTracker ->
                    trackingRemoved(alsoRemoveFromTracker, trackAndService.service)
                },
                onDismiss = { removeTrackDialog = HideDialog },
            )
        } else if (chapterTrackDialog is ShowDialog) {
            val trackAndService = (chapterTrackDialog as ShowDialog).trackAndService
            TrackingChapterDialog(
                themeColorState = themeColor,
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
                TrackingDate(readingDate = ReadingDate.Start, currentDate = trackAndService.track.startedReadingDate, dateFormat = dateFormat),
            )
        } else if (calendarFinishedTrackDialog is ShowDialog) {
            val trackAndService = (calendarFinishedTrackDialog as ShowDialog).trackAndService
            trackingFinishDateClick(trackAndService, TrackingDate(readingDate = ReadingDate.Finish, trackAndService.track.finishedReadingDate, dateFormat = dateFormat))
        }

        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(servicesProvider()) { service ->
                val track = tracksProvider().firstOrNull { it.trackServiceId == service.id }

                val trackAndService = when (track != null) {
                    true -> TrackAndService(track, service)
                    false -> null
                }
                TrackingServiceItem(
                    themeColor = themeColor,
                    inLibrary = inLibrary,
                    service = service,
                    trackAndService = trackAndService,
                    dateFormat = dateFormat,
                    onLogoClick = onLogoClick,
                    onSearchTrackClick = { clickTracked -> onSearchTrackClick(service, clickTracked) },
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
    themeColor: ThemeColorState,
    inLibrary: Boolean,
    service: TrackServiceItem,
    trackAndService: TrackAndService?,
    dateFormat: DateFormat,
    onLogoClick: (String, String) -> Unit,
    onSearchTrackClick: (TrackItem?) -> Unit,
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
                NoTrack(themeColor = themeColor, service = service, onLogoClick) { onSearchTrackClick(null) }
            } else {
                TrackRowOne(
                    themeColor = themeColor,
                    inLibrary = inLibrary,
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
private fun NoTrack(themeColor: ThemeColorState, service: TrackServiceItem, onLogoClick: (String, String) -> Unit, searchTrackerClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .height(48.dp)
            .clickable { searchTrackerClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Logo(service = service, track = null, onClick = onLogoClick)
        Text(text = stringResource(id = R.string.add_tracking), color = themeColor.buttonColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrackRowOne(
    themeColor: ThemeColorState,
    inLibrary: Boolean,
    track: TrackItem,
    service: TrackServiceItem,
    onLogoClick: (String, String) -> Unit,
    searchTrackerClick: (TrackItem) -> Unit,
    onRemoveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .conditional(!service.isMdList) {
                clickable { searchTrackerClick(track) }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Logo(service = service, track = track, onClick = onLogoClick)
        if (service.isAutoAddTracker && inLibrary) {
            Text(
                text = track.title,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = track.title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp),
                    tint = themeColor.buttonColor,
                )
            }
        }
    }
}

@Composable
private fun TrackRowTwo(track: TrackItem, service: TrackServiceItem, statusClick: () -> Unit, scoreClick: () -> Unit, chapterClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackingBox(clickable = statusClick) {
            Text(
                service.status(track.status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        VerticalDivider()

        if (!service.isMdList) {
            TrackingBox(clickable = chapterClick) {
                val chapterText = when {
                    track.totalChapters > 0 && track.lastChapterRead.toInt() == track.totalChapters -> stringResource(
                        R.string.all_chapters_read,
                    )
                    track.totalChapters > 0 -> stringResource(
                        R.string.chapter_x_of_y,
                        track.lastChapterRead.toInt(),
                        track.totalChapters,
                    )
                    track.lastChapterRead > 0 -> stringResource(
                        R.string.chapter_,
                        track.lastChapterRead.toInt().toString(),
                    )
                    else -> stringResource(R.string.not_started)
                }

                Text(text = chapterText, style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = (-.3f).sp), color = MaterialTheme.colorScheme.onSurface)
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
                else -> Text(service.displayScore(track), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun TrackRowThree(track: TrackItem, dateFormat: DateFormat, startDateClick: () -> Unit = {}, finishDateClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackingBox(clickable = startDateClick) {
            val (startText, startColor) = when (track.startedReadingDate != 0L) {
                true -> dateFormat.format(track.startedReadingDate) to MaterialTheme.colorScheme.onSurface
                false -> stringResource(id = R.string.started_reading_date) to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
            }
            Text(text = startText, color = startColor, style = MaterialTheme.typography.bodyMedium)
        }

        VerticalDivider()

        TrackingBox(clickable = finishDateClick) {
            val (endText, endColor) = when (track.finishedReadingDate != 0L) {
                true -> dateFormat.format(track.finishedReadingDate) to MaterialTheme.colorScheme.onSurface
                false -> stringResource(id = R.string.finished_reading_date) to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
            }
            Text(text = endText, color = endColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Logo(service: TrackServiceItem, track: TrackItem?, onClick: (String, String) -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .padding(start = 1.dp, top = 1.dp)
            .clip(RoundedCornerShape(topStart = 15.dp))
            .background(color = Color(service.logoColor))
            .conditional(track != null) {
                clickable {
                    onClick(track!!.trackingUrl, track.title)
                }
            },
    ) {
        Image(painter = painterResource(id = service.logoRes), contentDescription = null, modifier = Modifier.align(Alignment.Center))
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
