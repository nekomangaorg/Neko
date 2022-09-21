package org.nekomanga.presentation.screens.stats

import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SimpleStats(statsState: StatsConstants.SimpleState, contentPadding: PaddingValues, windowSizeClass: WindowSizeClass) {
    val na = stringResource(id = R.string.n_a)

    val context = LocalContext.current

    val numberFormat = NumberFormat.getInstance(NumberFormat.NUMBERSTYLE)

    val stats = remember {
        val userScore = when (statsState.averageUserRating == 0.0) {
            true -> na
            false -> statsState.averageUserRating.toString()
        }
        val libUpdates = when (statsState.lastLibraryUpdate.isEmpty()) {
            true -> na
            false -> statsState.lastLibraryUpdate
        }

        listOf(
            numberFormat.format(statsState.mangaCount).toString() to context.getString(R.string.total_manga),
            numberFormat.format(statsState.chapterCount).toString() to context.getString(R.string.total_chapters),
            numberFormat.format(statsState.readCount).toString() to context.getString(R.string.chapters_read),
            statsState.readDuration to context.getString(R.string.read_duration),
            libUpdates to context.getString(R.string.last_library_update),
            numberFormat.format(statsState.globalUpdateCount).toString() to context.getString(R.string.global_update_manga),
            statsState.averageMangaRating.toString() to context.getString(R.string.average_score),
            userScore to context.getString(R.string.mean_score),
            statsState.trackerCount.toString() to context.getString(R.string.trackers),
            numberFormat.format(statsState.trackedCount).toString() to context.getString(R.string.manga_tracked),
            numberFormat.format(statsState.tagCount).toString() to context.getString(R.string.tags),
            numberFormat.format(statsState.mergeCount).toString() to context.getString(R.string.merged),
        ).toImmutableList()
    }

    val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val verticalArrangement = when (isTablet) {
        true -> Arrangement.Center
        false -> Arrangement.Top
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = verticalArrangement) {
        val axisPadding = when (isTablet) {
            true -> 16.dp
            false -> 8.dp
        }

        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                mainAxisAlignment = MainAxisAlignment.Center,
                mainAxisSpacing = axisPadding,
                crossAxisAlignment = FlowCrossAxisAlignment.Center,
                crossAxisSpacing = axisPadding,
            ) {
                stats.forEach {
                    BasicStat(value = it.first, label = it.second, isTablet = isTablet)
                }
            }
        }
    }
}

@Composable
private fun BasicStat(value: String, label: String, isTablet: Boolean) {
    val (titleTypography, labelTypography, padding) = when (isTablet) {
        true -> Triple(MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold), MaterialTheme.typography.titleMedium, 20.dp)
        false -> Triple(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), MaterialTheme.typography.labelMedium, 12.dp)
    }

    ElevatedCard(
        shape = RoundedCornerShape(25),
    ) {
        Box(modifier = Modifier.padding(padding)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = value, style = titleTypography, color = MaterialTheme.colorScheme.primary)
                Text(text = label, style = labelTypography)
            }
        }
    }
}
