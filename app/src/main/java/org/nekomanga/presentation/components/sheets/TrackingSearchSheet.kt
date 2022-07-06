package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.dialog.TrackingSwitchDialog
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun TrackingSearchSheet(
    themeColorState: ThemeColorState,
    alreadySelectedTrack: Track? = null,
    cancelClick: () -> Unit,
    title: String,
    trackSearchResult: TrackSearchResult,
    service: TrackService,
    trackingRemoved: (Boolean, TrackService) -> Unit,
    searchTracker: (String) -> Unit,
    openInBrowser: (String) -> Unit,
    trackSearchItemClick: (TrackSearch) -> Unit,
) {
    val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .6

    var trackSearchItem by remember { mutableStateOf<TrackSearch?>(null) }


    BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {

            Header(cancelClick)

            when (trackSearchResult) {
                is TrackSearchResult.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeightIn(0.dp, maxLazyHeight.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    )
                    {
                        item {
                            Gap(8.dp)
                        }
                        items(trackSearchResult.trackSearchResult) { item: TrackSearch ->
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
                                    name = stringResource(id = service.nameRes()),
                                    oldName = alreadySelectedTrack?.title ?: "",
                                    newName = trackSearchItem!!.title,
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
            Footer(themeColorState, title, searchTracker)
        }
    }
}

@Composable
private fun ColumnScope.Header(cancelClick: () -> Unit) {

    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
        IconButton(onClick = { cancelClick() }) {
            Icon(
                imageVector = Icons.Default.Close, contentDescription = null,
                modifier = Modifier
                    .size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            text = stringResource(id = R.string.select_an_entry),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }

    Gap(4.dp)
    Divider()
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
            is TrackSearchResult.NoResult -> Text(text = stringResource(id = R.string.no_results_found))
            is TrackSearchResult.Error -> Text(text = trackSearchResult.errorMessage)
            else -> {}
        }

    }
}

@Composable
private fun TrackSearchItem(themeColorState: ThemeColorState, trackSearch: TrackSearch, alreadySelectedTrack: Track?, openInBrowser: (String) -> Unit, trackSearchItemClick: (TrackSearch) -> Unit) {

    val isSelected = alreadySelectedTrack != null && alreadySelectedTrack.media_id != 0L && alreadySelectedTrack.media_id == trackSearch.media_id

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
                model = ImageRequest.Builder(LocalContext.current).data(trackSearch.cover_url).build(),
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
                    onClick = { openInBrowser(trackSearch.tracking_url) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser, contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(8.dp),
                ) {
                    Text(
                        text = trackSearch.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .fillMaxWidth(.9f),
                    )

                    if (trackSearch.publishing_type.isNotEmpty()) {
                        Row {
                            Text(text = stringResource(id = R.string.type), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Gap(4.dp)
                            Text(
                                text = trackSearch.publishing_type,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    if (trackSearch.start_date.isNotEmpty()) {
                        Row {
                            Text(text = stringResource(id = R.string.started), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Gap(4.dp)
                            Text(
                                text = trackSearch.start_date,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast),
                                style = MaterialTheme.typography.bodyLarge,
                            )

                        }
                    }

                    Text(
                        text = trackSearch.summary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(color = MaterialTheme.colorScheme.surface),
                    tint = outlineColor,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Footer(themeColorState: ThemeColorState, title: String, searchTracker: (String) -> Unit) {

    val focusManager = LocalFocusManager.current
    var searchText by remember { mutableStateOf(title) }

    Divider()
    Gap(4.dp)

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        value = searchText,
        label = {
            Text(text = stringResource(id = R.string.title))
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { searchText = "" }) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = themeColorState.buttonColor)
                }
            }
        },
        onValueChange = { searchText = it },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedLabelColor = themeColorState.buttonColor,
            focusedBorderColor = themeColorState.buttonColor,
            cursorColor = themeColorState.buttonColor,
        ),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                searchTracker(searchText)
            },
        ),
    )
}
