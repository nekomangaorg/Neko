package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
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
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.screens.stats.BasicStatRow

@Composable
fun StatsScreen(
    statsState: State<StatsConstants.StatsState>,
    onBackPressed: () -> Unit,
) {
    NekoScaffold(
        title = stringResource(id = R.string.statistics),
        onNavigationIconClicked = onBackPressed,
        /*actions = {
            OverflowOptions(chapterActions = chapterActions, chaptersProvider = { generalState.value.activeChapters })
        }*/
    ) { incomingPaddingValues ->
        if (statsState.value.loading) {
            LoadingScreen()
        } else {
            val na = stringResource(id = R.string.n_a)

            val context = LocalContext.current

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
                    statsState.value.mangaCount.toString() to context.getString(R.string.total_manga),
                    statsState.value.chapterCount.toString() to context.getString(R.string.total_chapters),
                    statsState.value.readCount.toString() to context.getString(R.string.chapters_read),
                    statsState.value.readDuration to context.getString(R.string.read_duration),
                    libUpdates to context.getString(R.string.last_library_update),
                    statsState.value.globalUpdateCount.toString() to context.getString(R.string.global_update_manga),
                    statsState.value.averageMangaRating.toString() to context.getString(R.string.average_score),
                    userScore to context.getString(R.string.mean_score),
                    statsState.value.trackerCount.toString() to context.getString(R.string.trackers),
                    statsState.value.trackedCount.toString() to context.getString(R.string.manga_tracked),
                    statsState.value.tagCount.toString() to context.getString(R.string.tags),
                    statsState.value.mergeCount.toString() to context.getString(R.string.merged),
                ).toImmutableList()
            }
            val statusDistribution = remember {
                statsState.value.statusDistribution.map {
                    val statusRes = when (it.status) {
                        SManga.LICENSED -> R.string.licensed
                        SManga.ONGOING -> R.string.ongoing
                        SManga.COMPLETED -> R.string.completed
                        SManga.PUBLICATION_COMPLETE -> R.string.publication_complete
                        SManga.CANCELLED -> R.string.cancelled
                        SManga.HIATUS -> R.string.hiatus
                        else -> R.string.unknown
                    }
                    it.distribution.toString() to context.getString(statusRes)
                }.sortedBy { it.second }.toImmutableList()
            }



            LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = incomingPaddingValues) {
                item {
                    Label(label = stringResource(id = R.string.general))
                }
                item {
                    BasicStatRow(stats = stats)
                }
                item {
                    Gap(16.dp)
                    Label(label = stringResource(id = R.string.manga_status_distribution))
                }

                item {
                    BasicStatRow(stats = statusDistribution)
                }
            }
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
