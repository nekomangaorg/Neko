package org.nekomanga.presentation.screens.feed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenState
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.SearchOutlineTopAppBar
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.functions.getTopAppBarColor

@Composable
fun FeedScreenTopBar(
    feedScreenState: FeedScreenState,
    feedScreenActions: FeedScreenActions,
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    openSheetClick: () -> Unit,
) {

    val isSummaryView =
        feedScreenState.feedScreenType == FeedScreenType.Summary &&
            !feedScreenState.showingDownloads

    val titleOnlyAppBar =
        remember(feedScreenState.feedScreenType) {
            feedScreenState.feedScreenType !in
                listOf(FeedScreenType.Updates, FeedScreenType.History)
        }

    val (color, _, _) = getTopAppBarColor("", false)

    TitleTopAppBar(
        color = color,
        title = if (isSummaryView) stringResource(R.string.summary) else "",
        incognitoMode = feedScreenState.incognitoMode,
        actions = {
            val actionsList = buildList {
                if (!isSummaryView) {
                    add(
                        AppBar.Action(
                            title = UiText.StringResource(R.string.settings),
                            icon = Icons.Outlined.Tune,
                            onClick = openSheetClick,
                        )
                    )
                }
                add(mainDropDown)
            }

            AppBarActions(actions = actionsList)
        },
        scrollBehavior = scrollBehavior,
    )
    if (!titleOnlyAppBar) {

        val searchHint =
            when (feedScreenState.feedScreenType) {
                FeedScreenType.History -> stringResource(R.string.search_history)
                FeedScreenType.Updates -> stringResource(R.string.search_updates)
                else -> ""
            }

        SearchOutlineTopAppBar(
            onSearch = feedScreenActions.search,
            searchPlaceHolder = searchHint,
            color = color,
            incognitoMode = feedScreenState.incognitoMode,
            actions = {
                AppBarActions(
                    actions =
                        listOf(
                            AppBar.Action(
                                title = UiText.StringResource(R.string.settings),
                                icon = Icons.Outlined.Tune,
                                onClick = openSheetClick,
                            ),
                            mainDropDown,
                        )
                )
            },
            scrollBehavior = scrollBehavior,
        )
    }
}
