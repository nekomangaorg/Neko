package org.nekomanga.presentation.screens.webview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kevinnzou.web.WebViewNavigator
import com.kevinnzou.web.WebViewState
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.functions.getTopAppBarColor

@Composable
fun WebviewTopBar(
    title: String = "",
    subtitle: String = "",
    navigator: WebViewNavigator,
    state: WebViewState,
    incognitoMode: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigationIconClicked: () -> Unit,
    onShare: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit,
    canOpenInApp: (String) -> Boolean,
    onOpenInApp: (String) -> Unit,
) {
    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)
    TitleTopAppBar(
        color = color,
        title = title,
        subtitle = subtitle,
        navigationIcon = Icons.AutoMirrored.Default.ArrowBack,
        onNavigationIconClicked = onNavigationIconClicked,
        navigationIconLabel = stringResource(R.string.back),
        incognitoMode = incognitoMode,
        scrollBehavior = scrollBehavior,
        actions = {
            AppBarActions(
                actions =
                    listOf(
                        if (navigator.canGoBack) {
                            AppBar.Action(
                                title = UiText.StringResource(R.string.back),
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                onClick = { navigator.navigateBack() },
                            )
                        } else {
                            AppBar.Empty
                        }
                    ) +
                        listOf(
                            if (navigator.canGoForward) {
                                AppBar.Action(
                                    title = UiText.StringResource(R.string.forward),
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    onClick = { navigator.navigateForward() },
                                )
                            } else {
                                AppBar.Empty
                            }
                        ) +
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.refresh),
                                onClick = { navigator.reload() },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.share),
                                onClick = { onShare(subtitle) },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.open_in_browser),
                                onClick = { onOpenInBrowser(subtitle) },
                            ),
                        ) +
                        listOf(
                            if (
                                navigator.canGoBack &&
                                    state.lastLoadedUrl != null &&
                                    canOpenInApp(state.lastLoadedUrl!!)
                            ) {
                                AppBar.OverflowAction(
                                    title = UiText.StringResource(R.string.open_in_app),
                                    onClick = { onOpenInApp(subtitle) },
                                )
                            } else {
                                AppBar.Empty
                            }
                        )
            )
        },
    )
}
