package org.nekomanga.presentation.screens.stats

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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
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
import eu.kanade.tachiyomi.util.lang.capitalizeWords
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatDistribution
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedStatGroup
import org.nekomanga.presentation.screens.stats.StatsConstants.DetailedState
import org.nekomanga.presentation.screens.stats.StatsConstants.Sort
import org.nekomanga.presentation.screens.stats.StatsHelper.getReadDuration
import org.nekomanga.presentation.theme.Size

@Composable
fun DetailedStats(
    detailedStats: DetailedState,
    colors: List<Color>,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
) {
    var filterState by rememberSaveable { mutableStateOf(Filter.None) }

    var sortType by remember { mutableStateOf(Sort.Entries) }

    val viewType =
        when {
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> ViewType.Split
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium &&
                windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> ViewType.Compact
            else -> ViewType.Normal
        }

    val sortChipClick = {
        sortType =
            when (sortType) {
                Sort.Entries -> Sort.Chapters
                Sort.Chapters -> Sort.Duration
                Sort.Duration -> Sort.Entries
            }
    }

    val filterStateClick = { buttonFilterState: Filter ->
        filterState =
            when (filterState == buttonFilterState) {
                true -> Filter.None
                false -> buttonFilterState
            }
        sortType = Sort.Entries
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = contentPadding.calculateTopPadding())) {
        FilterChipHeader(filterState, filterStateClick)

        when (filterState) {
            Filter.None -> {
                DetailedCardView(detailedStats.manga, contentPadding = contentPadding, viewType)
            }
            Filter.Type -> {
                TypeView(
                    sortType = sortType,
                    distribution = detailedStats.distribution.typeDistribution,
                    colors = colors,
                    contentPadding = contentPadding,
                    viewType = viewType,
                    sortChipClick = sortChipClick,
                )
            }
            Filter.Status -> {
                StatusView(
                    sortType = sortType,
                    distribution = detailedStats.distribution.statusDistribution,
                    colors = colors,
                    contentPadding = contentPadding,
                    viewType = viewType,
                    sortChipClick = sortChipClick,
                )
            }
            Filter.ContentRating -> {
                ContentRatingView(
                    sortType = sortType,
                    distribution = detailedStats.distribution.contentRatingDistribution,
                    colors = colors,
                    contentPadding = contentPadding,
                    viewType = viewType,
                    sortChipClick = sortChipClick,
                )
            }
            Filter.Category -> {
                CategoryView(
                    sortType = sortType,
                    distribution = detailedStats.distribution.categoryDistribution,
                    colors = colors,
                    contentPadding = contentPadding,
                    viewType = viewType,
                    sortChipClick = sortChipClick,
                )
            }
            Filter.Tag -> {
                TagView(
                    sortType = sortType,
                    distribution = detailedStats.distribution.tagDistribution,
                    colors = colors,
                    contentPadding = contentPadding,
                    viewType = viewType,
                    sortChipClick = sortChipClick,
                )
            }
            Filter.StartYear -> {
                StartYearView(
                    distribution = detailedStats.distribution.startYearDistribution,
                    colors = colors,
                    contentPadding = contentPadding,
                    viewType = viewType,
                )
            }
        }
    }
}

@Composable
private fun FilterChipHeader(filterState: Filter, filterStateClick: (Filter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        item { Gap(Size.tiny) }
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
    distribution: DetailedStatDistribution,
    colors: List<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedGroups = remember(sortType, distribution) { distribution.sortedBy(sortType) }
    StatCardView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortedGroups = sortedGroups,
        sortType = sortType,
        sortChipClick = sortChipClick,
        showSortChip = true,
        color = colors[0],
        totalCount = distribution.totalCount,
        totalReadDuration = distribution.totalReadDuration,
        capitalizeHeader = true,
    )
}

@Composable
private fun ContentRatingView(
    sortType: Sort,
    distribution: DetailedStatDistribution,
    colors: List<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedGroups = remember(sortType, distribution) { distribution.sortedBy(sortType) }
    val colorMap = remember(sortedGroups, colors) { colorMap(sortedGroups.map { it.key }, colors) }
    val pieData =
        remember(sortType, sortedGroups, colorMap) { pieData(sortedGroups, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedGroups = sortedGroups,
        colorMap = colorMap,
        totalCount = distribution.totalCount,
        totalDuration = distribution.totalReadDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun CategoryView(
    sortType: Sort,
    distribution: DetailedStatDistribution,
    colors: List<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedGroups = remember(sortType, distribution) { distribution.sortedBy(sortType) }
    val colorMap =
        remember(sortedGroups, colors) {
            if (sortedGroups.size <= colors.size) {
                colorMap(sortedGroups.map { it.key }, colors)
            } else {
                sortedGroups.associate { group ->
                    val hue = (kotlin.math.abs(group.key.hashCode()) % 360).toFloat()
                    group.key to Color.hsl(hue = hue, saturation = 0.65f, lightness = 0.55f)
                }
            }
        }
    val pieData =
        remember(sortType, sortedGroups, colorMap) { pieData(sortedGroups, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedGroups = sortedGroups,
        colorMap = colorMap,
        totalCount = distribution.totalCount,
        totalDuration = distribution.totalReadDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun StartYearView(
    distribution: DetailedStatDistribution,
    colors: List<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
) {
    val notStartedString = stringResource(id = R.string.not_started)
    val sortedGroups =
        remember(distribution.groups, notStartedString) {
            distribution.groups.map { group ->
                if (group.key.isEmpty()) {
                    group.copy(key = notStartedString)
                } else {
                    group
                }
            }
        }

    val lineData =
        remember(distribution.groups) {
            distribution.groups.mapNotNull {
                if (it.key.isNotEmpty()) {
                    LineData(xValue = it.key, yValue = it.count.toFloat())
                } else {
                    null
                }
            }
        }
    val colorMap = remember(sortedGroups, colors) { sortedGroups.associate { it.key to colors[0] } }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        colorMap = colorMap,
        sortedGroups = sortedGroups,
        totalCount = distribution.totalCount,
        totalDuration = distribution.totalReadDuration,
        showSortChip = false,
    ) { modifier, chartWidth ->
        Line(
            lineData = lineData,
            chartWidth = chartWidth,
            modifier = modifier,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun StatusView(
    sortType: Sort,
    distribution: DetailedStatDistribution,
    colors: List<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedGroups = remember(sortType, distribution) { distribution.sortedBy(sortType) }
    val colorMap = remember(sortedGroups, colors) { colorMap(sortedGroups.map { it.key }, colors) }
    val pieData =
        remember(sortType, sortedGroups, colorMap) { pieData(sortedGroups, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedGroups = sortedGroups,
        colorMap = colorMap,
        totalCount = distribution.totalCount,
        totalDuration = distribution.totalReadDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun TypeView(
    sortType: Sort,
    distribution: DetailedStatDistribution,
    colors: List<Color>,
    contentPadding: PaddingValues,
    viewType: ViewType,
    sortChipClick: () -> Unit,
) {
    val sortedGroups = remember(sortType, distribution) { distribution.sortedBy(sortType) }
    val colorMap = remember(sortedGroups, colors) { colorMap(sortedGroups.map { it.key }, colors) }
    val pieData =
        remember(sortType, sortedGroups, colorMap) { pieData(sortedGroups, colorMap, sortType) }

    DefaultView(
        contentPadding = contentPadding,
        viewType = viewType,
        sortType = sortType,
        sortChipClick = sortChipClick,
        sortedGroups = sortedGroups,
        colorMap = colorMap,
        totalCount = distribution.totalCount,
        totalDuration = distribution.totalReadDuration,
    ) { modifier, chartWidth ->
        Pie(pieData = pieData, chartWidth = chartWidth, modifier = modifier)
    }
}

@Composable
private fun DefaultView(
    contentPadding: PaddingValues,
    sortType: Sort = Sort.Entries,
    sortChipClick: () -> Unit = {},
    sortedGroups: List<DetailedStatGroup>,
    colorMap: Map<String, Color>,
    totalCount: Int = 0,
    totalDuration: Long = 0L,
    viewType: ViewType,
    showSortChip: Boolean = true,
    graph: @Composable (modifier: Modifier, chartWidth: Float) -> Unit,
) {
    val chartWidth =
        when (viewType) {
            ViewType.Split -> .5f
            ViewType.Normal -> .9f
            ViewType.Compact -> .6f
        }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        if (showSortChip) {
            item { SortChip(sortType = sortType, onClick = sortChipClick) }
        }

        if (viewType == ViewType.Split) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    graph(Modifier, chartWidth)
                    Column(Modifier.fillMaxWidth(.9f).padding(Size.medium)) {
                        sortedGroups.forEach { group ->
                            StatCard(
                                header = group.key,
                                headerColor =
                                    colorMap[group.key] ?: MaterialTheme.colorScheme.primary,
                                count = group.count,
                                totalCount = totalCount,
                                readChapters = group.readChapters,
                                totalChapters = group.totalChapters,
                                readDuration = group.readDuration,
                                totalReadDuration = totalDuration,
                            )
                        }
                    }
                }
            }
        } else {
            item { graph(Modifier.fillMaxWidth(), chartWidth) }
            item { Gap(Size.small) }
            items(sortedGroups, key = { it.key }) { group ->
                StatCard(
                    header = group.key,
                    headerColor = colorMap[group.key] ?: MaterialTheme.colorScheme.primary,
                    count = group.count,
                    totalCount = totalCount,
                    readChapters = group.readChapters,
                    totalChapters = group.totalChapters,
                    readDuration = group.readDuration,
                    totalReadDuration = totalDuration,
                )
            }
        }
    }
}

@Composable
private fun DetailedCardView(
    mangaList: List<StatsConstants.DetailedStatManga>,
    contentPadding: PaddingValues,
    viewType: ViewType,
) {
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
    sortedGroups: List<DetailedStatGroup>,
    color: Color,
    totalCount: Int,
    totalReadDuration: Long,
    showSortChip: Boolean = false,
    sortType: Sort = Sort.Entries,
    sortChipClick: () -> Unit = {},
    capitalizeHeader: Boolean = false,
) {
    if (viewType == ViewType.Split) {
        LazyGridWrapper(
            contentPadding = contentPadding,
            showSortChip = showSortChip,
            sortType = sortType,
            sortChipClick = sortChipClick,
        ) {
            items(sortedGroups, key = { it.key }) { group ->
                StatCard(
                    header = if (capitalizeHeader) group.key.capitalizeWords() else group.key,
                    headerColor = color,
                    count = group.count,
                    totalCount = totalCount,
                    readChapters = group.readChapters,
                    totalChapters = group.totalChapters,
                    readDuration = group.readDuration,
                    totalReadDuration = totalReadDuration,
                )
            }
        }
    } else {
        LazyListWrapper(
            contentPadding = contentPadding,
            showSortChip = showSortChip,
            sortType = sortType,
            sortChipClick = sortChipClick,
        ) {
            items(sortedGroups, key = { it.key }) { group ->
                StatCard(
                    header = if (capitalizeHeader) group.key.capitalizeWords() else group.key,
                    headerColor = color,
                    count = group.count,
                    totalCount = totalCount,
                    readChapters = group.readChapters,
                    totalChapters = group.totalChapters,
                    readDuration = group.readDuration,
                    totalReadDuration = totalReadDuration,
                )
            }
        }
    }
}

@Composable
private fun LazyGridWrapper(
    contentPadding: PaddingValues,
    showSortChip: Boolean = false,
    sortType: Sort = Sort.Entries,
    sortChipClick: () -> Unit = {},
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(400.dp),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        if (showSortChip) {
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                SortChip(sortType = sortType, onClick = sortChipClick)
            }
        }
        content()
    }
}

@Composable
private fun LazyListWrapper(
    contentPadding: PaddingValues,
    showSortChip: Boolean = false,
    sortType: Sort = Sort.Entries,
    sortChipClick: () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        if (showSortChip) {
            item { SortChip(sortType = sortType, onClick = sortChipClick) }
        }
        content()
    }
}

@Composable
private fun SortChip(sortType: Sort, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(end = Size.medium),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AssistChip(
            onClick = onClick,
            label = { Text(text = stringResource(id = sortType.stringRes)) },
            trailingIcon = {
                Icon(imageVector = Icons.AutoMirrored.Default.Sort, contentDescription = null)
            },
        )
    }
}

private fun LazyListScope.CustomChip(
    isSelected: Boolean,
    onClick: () -> Unit,
    @StringRes label: Int,
) {
    item(key = label) {
        FilterChip(
            selected = isSelected,
            onClick = onClick,
            label = { Text(text = stringResource(id = label)) },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor =
                        MaterialTheme.colorScheme.surfaceColorAtElevation(Size.small),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
        )
    }
}

@Composable
private fun DetailedCard(manga: StatsConstants.DetailedStatManga, modifier: Modifier) {
    ElevatedCard(modifier = modifier.padding(horizontal = Size.medium, vertical = Size.small)) {
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = Size.small, vertical = Size.tiny),
        ) {
            Text(
                text = manga.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Line(
                    stringResource(id = R.string.typeNoSemi),
                    stringResource(id = manga.type.typeRes),
                )
                Line(
                    stringResource(id = R.string.status),
                    stringResource(id = manga.status.statusRes),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Line(
                    stringResource(id = R.string.read_duration),
                    manga.readDuration.getReadDuration(stringResource(id = R.string.none)),
                )
                Line(
                    stringResource(id = R.string.start_year),
                    manga.startYear?.toString() ?: stringResource(id = R.string.n_a),
                )
            }
            Line(
                stringResource(id = R.string.read_chapter_count),
                manga.readChapters.toString() + " / " + manga.totalChapters.toString(),
            )
            Line(
                stringResource(id = R.string.chapters_bookmarked),
                manga.bookmarkedChapters.toString(),
            )
            Line(
                stringResource(id = R.string.chapters_unavailable),
                manga.unavailableChapters.toString(),
            )
            Line(stringResource(id = R.string.categories), manga.categories.joinToString(", "))
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = NekoColors.mediumAlphaHighContrast,
                        ),
                ),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Pie(pieData: List<PieData>, chartWidth: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(Size.medium), contentAlignment = Alignment.Center) {
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Line(
    lineData: List<LineData>,
    chartWidth: Float,
    modifier: Modifier = Modifier,
    color: Color,
) {
    Box(
        modifier =
            Modifier.padding(horizontal = Size.medium, vertical = Size.none)
                .fillMaxWidth(chartWidth),
        contentAlignment = Alignment.Center,
    ) {
        if (lineData.isNotEmpty()) {
            val height = LocalConfiguration.current.screenHeightDp / 3

            LineChart(
                lineData = lineData,
                color = color,
                modifier = Modifier.fillMaxWidth().height(height.dp).padding(Size.medium),
                chartDimens = ChartDimens(Size.small),
                axisConfig =
                    AxisConfig(
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatCard(
    header: String,
    headerColor: Color,
    count: Int,
    totalCount: Int,
    readChapters: Int,
    totalChapters: Int,
    readDuration: Long,
    totalReadDuration: Long,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium, vertical = Size.small),
    ) {
        val labelStyle =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = NekoColors.mediumAlphaHighContrast,
                    ),
            )
        val valueStyle = MaterialTheme.typography.bodyMedium
        val headerStyle =
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = headerColor,
            )

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = Size.small)) {
            Text(text = header, style = headerStyle, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(id = R.string.count), style = labelStyle)
                    Text(text = count.toString(), style = valueStyle)
                    Text(text = percentage(count, totalCount), style = valueStyle)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(id = R.string.chapters_read), style = labelStyle)

                    Text(text = "$readChapters / $totalChapters", style = valueStyle)
                    Text(text = percentage(readChapters, totalChapters), style = valueStyle)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(id = R.string.read_duration), style = labelStyle)

                    Text(
                        text = readDuration.getReadDuration(stringResource(id = R.string.none)),
                        style = valueStyle,
                    )
                    Text(text = percentage(readDuration, totalReadDuration), style = valueStyle)
                }
            }
        }
    }
}

/** Creates a formatted percentage */
private fun percentage(count: Long, total: Long): String {
    val percentage =
        when (count == 0L || total == 0L) {
            true -> 0
            false -> ((count.toDouble() / total.toDouble()) * 100).roundToTwoDecimal()
        }
    return "($percentage%)"
}

private fun percentage(count: Int, total: Int): String {
    return percentage(count.toLong(), total.toLong())
}

/** Creates the color map using the unique key so changing sort order doesnt change the color */
private fun <T> colorMap(sortedSeries: List<T>, colors: List<Color>): Map<T, Color> {
    if (colors.isEmpty()) return emptyMap()
    return sortedSeries.mapIndexed { index, item -> item to colors[index % colors.size] }.toMap()
}

private fun pieData(
    sortedGroups: List<DetailedStatGroup>,
    colorMap: Map<String, Color>,
    sortType: Sort,
): List<PieData> {
    return sortedGroups.mapNotNull { group ->
        val data =
            when (sortType) {
                Sort.Entries -> group.count
                Sort.Chapters -> group.readChapters
                Sort.Duration -> group.readDuration
            }
        if (data.toFloat() > 0) {
            PieData(data.toFloat(), colorMap[group.key] ?: Color.Gray)
        } else {
            null
        }
    }
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

private enum class ViewType {
    Split,
    Normal,
    Compact,
}
