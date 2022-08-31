package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Detailed
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants.ScreenState.Loading
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.screens.stats.SimpleStats
import org.nekomanga.presentation.screens.stats.detailedStats

@Composable
fun StatsScreen(
    statsState: State<StatsConstants.SimpleState>,
    detailedState: State<StatsConstants.DetailedState>,
    onBackPressed: () -> Unit,
    onSwitchClick: () -> Unit,
) {
    NekoScaffold(
        title = stringResource(id = R.string.statistics),
        onNavigationIconClicked = onBackPressed,
    ) { incomingPaddingValues ->
        LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = incomingPaddingValues) {

            if (statsState.value.screenState is Loading || (statsState.value.screenState is Detailed && detailedState.value.isLoading)) {
                item {
                    LoadingScreen()
                }
            } else {
                val isSimpleScreen = statsState.value.screenState is StatsConstants.ScreenState.Simple

                val buttonText = if (isSimpleScreen) R.string.view_detailed_statistics else R.string.view_simple_statistics

                item {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp), horizontalArrangement = Arrangement.End) {
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
                if (statsState.value.screenState is StatsConstants.ScreenState.Simple) {
                    item {
                        SimpleStats(statsState = statsState)
                    }
                } else {
                    detailedStats(detailedStats = detailedState)
                }
            }
        }
    }
}
