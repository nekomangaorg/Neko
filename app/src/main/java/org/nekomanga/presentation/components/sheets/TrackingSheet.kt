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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.Divider
import eu.kanade.presentation.components.VerticalDivider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColors
import org.nekomanga.presentation.theme.Shapes
import java.text.DateFormat

@Composable
fun TrackingSheet(themeColors: ThemeColors, services: List<TrackService>, tracks: List<Track>, dateFormat: DateFormat, onLogoClick: (String) -> Unit, onSearchTrackClick: () -> Unit) {

    BaseSheet(themeColor = themeColors) {
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(services) { service ->
                TrackingServiceItem(
                    themeColors = themeColors,
                    service = service,
                    tracks.firstOrNull { it.sync_id == service.id },
                    dateFormat = dateFormat,
                    onLogoClick = onLogoClick,
                    onSearchTrackClick = onSearchTrackClick,
                )
            }
        }
    }
}

@Composable
private fun TrackingServiceItem(themeColors: ThemeColors, service: TrackService, track: Track?, dateFormat: DateFormat, onLogoClick: (String) -> Unit, onSearchTrackClick: () -> Unit) {

    OutlinedCard(
        shape = RoundedCornerShape(Shapes.sheetRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (track == null) {
                NoTrack(themeColors = themeColors, service = service, onLogoClick, onSearchTrackClick)
            } else {
                TrackRowOne(themeColors = themeColors, service = service, track = track, onLogoClick = onLogoClick, searchTrackerClick = onSearchTrackClick, onRemoveClick = {})
                Divider()
                TrackRowTwo(service = service, track = track)
                if (service.supportsReadingDates) {
                    TrackRowThree(service = service, track = track, dateFormat = dateFormat)
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
            .clickable { searchTrackerClick() },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Logo(service = service, track = track, onClick = onLogoClick)
        Text(text = track.title, color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = {}, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onRemoveClick) {
                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(24.dp), tint = themeColors.buttonColor)
            }
        }
    }
}

@Composable
private fun TrackRowTwo(service: TrackService, track: Track) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(service.getStatus(track.status))
        VerticalDivider()
        if (service.isMdList().not()) {
            Text("test")
            VerticalDivider()
        }

        when (track.score == 0f) {
            true -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.score))
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp))
                }
            }
            else -> Text(service.displayScore(track))
        }
    }
}

@Composable
fun TrackRowThree(service: TrackService, track: Track, dateFormat: DateFormat, startDateClick: () -> Unit = {}, finishDateClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (startText, startColor) = when (track.started_reading_date != 0L) {
            true -> dateFormat.format(track.started_reading_date) to MaterialTheme.colorScheme.onSurface
            false -> stringResource(id = R.string.started_reading_date) to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
        }
        Text(text = startText, color = startColor)

        val (endText, endColor) = when (track.finished_reading_date != 0L) {
            true -> dateFormat.format(track.finished_reading_date) to MaterialTheme.colorScheme.onSurface
            false -> stringResource(id = R.string.finished_reading_date) to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)
        }
        Text(text = endText, color = endColor)

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

fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        modifier.invoke(this)
    } else {
        this
    }
}
