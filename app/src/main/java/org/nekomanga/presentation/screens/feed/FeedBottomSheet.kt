package org.nekomanga.presentation.screens.feed

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.feed.FeedHistoryGroup
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedBottomSheet(
    contentPadding: PaddingValues,
    feedScreenType: FeedScreenType,
    downloadScreenVisible: Boolean,
    downloadOnlyOnWifi: Boolean,
    historyGrouping: FeedHistoryGroup,
    sortByFetched: Boolean,
    outlineCovers: Boolean,
    outlineCards: Boolean,
    swipeRefreshEnabled: Boolean,
    groupUpdateChapters: Boolean,
    toggleGroupUpdateChapters: () -> Unit,
    sortClick: () -> Unit,
    outlineCardsClick: () -> Unit,
    outlineCoversClick: () -> Unit,
    groupHistoryClick: (FeedHistoryGroup) -> Unit,
    clearHistoryClick: () -> Unit,
    clearDownloadsClick: () -> Unit,
    toggleDownloadOnWifi: () -> Unit,
    toggleSwipeRefresh: () -> Unit,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {

    BaseSheet(
        themeColor = themeColorState,
        maxSheetHeightPercentage = .6f,
        bottomPaddingAroundContent = contentPadding.calculateBottomPadding(),
    ) {
        Gap(16.dp)

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
            verticalArrangement = Arrangement.spacedBy(Size.small),
        ) {
            when {
                downloadScreenVisible ->
                    downloadsContent(clearDownloadsClick, downloadOnlyOnWifi, toggleDownloadOnWifi)
                feedScreenType == FeedScreenType.History ->
                    historyContent(
                        historyGrouping = historyGrouping,
                        outlineCovers = outlineCovers,
                        outlineCards = outlineCards,
                        outlineCoversClick = outlineCoversClick,
                        outlineCardsClick = outlineCardsClick,
                        groupHistoryClick = groupHistoryClick,
                        clearHistoryClick = clearHistoryClick,
                        swipeRefreshEnabled = swipeRefreshEnabled,
                        toggleSwipeRefresh = toggleSwipeRefresh,
                    )
                feedScreenType == FeedScreenType.Updates ->
                    uploadsContent(
                        fetchSort = sortByFetched,
                        outlineCovers = outlineCovers,
                        sortClick = sortClick,
                        groupUpdateChapters = groupUpdateChapters,
                        toggleGroupUpdateChapters = toggleGroupUpdateChapters,
                        outlineCoversClick = outlineCoversClick,
                        swipeRefreshEnabled = swipeRefreshEnabled,
                        toggleSwipeRefresh = toggleSwipeRefresh,
                    )
            }
        }
    }
}

private fun LazyListScope.historyContent(
    historyGrouping: FeedHistoryGroup,
    outlineCovers: Boolean,
    outlineCards: Boolean,
    swipeRefreshEnabled: Boolean,
    toggleSwipeRefresh: () -> Unit,
    outlineCoversClick: () -> Unit,
    outlineCardsClick: () -> Unit,
    groupHistoryClick: (FeedHistoryGroup) -> Unit,
    clearHistoryClick: () -> Unit,
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.group_chapters_together),
                style = MaterialTheme.typography.bodyMedium,
            )
            var expanded by remember { mutableStateOf(false) }
            var selectedText =
                when (historyGrouping) {
                    FeedHistoryGroup.No -> R.string.group_no
                    FeedHistoryGroup.Series -> R.string.group_by_series
                    FeedHistoryGroup.Day -> R.string.group_by_day
                    FeedHistoryGroup.Week -> R.string.group_by_week
                }
            ExposedDropdownMenuBox(
                expanded = expanded,
                modifier =
                    Modifier.padding(
                        start = Size.small,
                        top = Size.small,
                        bottom = Size.small,
                        end = Size.none,
                    ),
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    value = stringResource(id = selectedText),
                    onValueChange = {},
                    textStyle = MaterialTheme.typography.bodyMedium,
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    FeedHistoryGroup.entries.forEach { entry ->
                        val textRes =
                            when (entry) {
                                FeedHistoryGroup.No -> R.string.group_no
                                FeedHistoryGroup.Series -> R.string.group_by_series
                                FeedHistoryGroup.Day -> R.string.group_by_day
                                FeedHistoryGroup.Week -> R.string.group_by_week
                            }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(id = textRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                selectedText = textRes
                                expanded = false
                                groupHistoryClick(entry)
                            },
                        )
                    }
                }
            }
        }
    }
    item { SwitchRow(R.string.show_outline_around_covers, outlineCovers, outlineCoversClick) }
    item { SwitchRow(R.string.show_outline_around_cards, outlineCards, outlineCardsClick) }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = clearHistoryClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null)
                    Text(text = stringResource(id = R.string.clear_history))
                }
            }
        }
    }
    item { SwitchRow(R.string.feed_swipe_refresh_enabled, swipeRefreshEnabled, toggleSwipeRefresh) }
}

private fun LazyListScope.downloadsContent(
    clearDownloadsClick: () -> Unit,
    downloadOnlyOnWifi: Boolean,
    toggleDownloadOnWifi: () -> Unit,
) {
    item {
        SwitchRow(
            textRes = R.string.only_download_over_wifi,
            downloadOnlyOnWifi,
            toggleDownloadOnWifi,
        )
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = clearDownloadsClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null)
                    Gap(Size.small)
                    Text(text = stringResource(id = R.string.clear_download_queue))
                }
            }
        }
    }
}

private fun LazyListScope.uploadsContent(
    fetchSort: Boolean,
    outlineCovers: Boolean,
    groupUpdateChapters: Boolean,
    toggleGroupUpdateChapters: () -> Unit,
    sortClick: () -> Unit,
    outlineCoversClick: () -> Unit,
    swipeRefreshEnabled: Boolean,
    toggleSwipeRefresh: () -> Unit,
) {
    item { SwitchRow(R.string.sort_fetched_time, fetchSort, sortClick) }
    item { SwitchRow(R.string.show_outline_around_covers, outlineCovers, outlineCoversClick) }
    item {
        SwitchRow(R.string.group_chapters_together, groupUpdateChapters, toggleGroupUpdateChapters)
    }
    item { SwitchRow(R.string.feed_swipe_refresh_enabled, swipeRefreshEnabled, toggleSwipeRefresh) }
}

@Composable
private fun SwitchRow(@StringRes textRes: Int, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(id = textRes), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = { onClick() })
    }
}
