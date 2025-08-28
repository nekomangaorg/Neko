package org.nekomanga.presentation.screens

import ToolTipButton
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Discord
import compose.icons.simpleicons.Github
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.LATEST_COMMIT_URL
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.data.updater.REPO_URL
import eu.kanade.tachiyomi.ui.more.about.AboutScreenState
import eu.kanade.tachiyomi.util.system.isOnline
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.constants.Constants.DISCORD_URL
import org.nekomanga.constants.Constants.PRIVACY_POLICY_URL
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.dialog.AppUpdateDialog
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.components.snackbar.snackbarHost
import org.nekomanga.presentation.screens.settings.widgets.TextPreferenceWidget
import org.nekomanga.presentation.theme.Size

@Composable
fun AboutScreen(
    aboutScreenState: State<AboutScreenState>,
    checkForUpdate: (Context) -> Unit,
    onVersionClicked: (Context) -> Unit,
    onDownloadClicked: (String) -> Unit,
    onClickLicenses: () -> Unit,
    onBackPressed: () -> Unit,
    dismissDialog: () -> Unit,
    snackbar: SharedFlow<SnackbarState>,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    NekoScaffold(
        type = NekoScaffoldType.Title,
        onNavigationIconClicked = onBackPressed,
        title = stringResource(id = R.string.about),
        snackBarHost = snackbarHost(snackbarHostState),
        content = { incomingContentPadding ->
            LaunchedEffect(snackbarHostState.currentSnackbarData) {
                snackbar.collect { state ->
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result =
                            snackbarHostState.showSnackbar(
                                message = state.getFormattedMessage(context),
                                actionLabel = state.getFormattedActionLabel(context),
                                withDismissAction = true,
                            )
                        when (result) {
                            SnackbarResult.ActionPerformed -> state.action?.invoke()
                            SnackbarResult.Dismissed -> state.dismissAction?.invoke()
                        }
                    }
                }
            }

            if (
                aboutScreenState.value.shouldShowUpdateDialog &&
                    aboutScreenState.value.updateResult is AppUpdateResult.NewUpdate
            ) {
                AppUpdateDialog(
                    release =
                        (aboutScreenState.value.updateResult as AppUpdateResult.NewUpdate).release,
                    onDismissRequest = dismissDialog,
                    onConfirm = onDownloadClicked,
                )
            }

            val recyclerContentPadding =
                PaddingValues(
                    top = incomingContentPadding.calculateTopPadding(),
                    bottom =
                        Size.navBarSize +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    start = Size.medium,
                    end = Size.medium,
                )

            LazyColumn(
                contentPadding = recyclerContentPadding,
                verticalArrangement = Arrangement.spacedBy(Size.extraTiny),
            ) {
                item { LogoHeader() }
                item { Spacer(modifier = Modifier.size(Size.large)) }
                item {
                    ExpressiveListCard(listCardType = ListCardType.Top) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.version),
                            subtitle =
                                when {
                                    BuildConfig.DEBUG -> {
                                        "Debug ${BuildConfig.COMMIT_SHA} (${aboutScreenState.value.buildTime})"
                                    }

                                    else -> {
                                        "Stable ${BuildConfig.VERSION_NAME} (${aboutScreenState.value.buildTime})"
                                    }
                                },
                            onPreferenceClick = { onVersionClicked(context) },
                        )
                    }
                }

                item {
                    ExpressiveListCard(listCardType = ListCardType.Center) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.check_for_updates),
                            onPreferenceClick = {
                                if (!context.isOnline()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.no_network_connection),
                                            withDismissAction = true,
                                        )
                                    }
                                } else {
                                    checkForUpdate(context)
                                }
                            },
                        )
                    }
                }
                item {
                    ExpressiveListCard(listCardType = ListCardType.Center) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.whats_new),
                            onPreferenceClick = {
                                val url =
                                    if (BuildConfig.DEBUG) {
                                        LATEST_COMMIT_URL
                                    } else {
                                        RELEASE_URL
                                    }
                                uriHandler.openUri(url)
                            },
                        )
                    }
                }

                item {
                    ExpressiveListCard(listCardType = ListCardType.Center) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.open_source_licenses),
                            onPreferenceClick = onClickLicenses,
                        )
                    }
                }

                item {
                    ExpressiveListCard(listCardType = ListCardType.Bottom) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.privacy_policy),
                            onPreferenceClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                        )
                    }
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(Size.medium),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val modifier = Modifier.size(Size.extraLarge)
                        LinkIcon(
                            label = "Discord",
                            modifier = modifier,
                            icon = SimpleIcons.Discord,
                            url = DISCORD_URL,
                        )
                        LinkIcon(
                            modifier = modifier,
                            label = "GitHub",
                            icon = SimpleIcons.Github,
                            url = REPO_URL,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun LogoHeader() {
    Column {
        Surface(modifier = Modifier.fillMaxWidth().padding(top = Size.huge)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_neko_yokai),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Size.extraExtraHuge * 2),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun LinkIcon(
    modifier: Modifier = Modifier,
    label: String,
    painter: Painter? = null,
    icon: ImageVector? = null,
    url: String,
) {
    val uriHandler = LocalUriHandler.current

    ToolTipButton(
        label,
        iconModifier = Modifier.size(24.dp),
        icon = icon,
        painter = painter,
        enabledTint = MaterialTheme.colorScheme.primary.copy(alpha = .7f),
        buttonClicked = { uriHandler.openUri(url) },
    )
}
