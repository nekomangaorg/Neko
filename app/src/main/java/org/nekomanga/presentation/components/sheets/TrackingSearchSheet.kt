package org.nekomanga.presentation.components.sheets

import Header
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackSearchItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.components.dialog.TrackingSwitchDialog
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun TrackingSearchSheet(
    themeColorState: ThemeColorState,
    alreadySelectedTrack: TrackItem? = null,
    cancelClick: () -> Unit,
    title: String,
    trackSearchResult: TrackSearchResult,
    service: TrackServiceItem,
    trackingRemoved: (Boolean, TrackServiceItem) -> Unit,
    searchTracker: (String) -> Unit,
    openInBrowser: (String, String) -> Unit,
    trackSearchItemClick: (TrackSearchItem) -> Unit,
) {
    val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .5

    var trackSearchItem by remember { mutableStateOf<TrackSearchItem?>(null) }

    BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Header(stringResource(id = R.string.select_an_entry), cancelClick)

            when (trackSearchResult) {
                is TrackSearchResult.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeightIn(0.dp, maxLazyHeight.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Gap(8.dp)
                        }
                        if (alreadySelectedTrack == null && trackSearchResult.trackSearchResult.size == 1) {
                            trackSearchItemClick(trackSearchResult.trackSearchResult.first())
                        }

                        items(trackSearchResult.trackSearchResult) { item: TrackSearchItem ->
                            TrackSearchItem(
                                themeColorState = themeColorState,
                                trackSearch = item,
                                alreadySelectedTrack = alreadySelectedTrack,
                                openInBrowser = openInBrowser,
                                trackSearchItemClick = {
                                    if (alreadySelectedTrack == null) {
                                        trackSearchItemClick(it)
                                    } else {
                                        trackSearchItem = item
                                    }
                                },
                            )

                            if (trackSearchItem != null) {
                                TrackingSwitchDialog(
                                    themeColorState = themeColorState,
                                    name = stringResource(id = service.nameRes),
                                    oldName = alreadySelectedTrack?.title ?: "",
                                    newName = trackSearchItem!!.trackItem.title,
                                    onConfirm = { alsoRemoveFromTracker ->
                                        trackingRemoved(alsoRemoveFromTracker, service)
                                        trackSearchItemClick(trackSearchItem!!)
                                        cancelClick()
                                    },
                                    onDismiss = { trackSearchItem = null },
                                )
                            }
                        }

                        item {
                            Gap(8.dp)
                        }
                    }
                }
                else -> CenteredBox(themeColorState = themeColorState, trackSearchResult = trackSearchResult)
            }
            var searchText by remember { mutableStateOf(title) }
            SearchFooter(themeColorState = themeColorState, title = searchText, textChanged = { searchText = it }, search = searchTracker)
        }
    }
}

@Composable
private fun CenteredBox(themeColorState: ThemeColorState, trackSearchResult: TrackSearchResult) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (trackSearchResult) {
            is TrackSearchResult.Loading -> CircularProgressIndicator(color = themeColorState.buttonColor, modifier = Modifier.size(32.dp))
            is TrackSearchResult.NoResult -> Text(text = stringResource(id = R.string.no_results_found), color = MaterialTheme.colorScheme.onSurface)
            is TrackSearchResult.Error -> Text(text = trackSearchResult.errorMessage)
            else -> Unit
        }
    }
}

@Composable
private fun TrackSearchItem(
    themeColorState: ThemeColorState,
    trackSearch: TrackSearchItem,
    alreadySelectedTrack: TrackItem?,
    openInBrowser: (String, String) -> Unit,
    trackSearchItemClick: (TrackSearchItem) -> Unit,
) {
    val isSelected = alreadySelectedTrack != null && alreadySelectedTrack.mediaId != 0L && alreadySelectedTrack.mediaId == trackSearch.trackItem.mediaId

    val (backdropColor, outlineColor) = if (isSelected) {
        themeColorState.altContainerColor to themeColorState.buttonColor
    } else {
        MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outline
    }

    OutlinedCard(
        modifier = Modifier.padding(horizontal = 8.dp),
        border = CardDefaults.outlinedCardBorder(true).copy(brush = SolidColor(outlineColor)),
        onClick = { trackSearchItemClick(trackSearch) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(trackSearch.coverUrl).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )

            Box(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
                    .background(color = backdropColor.copy(alpha = NekoColors.highAlphaLowContrast)),
            ) {
                IconButton(
                    onClick = { openInBrowser(trackSearch.trackItem.trackingUrl, trackSearch.trackItem.title) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(8.dp),
                ) {
                    Text(
                        text = trackSearch.trackItem.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .fillMaxWidth(.9f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (trackSearch.publishingType.isNotEmpty()) {
                        Row {
                            Text(
                                text = stringResource(id = R.string.type),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Gap(4.dp)
                            Text(
                                text = trackSearch.publishingType,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    if (trackSearch.startDate.isNotEmpty()) {
                        Row {
                            Text(text = stringResource(id = R.string.started), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Gap(4.dp)
                            Text(
                                text = trackSearch.startDate,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    val summary = when (trackSearch.summary.isBlank()) {
                        true -> stringResource(id = R.string.no_description)
                        false -> trackSearch.summary
                    }

                    Text(
                        text = summary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color = outlineColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}
