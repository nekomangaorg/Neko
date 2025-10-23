package org.nekomanga.presentation.screens.feed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
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
import org.nekomanga.presentation.components.getTopAppBarColor

@Composable
fun FeedScreenTopBar(
    feedScreenState: FeedScreenState,
    feedScreenActions: FeedScreenActions,
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    openSheetClick: () -> Unit,
) {

    val titleOnlyAppBar =
        remember(feedScreenState.showingDownloads, feedScreenState.feedScreenType) {
            feedScreenState.showingDownloads ||
                feedScreenState.feedScreenType == FeedScreenType.Summary
        }

    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)

    if (titleOnlyAppBar) {
        TitleTopAppBar(
            color = color,
            title =
                if (feedScreenState.feedScreenType == FeedScreenType.Summary)
                    stringResource(R.string.summary)
                else "",
            incognitoMode = feedScreenState.incognitoMode,
            actions = { AppBarActions(actions = listOf(mainDropDown)) },
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        )
    } else {

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
