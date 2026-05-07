package org.nekomanga.presentation.components.sheets

import Header
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.source.online.MergedServerSource
import eu.kanade.tachiyomi.ui.manga.MergeConstants.IsMergedManga
import eu.kanade.tachiyomi.ui.manga.MergeConstants.MergeSearchResult
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.domain.manga.MergeArtwork
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun MergeSheet(
    themeColorState: ThemeColorState,
    title: String,
    altTitles: List<String>,
    validMergeTypes: List<MergeType>,
    isMergedManga: IsMergedManga,
    mergeSearchResults: MergeSearchResult,
    search: (String, MergeType, List<String>) -> Unit,
    openMergeSource: (String, String) -> Unit,
    removeMergeSource: (MergeMangaImpl) -> Unit,
    mergeMangaClick: (SourceMergeManga) -> Unit,
) {
    var addingMergeSource by remember { mutableStateOf(false) }
    var selectedMergeType: MergeType? by remember { mutableStateOf(null) }

    when {
        addingMergeSource || isMergedManga is IsMergedManga.No -> {
            MergeSelectionSheet(
                themeColorState = themeColorState,
                title = title,
                altTitles = altTitles,
                validMergeTypes = validMergeTypes,
                isMergedManga = isMergedManga,
                mergeSearchResults = mergeSearchResults,
                search = search,
                openMergeSource = openMergeSource,
                mergeMangaClick = mergeMangaClick,
                addingMergeSource = addingMergeSource,
                selectedMergeType = selectedMergeType,
                onMergeTypeSelected = { selectedMergeType = it },
                onBackClick = {
                    if (selectedMergeType != null) {
                        selectedMergeType = null
                    } else {
                        addingMergeSource = false
                    }
                },
            )
        }

        isMergedManga is IsMergedManga.Yes -> {
            BaseSheet(themeColor = themeColorState) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Size.small),
                ) {
                    isMergedManga.mergedMangaList.forEach { mergedManga ->
                        MergedItem(
                            themeColorState = themeColorState,
                            mergedManga = mergedManga,
                            onOpenWebView = {
                                val source =
                                    MergeType.getSource(mergedManga.mergeType, Injekt.get())
                                val url = source.getMangaUrl(mergedManga.url)
                                openMergeSource(url, mergedManga.title)
                            },
                            onRemove = { removeMergeSource(mergedManga) },
                        )
                    }

                    AddMergedSourceItem(
                        themeColorState = themeColorState,
                        onClick = { addingMergeSource = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun MergeSelectionSheet(
    themeColorState: ThemeColorState,
    title: String,
    altTitles: List<String>,
    validMergeTypes: List<MergeType>,
    isMergedManga: IsMergedManga,
    mergeSearchResults: MergeSearchResult,
    search: (String, MergeType, List<String>) -> Unit,
    openMergeSource: (String, String) -> Unit,
    mergeMangaClick: (SourceMergeManga) -> Unit,
    addingMergeSource: Boolean,
    selectedMergeType: MergeType?,
    onMergeTypeSelected: (MergeType?) -> Unit,
    onBackClick: () -> Unit,
) {
    BaseSheet(themeColor = themeColorState) {
        when (selectedMergeType == null) {
            true -> {
                val availableMergeTypes =
                    if (isMergedManga is IsMergedManga.Yes) {
                        val mergedTypeIds =
                            isMergedManga.mergedMangaList.map { it.mergeType.id }.toSet()
                        validMergeTypes.filter { mergeType ->
                            mergeType.multiMerge || !mergedTypeIds.contains(mergeType.id)
                        }
                    } else {
                        validMergeTypes
                    }

                if (addingMergeSource) {
                    Header(stringResource(id = R.string.add_merge_source), onBackClick)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Size.medium),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    availableMergeTypes.forEach { validMergeType ->
                        MergeLogo(
                            id = validMergeType.toDrawableRes(),
                            onClick = { onMergeTypeSelected(validMergeType) },
                            onLongClick = {
                                if (validMergeType.baseUrl.isNotEmpty()) {
                                    openMergeSource(
                                        validMergeType.baseUrl,
                                        validMergeType.scanlatorName,
                                    )
                                } else {
                                    val source = MergeType.getSource(validMergeType, Injekt.get())
                                    val hostUrl =
                                        if (source is MergedServerSource) source.hostUrl() else ""
                                    if (hostUrl.isNotEmpty())
                                        openMergeSource(hostUrl, validMergeType.scanlatorName)
                                }
                            },
                            title = validMergeType.name,
                        )
                    }
                }
            }

            false -> {
                val existingUrls =
                    if (isMergedManga is IsMergedManga.Yes) {
                        isMergedManga.mergedMangaList
                            .filter { it.mergeType.id == selectedMergeType.id }
                            .map { it.url }
                    } else {
                        emptyList()
                    }

                LaunchedEffect(key1 = selectedMergeType) {
                    search(title, selectedMergeType, existingUrls)
                }
                val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .5

                var searchTitle by remember { mutableStateOf(title) }

                Header(stringResource(id = R.string.select_an_entry), { onMergeTypeSelected(null) })
                Box(
                    modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp)
                ) {
                    if (mergeSearchResults is MergeSearchResult.Success) {
                        SuccessResults(
                            mergeMangaList = mergeSearchResults.mergeMangaList,
                            mergeType = selectedMergeType,
                            mergeMangaClick = mergeMangaClick,
                            mergeMangaLongClick = { item ->
                                val source = MergeType.getSource(selectedMergeType, Injekt.get())
                                val url = source.getMangaUrl(item.url)
                                openMergeSource(url, item.title)
                            },
                        )
                    }
                    NonSuccessResultsAndChips(
                        themeColorState = themeColorState,
                        searchResults = mergeSearchResults,
                        title = title,
                        altTitles = altTitles,
                        chipClick = { chipText ->
                            searchTitle = chipText
                            search(chipText, selectedMergeType, existingUrls)
                        },
                    )
                }

                SearchFooter(
                    themeColorState = themeColorState,
                    labelText = stringResource(id = R.string.title),
                    title = searchTitle,
                    textChanged = { searchTitle = it },
                    search = { search(it, selectedMergeType, existingUrls) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MergeLogo(
    @DrawableRes id: Int,
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    small: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(Size.small)
                    .clip(RoundedCornerShape(Shapes.coverRadius))
                    .conditional(small) {
                        padding(Size.none)
                        fillMaxHeight()
                    }
        ) {
            Image(
                painter = painterResource(id = id),
                contentDescription = title,
                modifier = Modifier.conditional(!small) { size(86.dp) },
            )
        }
        if (!small) Text(text = title, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MergedItem(
    themeColorState: ThemeColorState,
    mergedManga: MergeMangaImpl,
    onOpenWebView: () -> Unit,
    onRemove: () -> Unit,
) {
    val mergeType = mergedManga.mergeType
    val logoRes = mergeType.toDrawableRes()

    OutlinedCard(
        shape = RoundedCornerShape(Shapes.sheetRadius),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = NekoColors.disabledAlphaLowContrast
                ),
            ),
        modifier = Modifier.padding(horizontal = Size.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(Size.extraExtraHuge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Size.small),
        ) {
            MergeLogo(id = logoRes, title = mergeType.name, onClick = onOpenWebView, small = true)
            Text(
                text = mergedManga.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.9f).align(Alignment.CenterVertically),
            )
            IconButton(onClick = onRemove, modifier = Modifier.padding(end = Size.tiny)) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = stringResource(id = R.string.remove),
                    modifier = Modifier.size(28.dp),
                    tint = themeColorState.primaryColor,
                )
            }
        }
    }
}

@Composable
fun AddMergedSourceItem(themeColorState: ThemeColorState, onClick: () -> Unit) {
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().height(Size.huge).clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.add_merge_source),
                color = themeColorState.primaryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SuccessResults(
    mergeMangaList: List<SourceMergeManga>,
    mergeType: MergeType,
    mergeMangaClick: (SourceMergeManga) -> Unit,
    mergeMangaLongClick: (SourceMergeManga) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                top = Size.medium,
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
                        .combinedClickable(
                            onClick = { mergeMangaClick(item) },
                            onLongClick = { mergeMangaLongClick(item) },
                        )
            ) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(MergeArtwork(url = item.coverUrl, mergeType = mergeType))
                            .crossfade(true)
                            .build(),
                    placeholder = ColorPainter(colorResource(Pastel.getColorLight())),
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
        Gap(Size.medium)
        when (searchResults) {
            is MergeSearchResult.Loading ->
                CircularProgressIndicator(
                    color = themeColorState.primaryColor,
                    modifier = Modifier.size(32.dp),
                )

            is MergeSearchResult.NoResult ->
                Text(text = stringResource(id = R.string.no_results_found))

            is MergeSearchResult.Error -> Text(text = searchResults.errorMessage)
            else -> Unit
        }
        Gap(Size.medium)
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
                                    containerColor = themeColorState.primaryColor
                                ),
                        )
                    }
                }
            }
        }
    }
}

fun MergeType.toDrawableRes(): Int =
    when (this) {
        MergeType.Komga -> R.drawable.ic_komga_logo
        MergeType.Suwayomi -> R.drawable.ic_suwayomi_logo
        MergeType.Toonily -> R.drawable.ic_toonily
        MergeType.WeebCentral -> R.drawable.ic_weebcentral_logo
        MergeType.MangaBall -> R.drawable.ic_mangaball_logo
        MergeType.ProjectSuki -> R.drawable.ic_projectsuki_logo
        MergeType.Comix -> R.drawable.ic_comix_logo
        MergeType.Atsumaru -> R.drawable.ic_atsumaru_logo
        MergeType.Invalid -> R.drawable.ic_neko_yokai
    }
