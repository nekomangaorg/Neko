package org.nekomanga.presentation.components.sheets

import Header
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.manga.MergeManga
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Shapes

@Composable
fun MergeSheet(
    themeColorState: ThemeColorState,
    title: String,
    altTitles: List<String>,
    isMergedManga: IsMergedManga,
    mergeSearchResults: MergeSearchResult,
    search: (String) -> Unit,
    openMergeSource: (String, String) -> Unit,
    removeMergeSource: () -> Unit,
    mergeMangaClick: (MergeManga) -> Unit,
    cancelClick: () -> Unit,
) {
    if (isMergedManga is IsMergedManga.Yes) {
        BaseSheet(themeColor = themeColorState) {
            TextButton(onClick = { openMergeSource(isMergedManga.url, isMergedManga.title) }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.open_merged_in_webview), color = themeColorState.buttonColor)
            }
            Gap(8.dp)
            TextButton(onClick = removeMergeSource, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.remove_merged_source), color = themeColorState.buttonColor)
            }
        }
    } else {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .5

        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
            var searchTitle by remember { mutableStateOf(title) }

            Header(stringResource(id = R.string.select_an_entry), cancelClick)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {
                if (mergeSearchResults is MergeSearchResult.Success) {
                    SuccessResults(mergeMangaList = mergeSearchResults.mergeMangaList, mergeMangaClick = mergeMangaClick)
                }
                NonSuccessResultsAndChips(
                    themeColorState = themeColorState,
                    searchResults = mergeSearchResults,
                    title = title,
                    altTitles = altTitles,
                    chipClick = { chipText ->
                        searchTitle = chipText
                        search(chipText)
                    },
                )
            }

            SearchFooter(themeColorState = themeColorState, title = searchTitle, textChanged = { searchTitle = it }, search = search)
        }
    }
}

@Composable
private fun SuccessResults(mergeMangaList: List<MergeManga>, mergeMangaClick: (MergeManga) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp, start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(mergeMangaList) { item ->
            Box(
                modifier = Modifier
                    .aspectRatio(3f / 4f)
                    .fillMaxWidth(.25f)
                    .clip(RoundedCornerShape(Shapes.coverRadius))
                    .clickable { mergeMangaClick(item) },
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(item.thumbnail).crossfade(true).placeholder(Pastel.getColorLight()).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = .95f)),
                            ),
                            shape = RoundedCornerShape(
                                bottomStart = Shapes.coverRadius,
                                bottomEnd = Shapes.coverRadius,
                            ),
                        )
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.padding(4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.NonSuccessResultsAndChips(themeColorState: ThemeColorState, searchResults: MergeSearchResult, title: String, altTitles: List<String>, chipClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomStart),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Gap(16.dp)
        when (searchResults) {
            is MergeSearchResult.Loading -> CircularProgressIndicator(color = themeColorState.buttonColor, modifier = Modifier.size(32.dp))
            is MergeSearchResult.NoResult -> Text(text = stringResource(id = R.string.no_results_found))
            is MergeSearchResult.Error -> Text(text = searchResults.errorMessage)
            else -> Unit
        }
        Gap(16.dp)
        if (altTitles.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                val allTitles = listOf(title) + altTitles
                items(allTitles) { item ->
                    ElevatedSuggestionChip(
                        onClick = { chipClick(item) },
                        label = { Text(text = item, color = MaterialTheme.colorScheme.surface) },
                        colors = SuggestionChipDefaults.elevatedSuggestionChipColors(containerColor = themeColorState.buttonColor),
                    )
                }
            }
        }
    }
}
