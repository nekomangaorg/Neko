package org.nekomanga.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Detailed
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Loading
import eu.kanade.tachiyomi.ui.more.stats.StatsViewModel
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.ChartColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.stats.DetailedStats
import org.nekomanga.presentation.screens.stats.SimpleStats
import org.nekomanga.presentation.screens.stats.StatsTopBar

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onBackPressed: () -> Unit,
    windowSizeClass: WindowSizeClass,
) {

    val statsState by statsViewModel.simpleState.collectAsStateWithLifecycle()
    val detailedState by statsViewModel.detailState.collectAsStateWithLifecycle()

    StatsWrapper(
        statsState = statsState,
        detailedState = detailedState,
        onBackPressed = onBackPressed,
        onSwitchClick = statsViewModel::switchState,
        windowSizeClass = windowSizeClass,
    )
}

@Composable
fun StatsWrapper(
    statsState: StatsConstants.SimpleState,
    detailedState: StatsConstants.DetailedState,
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
        rememberSaveable(statsState.screenState) {
            statsState.screenState is StatsConstants.ScreenState.Simple
        }
    val hideAction =
        rememberSaveable(statsState.screenState) {
            statsState.screenState is StatsConstants.ScreenState.NoResults ||
                statsState.screenState is Loading
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

    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

    ChildScreenScaffold(
        scrollBehavior = scrollBehavior,
        topBar = {
            StatsTopBar(
                statsState = statsState,
                onNavigationIconClicked = onBackPressed,
                scrollBehavior = scrollBehavior,
                onSwitchClick = onSwitchClick,
            )
        },
    ) { contentPadding ->
        if (
            statsState.screenState is Loading ||
                (statsState.screenState is Detailed && detailedState.isLoading)
        ) {
            LoadingScreen()
        } else if (statsState.screenState is StatsConstants.ScreenState.NoResults) {
            EmptyScreen(
                message = UiText.StringResource(resourceId = R.string.unable_to_generate_stats),
                contentPadding = contentPadding,
            )
        } else {
            if (isSimple) {
                SimpleStats(
                    statsState = statsState,
                    contentPadding = contentPadding,
                    windowSizeClass = windowSizeClass,
                )
            } else {
                DetailedStats(
                    detailedStats = detailedState,
                    colors = colors,
                    contentPadding = contentPadding,
                    windowSizeClass = windowSizeClass,
                )
            }
        }
    }
}
