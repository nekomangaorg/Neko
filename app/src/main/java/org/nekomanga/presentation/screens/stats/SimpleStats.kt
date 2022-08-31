package org.nekomanga.presentation.screens.stats

import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.presentation.components.NekoColors

@Composable
fun SimpleStats(statsState: State<StatsConstants.SimpleState>) {

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
    val statusDistribution = remember {
        statsState.value.statusDistribution.map {
            numberFormat.format(it.distribution).toString() to context.getString(it.status.statusRes)
        }.sortedBy { it.second }.toImmutableList()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Label(label = stringResource(id = R.string.general))
        BasicStatRow(stats = stats)
        Gap(16.dp)
        Label(label = stringResource(id = R.string.manga_status_distribution))
        BasicStatRow(stats = statusDistribution)
    }
}

@Composable
private fun ColumnScope.Label(label: String) {
    Text(
        modifier = Modifier.padding(start = 8.dp),
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
    )
    Gap(8.dp)
}
