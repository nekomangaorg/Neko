package org.nekomanga.presentation.screens.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Loading
import org.nekomanga.R
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.components.getTopAppBarColor

@Composable
fun StatsTopBar(
    statsState: StatsConstants.SimpleState,
    onNavigationIconClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onSwitchClick: () -> Unit,
) {
    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)

    val hideAction =
        rememberSaveable(statsState.screenState) {
            statsState.screenState is StatsConstants.ScreenState.NoResults ||
                statsState.screenState is Loading
        }

    val isSimple =
        rememberSaveable(statsState.screenState) {
            statsState.screenState is StatsConstants.ScreenState.Simple
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

    TitleTopAppBar(
        color = color,
        title = stringResource(titleText),
        navigationIcon = Icons.AutoMirrored.Default.ArrowBack,
        onNavigationIconClicked = onNavigationIconClicked,
        navigationIconLabel = stringResource(R.string.back),
        incognitoMode = false,
        scrollBehavior = scrollBehavior,
        actions = {
            if (!hideAction) {
                ToolTipButton(
                    toolTipLabel = stringResource(id = actionText),
                    icon = actionIcon,
                    onClick = onSwitchClick,
                )
            }
        },
    )
}
