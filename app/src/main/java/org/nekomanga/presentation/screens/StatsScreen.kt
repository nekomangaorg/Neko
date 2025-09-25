package org.nekomanga.presentation.screens

import ToolTipButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Detailed
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Loading
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.ChartColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.screens.stats.DetailedStats
import org.nekomanga.presentation.screens.stats.SimpleStats

@Composable
fun StatsScreen(
    statsState: State<StatsConstants.SimpleState>,
    detailedState: State<StatsConstants.DetailedState>,
    onBackPressed: () -> Unit,
    onSwitchClick: () -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    val colors = remember {
        persistentListOf(
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

    val isSimple =
        rememberSaveable(statsState.value.screenState) {
            statsState.value.screenState is StatsConstants.ScreenState.Simple
        }
    val hideAction =
        rememberSaveable(statsState.value.screenState) {
            statsState.value.screenState is StatsConstants.ScreenState.NoResults ||
                statsState.value.screenState is Loading
        }

    val (actionText, titleText) =
        rememberSaveable(isSimple) {
            when (isSimple) {
                true -> Pair(R.string.view_detailed_statistics, R.string.simple_stats)
                false -> Pair(R.string.view_simple_statistics, R.string.detailed_stats)
            }
        }

    val actionIcon =
        when (titleText == R.string.simple_stats) {
            true -> Icons.Default.ZoomInMap
            false -> Icons.Default.ZoomOutMap
        }

    NekoScaffold(
        type = NekoScaffoldType.Title,
        onNavigationIconClicked = onBackPressed,
        title = stringResource(id = titleText),
        actions = {
            if (!hideAction) {
                ToolTipButton(
                    toolTipLabel = stringResource(id = actionText),
                    icon = actionIcon,
                    buttonClicked = onSwitchClick,
                )
            }
        },
        content = { incomingPaddingValues ->
            if (
                statsState.value.screenState is Loading ||
                    (statsState.value.screenState is Detailed && detailedState.value.isLoading)
            ) {
                LoadingScreen(incomingPaddingValues)
            } else if (statsState.value.screenState is StatsConstants.ScreenState.NoResults) {
                EmptyScreen(
                    message = stringResource(id = R.string.unable_to_generate_stats),
                    contentPadding = incomingPaddingValues,
                )
            } else {
                if (isSimple) {
                    SimpleStats(
                        statsState = statsState.value,
                        contentPadding = incomingPaddingValues,
                        windowSizeClass = windowSizeClass,
                    )
                } else {
                    DetailedStats(
                        detailedStats = detailedState.value,
                        colors = colors,
                        contentPadding = incomingPaddingValues,
                        windowSizeClass = windowSizeClass,
                    )
                }
            }
        },
    )
}
