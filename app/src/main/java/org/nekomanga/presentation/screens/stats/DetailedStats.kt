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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import eu.kanade.tachiyomi.util.system.roundToTwoDecimal
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

    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = contentPadding) {
        item {

            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    CustomChip(
                        isSelected = filterState == Filter.Type,
                        onClick = { filterStateClick(Filter.Type) },
                        label = stringResource(id = R.string.series_type),
                    )
                }
                item {
                    CustomChip(
                        isSelected = filterState == Filter.Status,
                        onClick = { filterStateClick(Filter.Status) },
                        label = stringResource(id = R.string.status),
                    )
                }
                item {
                    CustomChip(
                        isSelected = filterState == Filter.ContentRating,
                        onClick = { filterStateClick(Filter.ContentRating) },
                        label = stringResource(id = R.string.content_rating_distribution),
                    )
                }
            }
        }


        when (filterState) {
            Filter.None -> {
                items(detailedStats.value.manga, key = { manga -> manga.id }) { manga ->
                    DetailedCard(manga)
                }
            }
            Filter.Type -> {
                item {
                    Type(detailedStats = detailedStats, colors = colors, sortType = sortType, sortTypeClick = sortTypeClick)
                }
            }

            Filter.Status -> {
                item {
                    Status(detailedStats = detailedStats, colors = colors, sortType = sortType, sortTypeClick = sortTypeClick)
                }
            }

            Filter.ContentRating -> {
                item {
                    ContentRating(detailedStats = detailedStats, colors = colors, sortType = sortType, sortTypeClick = sortTypeClick)
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun SortChip(sortType: Sort, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        AssistChip(
            onClick = onClick,
            label = {
                Text(text = stringResource(id = sortType.stringRes))
            },
            trailingIcon = { Icon(imageVector = Icons.Default.Sort, contentDescription = null) },
        )
    }
}

@Composable
private fun CustomChip(isSelected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = isSelected,
        leadingIcon = {
            if (isSelected) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        onClick = onClick,
        label = { Text(text = label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
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
        PieChart(
            modifier = Modifier
                .scale(1f)
                .size(chartSize),
            pieData = pieData, config = PieConfig(isDonut = true, expandDonutOnClick = false),
        )
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
private fun Type(detailedStats: State<StatsConstants.DetailedState>, sortType: Sort, colors: List<Color>, sortTypeClick: () -> Unit) {
    val sortedSeries = remember { detailedStats.value.manga.groupBy { it.type }.entries.sortedByDescending { it.value.size } }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }

    val pieData = remember(sortType) {
        sortedSeries.mapIndexed { index, it ->
            val data = when (sortType) {
                Sort.Entries -> it.value.size
                Sort.Chapters -> it.value.sumOf { it.readChapters }
                Sort.Duration -> it.value.sumOf { it.readDuration }
            }
            PieData(data.toFloat(), colors[index])
        }
    }

    SortChip(sortType, onClick = sortTypeClick)
    Pie(pieData = pieData)

    sortedSeries.forEachIndexed { index, entry ->
        StatCard(
            header = stringResource(id = entry.key.typeRes),
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
private fun Status(detailedStats: State<StatsConstants.DetailedState>, sortType: Sort, colors: List<Color>, sortTypeClick: () -> Unit) {
    val sortedSeries = remember { detailedStats.value.manga.groupBy { it.status }.entries.sortedByDescending { it.value.size } }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }

    val pieData = remember(sortType) {
        sortedSeries.mapIndexed { index, it ->
            val data = when (sortType) {
                Sort.Entries -> it.value.size
                Sort.Chapters -> it.value.sumOf { it.readChapters }
                Sort.Duration -> it.value.sumOf { it.readDuration }
            }
            PieData(data.toFloat(), colors[index])
        }
    }

    SortChip(sortType, onClick = sortTypeClick)
    Pie(pieData = pieData)

    sortedSeries.forEachIndexed { index, entry ->
        StatCard(
            header = stringResource(id = entry.key.statusRes),
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
private fun ContentRating(detailedStats: State<StatsConstants.DetailedState>, sortType: Sort, colors: List<Color>, sortTypeClick: () -> Unit) {
    val sortedSeries = remember { detailedStats.value.manga.groupBy { it.contentRating }.entries.sortedByDescending { it.value.size } }
    val totalCount = remember { sortedSeries.sumOf { it.value.size } }
    val totalDuration = remember { sortedSeries.sumOf { values -> values.value.sumOf { it.readDuration } } }

    val pieData = remember(sortType) {
        sortedSeries.mapIndexed { index, it ->
            val data = when (sortType) {
                Sort.Entries -> it.value.size
                Sort.Chapters -> it.value.sumOf { it.readChapters }
                Sort.Duration -> it.value.sumOf { it.readDuration }
            }
            PieData(data.toFloat(), colors[index])
        }
    }

    SortChip(sortType, onClick = sortTypeClick)
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

private enum class Filter {
    None,
    Type,
    Status,
    ContentRating,
    ReadDuration
}

private enum class Sort(@StringRes val stringRes: Int) {
    Entries(R.string.most_entries),
    Chapters(R.string.chapters_read),
    Duration(R.string.read_duration)
}

