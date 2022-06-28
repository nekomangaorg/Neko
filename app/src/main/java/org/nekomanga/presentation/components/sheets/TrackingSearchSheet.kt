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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SearchOff
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
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.ui.manga.TrackingConstants.TrackSearchResult
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.IconsEmptyScreen
import org.nekomanga.presentation.screens.ThemeColors

@Composable
fun TrackingSearchSheet(
    themeColors: ThemeColors,
    cancelClick: () -> Unit,
    title: String,
    trackSearchResult: TrackSearchResult,
    searchTracker: (String) -> Unit,
    openInBrowser: (String) -> Unit,
    trackSearchItemClick: (TrackSearch) -> Unit,
) {
    val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .6

    BaseSheet(themeColors = themeColors, maxSheetHeightPercentage = .9f) {

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
                            TrackSearchItem(trackSearch = item, openInBrowser = openInBrowser, trackSearchItemClick = trackSearchItemClick)
                        }
                        item {
                            Gap(8.dp)
                        }
                    }
                }
                else -> CenteredBox(themeColors = themeColors, trackSearchResult = trackSearchResult)
            }
            Footer(themeColors, title, searchTracker)
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
private fun CenteredBox(themeColors: ThemeColors, trackSearchResult: TrackSearchResult) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (trackSearchResult) {
            is TrackSearchResult.Loading -> CircularProgressIndicator(color = themeColors.buttonColor, modifier = Modifier.size(32.dp))
            is TrackSearchResult.NoResult -> Text(text = stringResource(id = R.string.no_results_found))
            is TrackSearchResult.Error -> IconsEmptyScreen(icon = Icons.Default.SearchOff, message = trackSearchResult.errorMessage)
            else -> {}
        }

    }
}

@Composable
private fun TrackSearchItem(trackSearch: TrackSearch, openInBrowser: (String) -> Unit, trackSearchItemClick: (TrackSearch) -> Unit) {
    OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp), onClick = { trackSearchItemClick(trackSearch) }) {

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
                    .background(color = MaterialTheme.colorScheme.surface.copy(alpha = NekoColors.highAlphaLowContrast)),
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
        }
    }
}

@Composable
private fun ColumnScope.Footer(themeColors: ThemeColors, title: String, searchTracker: (String) -> Unit) {

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
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = themeColors.buttonColor)
                }
            }
        },
        onValueChange = { searchText = it },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedLabelColor = themeColors.buttonColor,
            focusedBorderColor = themeColors.buttonColor,
            cursorColor = themeColors.buttonColor,
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
