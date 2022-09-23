package org.nekomanga.presentation.screens.stats

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.himanshoe.charty.common.axis.AxisConfig
import com.himanshoe.charty.common.dimens.ChartDimens
import com.himanshoe.charty.line.LineChart
import com.himanshoe.charty.line.config.LineConfig
import com.himanshoe.charty.line.model.LineData
import com.himanshoe.charty.pie.PieChart
import com.himanshoe.charty.pie.config.PieConfig
import com.himanshoe.charty.pie.config.PieData
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.DetailedState
import eu.kanade.tachiyomi.ui.more.stats.StatsHelper.getReadDuration
import eu.kanade.tachiyomi.util.lang.capitalizeWords
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
import jp.wasabeef.gap.Gap
import kotlin.random.Random
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.extensions.surfaceColorAtElevation

@Composable
fun DetailedStats(detailedStats: DetailedState, colors: ImmutableList<Color>, contentPadding: PaddingValues, windowSizeClass: WindowSizeClass) {
    var filterState by rememberSaveable { mutableStateOf(Filter.None) }

    var sortType by remember { mutableStateOf(Sort.Entries) }

    val viewType = when {
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> ViewType.Split
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium && windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> ViewType.Compact
        else -> ViewType.Normal
    }

    val sortChipClick = {
        sortType = when (sortType) {
            Sort.Entries -> Sort.Chapters
            Sort.Chapters -> Sort.Duration
            Sort.Duration -> Sort.Entries
        }
    }

    val filterStateClick = { buttonFilterState: Filter ->
        filterState = when (filterState == buttonFilterState) {
            true -> Filter.None
            false -> buttonFilterState
        }
        sortType = Sort.Entries
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        FilterChipHeader(filterState, filterStateClick)

        val context = LocalContext.current

        when (filterState) {
            Filter.None -> {
                DetailedCardView(detailedStats.manga, contentPadding = contentPadding, viewType)
            }
            Filter.Type -> {
                TypeView(sortType, detailedStats, context, colors, contentPadding, viewType, sortChipClick)
            }

            Filter.Status -> {
                StatusView(sortType, detailedStats, context, colors, contentPadding, viewType, sortChipClick)
            }

            Filter.ContentRating -> {
                ContentRatingView(sortType, detailedStats, colors, contentPadding, viewType, sortChipClick)
            }

            Filter.Category -> {
                CategoryView(sortType, detailedStats, colors, contentPadding, viewType, sortChipClick)
            }

            Filter.Tag -> {
                TagView(sortType, detailedStats, colors, contentPadding, viewType, sortChipClick)
            }

            Filter.StartYear -> {
                StartYearView(detailedStats, colors, contentPadding, viewType)
            }
        }
    }
}

@Composable
private fun FilterChipHeader(filterState: Filter, filterStateClick: (Filter) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Gap(8.dp)
        }
        CustomChip(
            isSelected = filterState == Filter.Type,
            onClick = { filterStateClick(Filter.Type) },
            label = R.string.series_type,
        )
        CustomChip(
            isSelected = filterState == Filter.Status,
            onClick = { filterStateClick(Filter.Status) },
            label = R.string.status,
        )
        CustomChip(
            isSelected = filterState == Filter.ContentRating,
            onClick = { filterStateClick(Filter.ContentRating) },
            label = R.string.content_rating_distribution,
        )
        CustomChip(
            isSelected = filterState == Filter.Category,
            onClick = { filterStateClick(Filter.Category) },
            label = R.string.categories,
        )
        CustomChip(
            isSelected = filterState == Filter.Tag,
            onClick = { filterStateClick(Filter.Tag) },
            label = R.string.tag,
        )
        /*  CustomChip(
              isSelected = filterState == Filter.StartYear,
              onClick = { filterStateClick(Filter.StartYear) },
              label = R.string.start_year,
          )*/
    }
}

@Composable
private fun TagView(
    sortType: Sort,
    detailedStats: DetailedState,
    colors: ImmutableList<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val tagStats = detailedStats.detailTagState
    val sortedTagPairs = remember(sortType) {
        tagStats.sortedTagPairs.map { it }

        tagStats.sortedTagPairs.sortedWith { t, t2 ->
            when (sortType) {
                Sort.Entries -> t2.second.size.compareTo(t.second.size)
                Sort.Chapters -> t2.second.sumOf { it.readChapters }.compareTo(t.second.sumOf { it.readChapters })
                Sort.Duration -> t2.second.sumOf { it.readDuration }.compareTo(t.second.sumOf { it.readDuration })
            }
        }.toPersistentList()
    }
    StatCardView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortedSeries = sortedTagPairs,
        sortType = sortType,
        sortChipClick = sortChipClick,
        showSortChip = true,
        color = colors[0],
        totalCount = detailedStats.detailTagState.totalChapters,
        totalReadDuration = detailedStats.detailTagState.totalReadDuration,
    )
}

@Composable
private fun ContentRatingView(
    sortType: Sort,
    detailedStats: DetailedState,
    colors: ImmutableList<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedSeries = remember(sortType) {
        detailedStats.manga.groupBy { it.contentRating.prettyPrint() }.entries.sortedWith(mapEntryComparator(sortType)).toPersistentList()
    }
    val colorMap = remember { colorMap(sortedSeries.map { it.key }, colors) }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }
    val pieData = remember(sortType) { pieData(sortedSeries, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedSeries = sortedSeries,
        colorMap = colorMap,
        totalCount = totalCount,
        totalDuration = totalDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun CategoryView(
    sortType: Sort,
    detailedStats: DetailedState,
    colors: ImmutableList<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val defaultCategoryName = stringResource(id = R.string.default_value)

    val sortedSeries = remember(sortType) {
        detailedStats.categories.associateWith { category ->
            detailedStats.manga.filter { it.categories.contains(category) }
        }.entries.filter { it.key != defaultCategoryName || it.value.isNotEmpty() }.sortedWith(mapEntryComparator(sortType)).toPersistentList()
    }
    val colorsToUse = remember {
        when (sortedSeries.size <= colors.size) {
            true -> colors
            false -> sortedSeries.map { Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)) }.toImmutableList()
        }
    }

    val colorMap = remember { colorMap(sortedSeries.map { it.key }, colorsToUse) }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }
    val pieData = remember(sortType) { pieData(sortedSeries, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedSeries = sortedSeries,
        colorMap = colorMap,
        totalCount = totalCount,
        totalDuration = totalDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun StartYearView(
    detailedStats: DetailedState,
    colors: ImmutableList<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
) {
    val notStartedString = stringResource(id = R.string.not_started)
    val sortedSeries = remember {
        detailedStats.manga.groupBy { it.startYear?.toString() ?: notStartedString }.entries.sortedBy { it.key }.toPersistentList()
    }
    val lineData = remember {
        sortedSeries.filter { it.key != notStartedString }.map { LineData(xValue = it.key, yValue = it.value.size.toFloat()) }.toPersistentList()
    }
    val colorMap = remember { sortedSeries.associate { it.key to colors[0] }.toImmutableMap() }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        colorMap = colorMap,
        sortedSeries = sortedSeries,
        showSortChip = false,
    ) { modifier, chartWidth ->
        Line(lineData = lineData, chartWidth = chartWidth, modifier = modifier, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun StatusView(
    sortType: Sort,
    detailedStats: DetailedState,
    context: Context,
    colors: ImmutableList<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedSeries = remember(sortType) {
        detailedStats.manga.groupBy { context.getString(it.status.statusRes) }.entries.sortedWith(mapEntryComparator(sortType)).toPersistentList()
    }
    val colorMap = remember { colorMap(sortedSeries.map { it.key }, colors) }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }
    val pieData = remember(sortType) { pieData(sortedSeries, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedSeries = sortedSeries,
        colorMap = colorMap,
        totalCount = totalCount,
        totalDuration = totalDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun TypeView(
    sortType: Sort,
    detailedStats: DetailedState,
    context: Context,
    colors: ImmutableList<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedSeries = remember(sortType) {
        detailedStats.manga.groupBy { context.getString(it.type.typeRes) }.entries.sortedWith(mapEntryComparator(sortType)).toPersistentList()
    }
    val colorMap = remember { colorMap(sortedSeries.map { it.key }, colors) }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }
    val pieData = remember(sortType) { pieData(sortedSeries, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedSeries = sortedSeries,
        colorMap = colorMap,
        totalCount = totalCount,
        totalDuration = totalDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun DefaultView(
    contentPadding: PaddingValues,
    sortType: Sort = Sort.Entries,
    sortChipClick: () -> Unit = {},
    sortedSeries: ImmutableList<Map.Entry<String, List<StatsConstants.DetailedStatManga>>>,
    colorMap: ImmutableMap<String, Color>,
    totalCount: Int = 0,
    totalDuration: Long = 0L,
    viewType: ViewType,
    showSortChip: Boolean = true,
    graph: @Composable (modifier: Modifier, chartWidth: Float) -> Unit,
) {
    val chartWidth = when (viewType) {
        ViewType.Split -> .5f
        ViewType.Normal -> .9f
        ViewType.Compact -> .6f
    }

    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
        if (showSortChip) {
            item {
                SortChip(sortType = sortType, onClick = sortChipClick)
            }
        }

        if (viewType == ViewType.Split) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    graph(chartWidth = chartWidth, modifier = Modifier)
                    Column(
                        Modifier
                            .fillMaxWidth(.9f)
                            .padding(16.dp),
                    ) {
                        sortedSeries.forEach { entry ->
                            StatCard(
                                header = entry.key,
                                headerColor = colorMap[entry.key]!!,
                                count = entry.value.size,
                                totalCount = totalCount,
                                readChapters = entry.value.sumOf { stat -> stat.readChapters },
                                totalChapters = entry.value.sumOf { stat -> stat.totalChapters },
                                readDuration = entry.value.sumOf { stat -> stat.readDuration },
                                totalReadDuration = totalDuration,
                            )
                        }
                    }
                }
            }
        } else {
            item {
                graph(chartWidth = chartWidth, modifier = Modifier.fillMaxWidth())
            }
            item {
                Gap(padding = 8.dp)
            }
            items(sortedSeries, key = { it.key }) { entry ->
                StatCard(
                    header = entry.key,
                    headerColor = colorMap[entry.key]!!,
                    count = entry.value.size,
                    totalCount = totalCount,
                    readChapters = entry.value.sumOf { stat -> stat.readChapters },
                    totalChapters = entry.value.sumOf { stat -> stat.totalChapters },
                    readDuration = entry.value.sumOf { stat -> stat.readDuration },
                    totalReadDuration = totalDuration,
                )
            }
        }
    }
}

@Composable
private fun DetailedCardView(mangaList: ImmutableList<StatsConstants.DetailedStatManga>, contentPadding: PaddingValues, viewType: ViewType) {
    if (viewType == ViewType.Split) {
        LazyGridWrapper(contentPadding = contentPadding) {
            items(mangaList, key = { it.id }) {
                DetailedCard(manga = it, modifier = Modifier.fillMaxWidth(.3f))
            }
        }
    } else {
        LazyListWrapper(contentPadding = contentPadding) {
            items(mangaList, key = { it.id }) {
                DetailedCard(manga = it, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StatCardView(
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortedSeries: ImmutableList<Pair<String, ImmutableList<StatsConstants.DetailedStatManga>>>,
    color: Color,
    totalCount: Int,
    totalReadDuration: Long,
    showSortChip: Boolean = false,
    sortType: Sort = Sort.Entries,
    sortChipClick: () -> Unit = {},
) {
    if (viewType == ViewType.Split) {
        LazyGridWrapper(contentPadding = contentPadding, showSortChip = showSortChip, sortType = sortType, sortChipClick = sortChipClick) {
            items(sortedSeries, key = { it.first }) { entry ->
                StatCard(
                    header = entry.first.capitalizeWords(),
                    headerColor = color,
                    count = entry.second.size,
                    totalCount = totalCount,
                    readChapters = entry.second.sumOf { stat -> stat.readChapters },
                    totalChapters = entry.second.sumOf { stat -> stat.totalChapters },
                    readDuration = entry.second.sumOf { stat -> stat.readDuration },
                    totalReadDuration = totalReadDuration,
                )
            }
        }
    } else {
        LazyListWrapper(contentPadding = contentPadding, showSortChip = showSortChip, sortType = sortType, sortChipClick = sortChipClick) {
            items(sortedSeries, key = { it.first }) { entry ->
                StatCard(
                    header = entry.first.capitalizeWords(),
                    headerColor = color,
                    count = entry.second.size,
                    totalCount = totalCount,
                    readChapters = entry.second.sumOf { stat -> stat.readChapters },
                    totalChapters = entry.second.sumOf { stat -> stat.totalChapters },
                    readDuration = entry.second.sumOf { stat -> stat.readDuration },
                    totalReadDuration = totalReadDuration,
                )
            }
        }
    }
}

@Composable
private fun LazyGridWrapper(contentPadding: PaddingValues, showSortChip: Boolean = false, sortType: Sort = Sort.Entries, sortChipClick: () -> Unit = {}, content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(400.dp), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
        if (showSortChip) {
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                SortChip(sortType = sortType, onClick = sortChipClick)
            }
        }
        content()
    }
}

@Composable
private fun LazyListWrapper(contentPadding: PaddingValues, showSortChip: Boolean = false, sortType: Sort = Sort.Entries, sortChipClick: () -> Unit = {}, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
        if (showSortChip) {
            item {
                SortChip(sortType = sortType, onClick = sortChipClick)
            }
        }
        content()
    }
}

@Composable
private fun SortChip(sortType: Sort, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AssistChip(
            onClick = onClick,
            label = {
                Text(text = stringResource(id = sortType.stringRes))
            },
            trailingIcon = { Icon(imageVector = Icons.Default.Sort, contentDescription = null) },
        )
    }
}

private fun LazyListScope.CustomChip(isSelected: Boolean, onClick: () -> Unit, @StringRes label: Int) {
    item(key = label) {
        FilterChip(
            selected = isSelected,
            onClick = onClick,
            label = { Text(text = stringResource(id = label)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                selectedLabelColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun DetailedCard(manga: StatsConstants.DetailedStatManga, modifier: Modifier) {
    ElevatedCard(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(text = manga.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Line(stringResource(id = R.string.typeNoSemi), stringResource(id = manga.type.typeRes))
                Line(stringResource(id = R.string.status), stringResource(id = manga.status.statusRes))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Line(stringResource(id = R.string.read_duration), manga.readDuration.getReadDuration(stringResource(id = R.string.none)))
                Line(stringResource(id = R.string.start_year), manga.startYear?.toString() ?: stringResource(id = R.string.n_a))
            }
            Line(stringResource(id = R.string.read_chapter_count), manga.readChapters.toString() + " / " + manga.totalChapters.toString())
            Line(stringResource(id = R.string.categories), manga.categories.joinToString(", "))
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast)),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Pie(pieData: List<PieData>, chartWidth: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (pieData.isNotEmpty()) {
            PieChart(
                modifier = Modifier.fillMaxWidth(chartWidth),
                pieData = pieData,
                config = PieConfig(isDonut = true, expandDonutOnClick = false),
            )
        } else {
            Text(
                text = stringResource(id = R.string.no_data_to_show_in_chart),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Line(lineData: List<LineData>, chartWidth: Float, modifier: Modifier = Modifier, color: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .fillMaxWidth(chartWidth),
        contentAlignment = Alignment.Center,
    ) {
        if (lineData.isNotEmpty()) {
            val height = LocalConfiguration.current.screenHeightDp / 3

            LineChart(
                lineData = lineData,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height.dp)
                    .padding(16.dp),
                chartDimens = ChartDimens(8.dp),
                axisConfig = AxisConfig(
                    showAxis = true,
                    isAxisDashed = false,
                    showUnitLabels = true,
                    showXLabels = true,
                    xAxisColor = MaterialTheme.colorScheme.onSurface,
                    yAxisColor = MaterialTheme.colorScheme.onSurface,
                ),
                lineConfig = LineConfig(hasSmoothCurve = false, hasDotMarker = true),
            )
        } else {
            Text(
                text = stringResource(id = R.string.no_data_to_show_in_chart),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatCard(header: String, headerColor: Color, count: Int, totalCount: Int, readChapters: Int, totalChapters: Int, readDuration: Long, totalReadDuration: Long) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast))
        val valueStyle = MaterialTheme.typography.bodyMedium
        val headerStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = headerColor)

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = header, style = headerStyle, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.count),
                        style = labelStyle,
                    )
                    Text(text = count.toString(), style = valueStyle)
                    Text(text = percentage(count, totalCount), style = valueStyle)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.chapters_read),
                        style = labelStyle,
                    )

                    Text(text = "$readChapters / $totalChapters", style = valueStyle)
                    Text(text = percentage(readChapters, totalChapters), style = valueStyle)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.read_duration),
                        style = labelStyle,
                    )

                    Text(text = readDuration.getReadDuration(stringResource(id = R.string.none)), style = valueStyle)
                    Text(text = percentage(readDuration, totalReadDuration), style = valueStyle)
                }
            }
        }
    }
}

/**
 * Creates a formatted percentage
 */
private fun percentage(count: Long, total: Long): String {
    val percentage = when (count == 0L || total == 0L) {
        true -> 0
        false -> ((count.toDouble() / total.toDouble()) * 100).roundToTwoDecimal()
    }
    return "($percentage%)"
}

private fun percentage(count: Int, total: Int): String {
    return percentage(count.toLong(), total.toLong())
}

/**
 * Returns a comparator depending on the given sort type
 */
private fun <T : Comparable<T>> mapEntryComparator(sortType: Sort) = Comparator<Map.Entry<T, List<StatsConstants.DetailedStatManga>>> { a, b ->
    when (sortType) {
        Sort.Entries -> b.value.size.compareTo(a.value.size)
        Sort.Duration -> b.value.sumOf { it.readDuration }.compareTo(a.value.sumOf { it.readDuration })
        Sort.Chapters -> b.value.sumOf { it.readChapters }.compareTo(a.value.sumOf { it.readChapters })
    }
}

/**
 * Creates the color map using the unique key so changing sort order doesnt change the color
 */
private fun <T> colorMap(sortedSeries: List<T>, colors: List<Color>): ImmutableMap<T, Color> {
    return sortedSeries.mapIndexed { index, T -> T to colors[index] }.toMap().toImmutableMap()
}

private fun <T> pieData(sortedSeries: ImmutableList<Map.Entry<T, List<StatsConstants.DetailedStatManga>>>, colorMap: ImmutableMap<T, Color>, sortType: Sort): ImmutableList<PieData> {
    return sortedSeries.mapNotNull { entry ->
        val data = when (sortType) {
            Sort.Entries -> entry.value.size
            Sort.Chapters -> entry.value.sumOf { it.readChapters }
            Sort.Duration -> entry.value.sumOf { it.readDuration }
        }
        if (data.toFloat() > 0) {
            PieData(data.toFloat(), colorMap[entry.key]!!)
        } else {
            null
        }
    }.toPersistentList()
}

private enum class Filter {
    None,
    Type,
    Status,
    ContentRating,
    Category,
    Tag,
    StartYear,
}

private enum class Sort(@StringRes val stringRes: Int) {
    Entries(R.string.most_entries),
    Chapters(R.string.chapters_read),
    Duration(R.string.read_duration),
}

private enum class ViewType {
    Split,
    Normal,
    Compact,
}
