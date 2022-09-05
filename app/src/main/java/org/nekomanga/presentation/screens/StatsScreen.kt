package org.nekomanga.presentation.screens

import ToolTipIconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Detailed
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Loading
import org.nekomanga.presentation.components.ChartColors
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.screens.stats.DetailedStats
import org.nekomanga.presentation.screens.stats.SimpleStats

@Composable
fun StatsScreen(
    statsState: State<StatsConstants.SimpleState>,
    detailedState: State<StatsConstants.DetailedState>,
    onBackPressed: () -> Unit,
    onSwitchClick: () -> Unit,
) {
    val colors = remember {
        arrayListOf(
            ChartColors.one,
            ChartColors.two,
            ChartColors.three,
            ChartColors.four,
            ChartColors.five,
            ChartColors.six,
            ChartColors.seven,
            ChartColors.eight,
            ChartColors.nine,
            ChartColors.ten,
            ChartColors.eleven,
            ChartColors.twelve,
        )
    }

    val isSimple = statsState.value.screenState is StatsConstants.ScreenState.Simple
    val hideAction = statsState.value.screenState is StatsConstants.ScreenState.NoResults || statsState.value.screenState is StatsConstants.ScreenState.Loading


    val (actionIcon, actionText, titleText) = remember(isSimple) {
        when (isSimple) {
            true -> Triple(Icons.Default.ZoomInMap, R.string.view_detailed_statistics, R.string.simple_stats)
            false -> Triple(Icons.Default.ZoomOutMap, R.string.view_simple_statistics, R.string.detailed_stats)
        }
    }


    NekoScaffold(
        title = stringResource(id = titleText),
        onNavigationIconClicked = onBackPressed,
        actions = {
            if (!hideAction) {
                ToolTipIconButton(
                    toolTipLabel = stringResource(id = actionText),
                    icon = actionIcon,
                    buttonClicked = onSwitchClick,
                )
            }
        },
    ) { incomingPaddingValues ->

        if (statsState.value.screenState is Loading || (statsState.value.screenState is Detailed && detailedState.value.isLoading)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingScreen()
            }
        } else if (statsState.value.screenState is StatsConstants.ScreenState.NoResults) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = incomingPaddingValues.calculateTopPadding(), start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                IconicsEmptyScreen(iconicImage = CommunityMaterial.Icon2.cmd_heart_off, iconSize = 128.dp, message = stringResource(id = R.string.unable_to_generate_stats))
            }
        } else {

            if (isSimple) {
                SimpleStats(statsState = statsState, colors = colors, contentPadding = incomingPaddingValues)
            } else {
                DetailedStats(detailedStats = detailedState, colors = colors, contentPadding = incomingPaddingValues)
            }
        }
    }
}

@Composable
private fun SwitchViewRow(isSimpleScreen: Boolean, onSwitchClick: () -> Unit) {
    val buttonText = if (isSimpleScreen) R.string.view_detailed_statistics else R.string.view_simple_statistics
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedButton(onClick = onSwitchClick) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = buttonText),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
            )
        }
    }
}
