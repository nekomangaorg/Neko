package org.nekomanga.presentation.screens.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.source.browse.BrowseScreenState
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.components.getTopAppBarColor

@Composable
fun BrowseScreenTopBar(
    browseScreenState: BrowseScreenState,
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    openSheetClick: () -> Unit,
) {

    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)

    TitleTopAppBar(
        color = color,
        title = browseScreenState.title.asString(),
        incognitoMode = browseScreenState.incognitoMode,
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
