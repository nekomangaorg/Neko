package org.nekomanga.presentation.screens.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.himanshoe.charty.pie.PieChart
import com.himanshoe.charty.pie.config.PieConfig
import com.himanshoe.charty.pie.config.PieData
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import eu.kanade.tachiyomi.ui.more.stats.StatsHelper.getReadDuration
import eu.kanade.tachiyomi.util.lang.capitalizeWords
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.extensions.surfaceColorAtElevation

@Composable
fun DetailedStats(detailedStats: State<StatsConstants.DetailedState>, colors: List<Color>, contentPadding: PaddingValues) {

    var filterState by rememberSaveable { mutableStateOf(Filter.None) }

    var sortType by remember { mutableStateOf(Sort.Entries) }

    val sortTypeClick = {
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
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            customChip(
                isSelected = filterState == Filter.Type,
                onClick = { filterStateClick(Filter.Type) },
                label = R.string.series_type,
            )
            customChip(
                isSelected = filterState == Filter.Status,
                onClick = { filterStateClick(Filter.Status) },
                label = R.string.status,
            )
            customChip(
                isSelected = filterState == Filter.ContentRating,
                onClick = { filterStateClick(Filter.ContentRating) },
                label = R.string.content_rating_distribution,
            )
            /* customChip(
                 isSelected = filterState == Filter.Tag,
                 onClick = { filterStateClick(Filter.Tag) },
                 label = R.string.tag,
             )*/
        }

        when (filterState) {
            Filter.None -> {
                LazyWrapper(
                    contentPadding = contentPadding,
                    contentList = detailedStats.value.manga.map { manga -> { DetailedCard(manga) } },
                )
            }
            Filter.Type -> {
                LazyWrapper(
                    contentPadding = contentPadding,
                    contentList = listOf(
                        { SortChip(sortType, onClick = sortTypeClick) },
                        { Type(detailedStats = detailedStats, colors = colors, sortType = sortType) },
                    ),
                )
            }

            Filter.Status -> {
                LazyWrapper(
                    contentPadding = contentPadding,
                    contentList = listOf(
                        { SortChip(sortType, onClick = sortTypeClick) },
                        { Status(detailedStats = detailedStats, colors = colors, sortType = sortType) },
                    ),
                )
            }

            Filter.ContentRating -> {
                LazyWrapper(
                    contentPadding = contentPadding,
                    contentList = listOf(
                        { SortChip(sortType, onClick = sortTypeClick) },
                        { ContentRating(detailedStats = detailedStats, sortType = sortType, colors = colors) },
                    ),
                )
            }
            /*Filter.Tag -> {
                val tagStats = detailedStats.value.detailTagState
                val sortedTagPairs = when (sortType) {

                }
                items(tagStats.sortedTagPairs) { tagPair ->
                    Tag(entry = tagPair, color = colors[0], totalCount = detailedStats.value.detailTagState.totalChapters, totalReadDuration = detailedStats.value.detailTagState.totalReadDuration)
                }
            }*/

            else -> Unit
        }
    }
}

@Composable
private fun LazyWrapper(contentPadding: PaddingValues, contentList: List<@Composable () -> Unit>) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
        contentList.forEach { content ->
            item { content() }
        }
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

private fun LazyListScope.customChip(isSelected: Boolean, onClick: () -> Unit, @StringRes label: Int) {
    item {
        FilterChip(
            selected = isSelected,
            leadingIcon = {
                if (isSelected) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            },
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
private fun DetailedCard(manga: StatsConstants.DetailedStatManga) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(text = manga.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Line(stringResource(id = R.string.typeNoSemi), stringResource(id = manga.type.typeRes))
                Line(stringResource(id = R.string.status), stringResource(id = manga.status.statusRes))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Pie(pieData: List<PieData>) {
    val chartSize = (LocalConfiguration.current.screenWidthDp / 1.2).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (pieData.isNotEmpty()) {
            PieChart(
                modifier = Modifier
                    .scale(1f)
                    .size(chartSize),
                pieData = pieData, config = PieConfig(isDonut = true, expandDonutOnClick = false),
            )
        } else {
            Text(text = stringResource(id = R.string.no_data_to_show_in_chart), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatCard(header: String, headerColor: Color, count: Int, totalCount: Int, readChapters: Int, totalChapters: Int, readDuration: Long, totalReadDuration: Long) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val labelStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaHighContrast))
        val valueStyle = MaterialTheme.typography.bodyMedium

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = header, style = MaterialTheme.typography.titleMedium, color = headerColor)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = stringResource(id = R.string.count),
                        style = labelStyle,
                    )
                    Text(text = count.toString(), style = valueStyle)
                    Text(text = percentage(count, totalCount), style = valueStyle)
                }
                Column {
                    Text(
                        text = stringResource(id = R.string.chapters_read),
                        style = labelStyle,
                    )

                    Text(text = "$readChapters / $totalChapters", style = valueStyle)
                    Text(text = percentage(readChapters, totalChapters), style = valueStyle)
                }
                Column {
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

@Composable
private fun Type(detailedStats: State<StatsConstants.DetailedState>, sortType: Sort, colors: List<Color>) {
    val sortedSeries = remember(sortType) {
        detailedStats.value.manga.groupBy { it.type }.entries.sortedWith(mapEntryComparator(sortType))
    }
    val colorMap = remember { colorMap(sortedSeries.map { it.key }, colors) }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }
    val pieData = remember(sortType) { pieData(sortedSeries, colorMap, sortType) }


    Pie(pieData = pieData)

    sortedSeries.forEach { entry ->
        StatCard(
            header = stringResource(id = entry.key.typeRes),
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

@Composable
private fun Status(detailedStats: State<StatsConstants.DetailedState>, sortType: Sort, colors: List<Color>) {
    val sortedSeries = remember(sortType) {
        detailedStats.value.manga.groupBy { it.status }.entries.sortedWith(mapEntryComparator(sortType))
    }
    val colorMap = remember { colorMap(sortedSeries.map { it.key }, colors) }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }
    val pieData = remember(sortType) { pieData(sortedSeries, colorMap, sortType) }


    Pie(pieData = pieData)

    sortedSeries.forEach { entry ->
        StatCard(
            header = stringResource(id = entry.key.statusRes),
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

@Composable
private fun ContentRating(detailedStats: State<StatsConstants.DetailedState>, sortType: Sort, colors: List<Color>) {
    val sortedSeries = remember(sortType) {
        detailedStats.value.manga.groupBy { it.contentRating }.entries.sortedWith(mapEntryComparator(sortType))
    }
    val colorMap = remember { colorMap(sortedSeries.map { it.key }, colors) }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }
    val pieData = remember(sortType) { pieData(sortedSeries, colorMap, sortType) }

    Pie(pieData = pieData)

    sortedSeries.forEachIndexed { index, entry ->
        StatCard(
            header = entry.key.prettyPrint(),
            headerColor = colors[index],
            count = entry.value.size,
            totalCount = totalCount,
            readChapters = entry.value.sumOf { stat -> stat.readChapters },
            totalChapters = entry.value.sumOf { stat -> stat.totalChapters },
            readDuration = entry.value.sumOf { stat -> stat.readDuration },
            totalReadDuration = totalDuration,
        )
    }
}

@Composable
private fun Tag(entry: Pair<String, ImmutableList<StatsConstants.DetailedStatManga>>, color: Color, totalCount: Int, totalReadDuration: Long) {
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
private fun <T> colorMap(sortedSeries: List<T>, colors: List<Color>): Map<T, Color> {
    return sortedSeries.mapIndexed { index, T -> T to colors[index] }.toMap()
}

private fun <T> pieData(sortedSeries: List<Map.Entry<T, List<StatsConstants.DetailedStatManga>>>, colorMap: Map<T, Color>, sortType: Sort): List<PieData> {
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
    }
}

private enum class Filter {
    None,
    Type,
    Status,
    ContentRating,
    Tag,
}

private enum class Sort(@StringRes val stringRes: Int) {
    Entries(R.string.most_entries),
    Chapters(R.string.chapters_read),
    Duration(R.string.read_duration)
}

