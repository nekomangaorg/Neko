package org.nekomanga.presentation.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.stats.StatsConstants.SimpleStatMetricUiModel
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.ThemeConfig
import org.nekomanga.presentation.theme.ThemeConfigProvider
import org.nekomanga.presentation.theme.ThemedPreviews

@Composable
fun SimpleStats(
    statsState: StatsConstants.SimpleState,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
) {
    SimpleStats(
        metrics = statsState.simpleStatMetrics,
        contentPadding = contentPadding,
        windowSizeClass = windowSizeClass,
    )
}

@Composable
fun SimpleStats(
    metrics: List<SimpleStatMetricUiModel>,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
) {
    val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val verticalArrangement =
        when (isTablet) {
            true -> Arrangement.Center
            false -> Arrangement.Top
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        val axisPadding =
            when (isTablet) {
                true -> Size.medium
                false -> Size.small
            }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(Size.medium),
                horizontalArrangement =
                    Arrangement.spacedBy(axisPadding, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(axisPadding, Alignment.CenterVertically),
            ) {
                metrics.forEach { metric ->
                    BasicStat(
                        value = metric.formattedValue,
                        label = metric.title.asString(),
                        isTablet = isTablet,
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicStat(value: String, label: String, isTablet: Boolean) {
    val (titleTypography, labelTypography, padding) =
        when (isTablet) {
            true ->
                Triple(
                    MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                    MaterialTheme.typography.titleMedium,
                    20.dp,
                )
            false ->
                Triple(
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    MaterialTheme.typography.labelMedium,
                    12.dp,
                )
        }

    ElevatedCard(shape = RoundedCornerShape(25)) {
        Box(modifier = Modifier.padding(padding)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = titleTypography,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = label, style = labelTypography)
            }
        }
    }
}

@Preview
@Composable
private fun SimpleStatsPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig,
) {
    ThemedPreviews(themeConfig) {
        SimpleStats(
            metrics =
                listOf(
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.total_manga),
                        formattedValue = "1,234",
                    ),
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.total_chapters),
                        formattedValue = "12,450",
                    ),
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.chapters_read),
                        formattedValue = "8,920",
                    ),
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.read_duration),
                        formattedValue = "12d 4h 30min",
                    ),
                ),
            contentPadding = PaddingValues(),
            windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
        )
    }
}

@Preview(name = "SimpleStats Tablet", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun SimpleStatsTabletPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig,
) {
    ThemedPreviews(themeConfig) {
        SimpleStats(
            metrics =
                listOf(
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.total_manga),
                        formattedValue = "1,234",
                    ),
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.total_chapters),
                        formattedValue = "12,450",
                    ),
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.chapters_read),
                        formattedValue = "8,920",
                    ),
                    SimpleStatMetricUiModel(
                        title = UiText.StringResource(R.string.read_duration),
                        formattedValue = "12d 4h 30min",
                    ),
                ),
            contentPadding = PaddingValues(),
            windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
        )
    }
}
