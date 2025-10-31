package org.nekomanga.presentation.screens.display

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenState
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.components.getTopAppBarColor

@Composable
fun DisplayTopBar(
    screenState: DisplayScreenState,
    onNavigationIconClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingClick: () -> Unit,
) {
    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)
    TitleTopAppBar(
        color = color,
        title = screenState.title.asString(),
        navigationIcon = Icons.AutoMirrored.Default.ArrowBack,
        onNavigationIconClicked = onNavigationIconClicked,
        navigationIconLabel = stringResource(R.string.back),
        incognitoMode = screenState.incognitoMode,
        scrollBehavior = scrollBehavior,
        actions = {
            AppBarActions(
                actions =
                    listOf(
                        AppBar.Action(
                            title = UiText.StringResource(R.string.settings),
                            icon = Icons.Outlined.Tune,
                            onClick = onSettingClick,
                        )
                    )
            )
        },
    )
}
