package org.nekomanga.presentation.screens.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.functions.getTopAppBarColor

@Composable
fun StatsTopBar(
    statsState: StatsConstants.SimpleState,
    onNavigationIconClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onSwitchClick: () -> Unit,
) {
    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)

    // Return null for states that shouldn't have a title
    val titleTextRes =
        remember(statsState.screenState) {
            when (statsState.screenState) {
                ScreenState.Simple -> R.string.simple_stats
                ScreenState.Detailed -> R.string.detailed_stats
                else -> null
            }
        }

    TitleTopAppBar(
        color = color,
        title = titleTextRes?.let { stringResource(it) } ?: "",
        navigationIcon = Icons.AutoMirrored.Default.ArrowBack,
        onNavigationIconClicked = onNavigationIconClicked,
        navigationIconLabel = stringResource(R.string.back),
        incognitoMode = false,
        scrollBehavior = scrollBehavior,
        actions = {
            // Group the specific action logic directly by state
            when (statsState.screenState) {
                ScreenState.Simple -> {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.view_simple_statistics),
                        icon = Icons.Default.ZoomInMap,
                        onClick = onSwitchClick,
                    )
                }
                ScreenState.Detailed -> {
                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.view_detailed_statistics),
                        icon = Icons.Default.ZoomOutMap,
                        onClick = onSwitchClick,
                    )
                }
                else -> {}
            }
        },
    )
}
