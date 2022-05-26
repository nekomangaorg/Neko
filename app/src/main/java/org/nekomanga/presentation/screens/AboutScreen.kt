package org.nekomanga.presentation.screens

import ToolTipIconButton
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import eu.kanade.presentation.components.PreferenceRow
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.util.system.isOnline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.dialog.AppUpdateDialog
import org.nekomanga.presentation.components.snackbar.snackbarHost
import org.nekomanga.presentation.theme.Padding

@Composable
fun AboutScreen(
    getFormattedBuildTime: () -> String,
    checkForUpdate: (Context) -> Flow<AppUpdateResult>,
    onVersionClicked: (Context) -> Unit,
    onDownloadClicked: (String) -> Unit,
    onClickLicenses: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shouldShowDialog = remember { mutableStateOf(false) }
    var appUpdateResult: AppUpdateResult = AppUpdateResult.NoNewUpdate

    NekoScaffold(
        title = stringResource(id = R.string.about),
        onNavigationIconClicked = onBackPressed,
        snackBarHost = snackbarHost(snackbarHostState),
    ) { contentPadding ->

        if (shouldShowDialog.value && appUpdateResult is AppUpdateResult.NewUpdate) {
            val release = (appUpdateResult as AppUpdateResult.NewUpdate).release
            AppUpdateDialog(
                release = release, onDismissRequest = { shouldShowDialog.value = false },
                onConfirm = { url ->
                    onDownloadClicked(url)
                    shouldShowDialog.value = false
                },
            )
        }

        LazyColumn(contentPadding = contentPadding) {
            item {
                LogoHeader()
            }
            item {
                Spacer(modifier = Modifier.size(24.dp))
            }
            item {
                PreferenceRow(
                    title = stringResource(R.string.version),
                    subtitle = when {
                        BuildConfig.DEBUG -> {
                            "Debug ${BuildConfig.COMMIT_SHA} (${getFormattedBuildTime()})"
                        }
                        else -> {
                            "Stable ${BuildConfig.VERSION_NAME} (${getFormattedBuildTime()})"
                        }
                    },
                    onClick = {
                        onVersionClicked(context)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string._copied_to_clipboard, "Build information"),
                                withDismissAction = true,
                            )
                        }
                    },
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.check_for_updates),
                    onClick = {
                        if (context.isOnline().not()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.no_network_connection),
                                    withDismissAction = true,
                                )
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.searching_for_updates),
                                    withDismissAction = true,
                                )

                                checkForUpdate(context).collect { result ->
                                    when (result) {
                                        is AppUpdateResult.NewUpdate -> {
                                            XLog.i("should show dialog now")
                                            appUpdateResult = result
                                            shouldShowDialog.value = true
                                        }
                                        else -> {
                                            XLog.i("no update found")
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.no_new_updates_available),
                                                withDismissAction = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
            item {
                PreferenceRow(
                    title = stringResource(R.string.whats_new),
                    onClick = {
                        val url = if (BuildConfig.DEBUG) {
                            "https://github.com/CarlosEsco/Neko/commits/master"
                        } else {
                            RELEASE_URL
                        }
                        uriHandler.openUri(url)
                    },
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.open_source_licenses),
                    onClick = onClickLicenses,
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.privacy_policy),
                    onClick = { uriHandler.openUri("https://tachiyomi.org/privacy") },
                )
            }
            item {
                Spacer(modifier = Modifier.size(16.dp))
            }

            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.horizontalPadding),
                    mainAxisAlignment = FlowMainAxisAlignment.SpaceEvenly,
                ) {
                    val modifier = Modifier.size(24.dp)
                    LinkIcon(
                        label = stringResource(R.string.website),
                        modifier = modifier,
                        painter = rememberVectorPainter(Icons.Outlined.Public),
                        url = "https://tachiyomi.org",
                    )
                    LinkIcon(
                        label = "Discord",
                        modifier = modifier,
                        painter = painterResource(R.drawable.ic_discord_24dp),
                        url = "https://discord.gg/tachiyomi",
                    )
                    LinkIcon(
                        label = "Twitter",
                        modifier = modifier,
                        painter = painterResource(R.drawable.ic_twitter_24dp),
                        url = "https://twitter.com/tachiyomiorg",
                    )
                    LinkIcon(
                        label = "Facebook",
                        modifier = modifier,
                        painter = painterResource(R.drawable.ic_facebook_24dp),
                        url = "https://facebook.com/tachiyomiorg",
                    )
                    LinkIcon(
                        label = "Reddit",
                        modifier = modifier,
                        painter = painterResource(R.drawable.ic_reddit_24dp),
                        url = "https://www.reddit.com/r/Tachiyomi",
                    )
                    LinkIcon(
                        label = "GitHub",
                        modifier = modifier,
                        painter = painterResource(R.drawable.ic_github_24dp),
                        url = "https://github.com/CarlosEsco/neko",
                    )
                    LinkIcon(
                        label = "Tachiyomi",
                        modifier = modifier,
                        painter = painterResource(R.drawable.ic_tachi),
                        url = "https://github.com/tachiyomiorg",
                    )
                }
            }

        }
    }
}

@Composable
private fun LogoHeader() {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_neko_notification),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(200.dp),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun LinkIcon(
    modifier: Modifier = Modifier,
    label: String,
    painter: Painter,
    url: String,
) {
    val uriHandler = LocalUriHandler.current

    ToolTipIconButton(label, modifier = modifier, painter = painter, tint = MaterialTheme.colorScheme.primary.copy(alpha = .7f), buttonClicked = { uriHandler.openUri(url) })
}

