package org.nekomanga.presentation.screens.stats

import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.himanshoe.charty.pie.PieChart
import com.himanshoe.charty.pie.config.PieConfig
import com.himanshoe.charty.pie.config.PieData
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.presentation.components.NekoColors

@Composable
fun SimpleStats(statsState: State<StatsConstants.SimpleState>, contentPadding: PaddingValues, switchRow: @Composable () -> Unit) {

    val na = stringResource(id = R.string.n_a)

    val context = LocalContext.current

    val numberFormat = NumberFormat.getInstance(NumberFormat.NUMBERSTYLE)

    val stats = remember {

        val userScore = when (statsState.value.averageUserRating == 0.0) {
            true -> na
            false -> statsState.value.averageUserRating.toString()
        }
        val libUpdates = when (statsState.value.lastLibraryUpdate.isEmpty()) {
            true -> na
            false -> statsState.value.lastLibraryUpdate
        }

        listOf(
            numberFormat.format(statsState.value.mangaCount).toString() to context.getString(R.string.total_manga),
            numberFormat.format(statsState.value.chapterCount).toString() to context.getString(R.string.total_chapters),
            numberFormat.format(statsState.value.readCount).toString() to context.getString(R.string.chapters_read),
            statsState.value.readDuration to context.getString(R.string.read_duration),
            libUpdates to context.getString(R.string.last_library_update),
            numberFormat.format(statsState.value.globalUpdateCount).toString() to context.getString(R.string.global_update_manga),
            statsState.value.averageMangaRating.toString() to context.getString(R.string.average_score),
            userScore to context.getString(R.string.mean_score),
            statsState.value.trackerCount.toString() to context.getString(R.string.trackers),
            numberFormat.format(statsState.value.trackedCount).toString() to context.getString(R.string.manga_tracked),
            numberFormat.format(statsState.value.tagCount).toString() to context.getString(R.string.tags),
            numberFormat.format(statsState.value.mergeCount).toString() to context.getString(R.string.merged),
        ).toImmutableList()
    }

    val size = (LocalConfiguration.current.screenWidthDp / 2.8).dp

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {

        item {
            switchRow()
        }
        item {
            Label(label = stringResource(id = R.string.general))
            BasicStatRow(stats = stats)
        }

        item {
            Gap(16.dp)
            Label(label = stringResource(id = R.string.manga_status_distribution))

            val statusDistribution = remember {
                statsState.value.statusDistribution.sortedByDescending { it.distribution }.toImmutableList()
            }

            val statusDistributionText = remember {
                statusDistribution.map {
                    PieRowText(context.getString(it.status.statusRes) + ": " + numberFormat.format(it.distribution), Color(it.status.color))
                }.toImmutableList()
            }

            val statusDistributionPieData = remember {
                statusDistribution.map {
                    PieData(it.distribution.toFloat(), Color(it.status.color))
                }
            }

            PieRow(pieData = statusDistributionPieData, pieSize = size, pieRowText = statusDistributionText)
        }

        item {
            Gap(16.dp)
            Label(label = stringResource(id = R.string.content_rating_distribution))

            val contentRatingDistribution = remember {
                statsState.value.contentRatingDistribution.sortedByDescending { it.distribution }.toImmutableList()
            }

            val contentRatingDistributionText = remember {
                contentRatingDistribution.map {
                    PieRowText(it.rating.prettyPrint() + ": " + numberFormat.format(it.distribution), Color(it.rating.color))
                }.toImmutableList()
            }

            val contentRatingDistributionPieData = remember {
                contentRatingDistribution.map {
                    PieData(it.distribution.toFloat(), Color(it.rating.color))
                }
            }

            PieRow(pieData = contentRatingDistributionPieData, pieSize = size, pieRowText = contentRatingDistributionText)
        }

    }
}

@Composable
private fun LazyItemScope.Label(label: String) {
    Text(
        modifier = Modifier.padding(start = 8.dp),
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
    )
    Gap(8.dp)
}

private data class PieRowText(val text: String, val color: Color)

@Composable
private fun PieRow(pieData: List<PieData>, pieSize: Dp, pieRowText: ImmutableList<PieRowText>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PieChart(
            modifier = Modifier
                .scale(1f)
                .size(pieSize),
            pieData = pieData, config = PieConfig(isDonut = true, expandDonutOnClick = false),
        )
        Column(
            Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
        ) {
            pieRowText.forEach {
                Text(text = it.text, style = MaterialTheme.typography.bodyMedium.copy(color = it.color, fontWeight = FontWeight.Medium))
            }
        }
    }
}
