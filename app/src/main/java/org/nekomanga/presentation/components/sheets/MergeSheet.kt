package org.nekomanga.presentation.components.sheets

import Header
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.domain.manga.MergeArtwork
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun MergeSheet(
    themeColorState: ThemeColorState,
    title: String,
    altTitles: List<String>,
    validMergeTypes: List<MergeType>,
    isMergedManga: IsMergedManga,
    mergeSearchResults: MergeSearchResult,
    search: (String, MergeType) -> Unit,
    openMergeSource: (String, String) -> Unit,
    removeMergeSource: (MergeType) -> Unit,
    mergeMangaClick: (SourceMergeManga) -> Unit,
    cancelClick: () -> Unit,
) {
    when (isMergedManga) {
        is IsMergedManga.Yes -> {
            BaseSheet(themeColor = themeColorState) {
                val text = MergeType.getMergeTypeName(isMergedManga.mergeType)

                Gap(Size.small)
                Text(
                    text = stringResource(id = R.string.merge_source_, text),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Gap(Size.tiny)

                TextButton(
                    onClick = { openMergeSource(isMergedManga.url, isMergedManga.title) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.open_merged_in_webview),
                        color = themeColorState.buttonColor,
                    )
                }
                Gap(Size.tiny)
                TextButton(
                    onClick = { removeMergeSource(isMergedManga.mergeType) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.remove_merged_source),
                        color = themeColorState.buttonColor,
                    )
                }
                Gap(Size.tiny)
            }
        }
        is IsMergedManga.No -> {
            var mergeType: MergeType? by remember { mutableStateOf(null) }
            BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
                when (mergeType == null) {
                    true -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            validMergeTypes.forEach { validMergeType ->
                                val id =
                                    when (validMergeType) {
                                        MergeType.Komga -> R.drawable.ic_komga_logo
                                        MergeType.MangaLife -> R.drawable.ic_mangalife_logo
                                        MergeType.Suwayomi -> R.drawable.ic_suwayomi_logo
                                        MergeType.Toonily -> R.drawable.ic_toonily
                                        MergeType.WeebCentral -> R.drawable.ic_weebcentral_logo
                                        MergeType.Comick -> R.drawable.ic_comick_logo
                                    }
                                MergeLogo(id = id, onClick = { mergeType = validMergeType })
                            }
                        }
                    }
                    false -> {
                        LaunchedEffect(key1 = mergeType) { search(title, mergeType!!) }
                        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .5

                        var searchTitle by remember { mutableStateOf(title) }

                        Header(stringResource(id = R.string.select_an_entry), cancelClick)
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .requiredHeightIn(Size.none, maxLazyHeight.dp)
                        ) {
                            if (mergeSearchResults is MergeSearchResult.Success) {
                                SuccessResults(
                                    mergeMangaList = mergeSearchResults.mergeMangaList,
                                    mergeType = mergeType!!,
                                    mergeMangaClick = mergeMangaClick,
                                )
                            }
                            NonSuccessResultsAndChips(
                                themeColorState = themeColorState,
                                searchResults = mergeSearchResults,
                                title = title,
                                altTitles = altTitles,
                                chipClick = { chipText ->
                                    searchTitle = chipText
                                    search(chipText, mergeType!!)
                                },
                            )
                        }

                        SearchFooter(
                            themeColorState = themeColorState,
                            labelText = stringResource(id = R.string.title),
                            title = searchTitle,
                            textChanged = { searchTitle = it },
                            search = { search(it, mergeType!!) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeLogo(@DrawableRes id: Int, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(Shapes.coverRadius))
                .clickable(onClick = onClick)
                .padding(Size.small)
                .clip(RoundedCornerShape(Shapes.coverRadius))
    ) {
        Image(
            painter = painterResource(id = id),
            contentDescription = null,
            modifier = Modifier.size(86.dp),
        )
    }
}

@Composable
private fun SuccessResults(
    mergeMangaList: List<SourceMergeManga>,
    mergeType: MergeType,
    mergeMangaClick: (SourceMergeManga) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                top = 16.dp,
                bottom = Size.huge * 2,
                start = Size.small,
                end = Size.small,
            ),
        verticalArrangement = Arrangement.spacedBy(Size.small),
        horizontalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        items(mergeMangaList, key = { item -> item.hashCode() }) { item ->
            Box(
                modifier =
                    Modifier.aspectRatio(3f / 4f)
                        .fillMaxWidth(.25f)
                        .clip(RoundedCornerShape(Shapes.coverRadius))
                        .clickable { mergeMangaClick(item) }
            ) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(MergeArtwork(url = item.coverUrl, mergeType = mergeType))
                            .crossfade(true)
                            .placeholder(Pastel.getColorLight())
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    Modifier.fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = .95f))
                            ),
                            shape =
                                RoundedCornerShape(
                                    bottomStart = Shapes.coverRadius,
                                    bottomEnd = Shapes.coverRadius,
                                ),
                        )
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.padding(Size.tiny),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        style =
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.NonSuccessResultsAndChips(
    themeColorState: ThemeColorState,
    searchResults: MergeSearchResult,
    title: String,
    altTitles: List<String>,
    chipClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Gap(16.dp)
        when (searchResults) {
            is MergeSearchResult.Loading ->
                CircularProgressIndicator(
                    color = themeColorState.buttonColor,
                    modifier = Modifier.size(32.dp),
                )
            is MergeSearchResult.NoResult ->
                Text(text = stringResource(id = R.string.no_results_found))
            is MergeSearchResult.Error -> Text(text = searchResults.errorMessage)
            else -> Unit
        }
        Gap(16.dp)
        if (altTitles.isNotEmpty()) {
            val allTitles = listOf(title) + altTitles.sorted()
            val partitioned =
                allTitles.partition { title -> allTitles.indexOf(title).mod(2) != 0 }.toList()

            partitioned.forEach { chunk ->
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Size.tiny),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    items(chunk) { item ->
                        ElevatedSuggestionChip(
                            onClick = { chipClick(item) },
                            label = {
                                Text(text = item, color = MaterialTheme.colorScheme.surface)
                            },
                            colors =
                                SuggestionChipDefaults.elevatedSuggestionChipColors(
                                    containerColor = themeColorState.buttonColor
                                ),
                        )
                    }
                }
            }
        }
    }
}
