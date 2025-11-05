package org.nekomanga.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Discord
import compose.icons.simpleicons.Github
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.LATEST_COMMIT_URL
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.data.updater.REPO_URL
import eu.kanade.tachiyomi.ui.main.states.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.states.ScreenBars
import eu.kanade.tachiyomi.ui.more.about.AboutScreenState
import eu.kanade.tachiyomi.ui.more.about.AboutViewModel
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.isOnline
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.constants.Constants.DISCORD_URL
import org.nekomanga.constants.Constants.PRIVACY_POLICY_URL
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.dialog.AppUpdateDialog
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.screens.about.AboutTopAppBar
import org.nekomanga.presentation.screens.settings.widgets.TextPreferenceWidget
import org.nekomanga.presentation.theme.Size

@Composable
fun AboutScreen(
    aboutViewModel: AboutViewModel,
    windowSizeClass: WindowSizeClass,
    onBackPressed: () -> Unit,
    onNavigateTo: (NavKey) -> Unit,
) {

    val screenState by aboutViewModel.aboutScreenState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    AboutWrapper(
        aboutScreenState = screenState,
        notOnlineSnackbar = aboutViewModel::notOnlineSnackbar,
        checkForUpdate = aboutViewModel::checkForUpdate,
        windowSizeClass = windowSizeClass,
        onVersionClicked = { context ->
            aboutViewModel.copyToClipboard()
            val deviceInfo = CrashLogUtil(context).getDebugInfo()
            val clipboard = context.getSystemService<ClipboardManager>()!!
            val appInfo = context.getString(R.string.app_info)
            clipboard.setPrimaryClip(ClipData.newPlainText(appInfo, deviceInfo))
        },
        onDownloadClicked = { url ->
            aboutViewModel.hideUpdateDialog()
            AppDownloadInstallJob.start(context, url, true)
        },
        onClickLicenses = { onNavigateTo(Screens.License) },
        onBackPressed = onBackPressed,
        dismissDialog = aboutViewModel::hideUpdateDialog,
    )
}

@Composable
private fun AboutWrapper(
    aboutScreenState: AboutScreenState,
    windowSizeClass: WindowSizeClass,
    notOnlineSnackbar: () -> Unit,
    checkForUpdate: () -> Unit,
    onVersionClicked: (Context) -> Unit,
    onDownloadClicked: (String) -> Unit,
    onClickLicenses: () -> Unit,
    onBackPressed: () -> Unit,
    dismissDialog: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val updateTopBar = LocalBarUpdater.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val screenBars = remember {
        ScreenBars(
            topBar = {
                AboutTopAppBar(
                    incognitoMode = aboutScreenState.incognitoMode,
                    scrollBehavior = scrollBehavior,
                    onNavigationClicked = onBackPressed,
                )
            },
            scrollBehavior = scrollBehavior,
        )
    }
    DisposableEffect(Unit) {
        updateTopBar(screenBars)
        onDispose { updateTopBar(ScreenBars(id = screenBars.id, topBar = null)) }
    }

    if (
        aboutScreenState.shouldShowUpdateDialog &&
            aboutScreenState.updateResult is AppUpdateResult.NewUpdate
    ) {
        AppUpdateDialog(
            release = (aboutScreenState.updateResult as AppUpdateResult.NewUpdate).release,
            onDismissRequest = dismissDialog,
            onConfirm = onDownloadClicked,
        )
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = Size.medium),
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
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
                                "Debug ${BuildConfig.COMMIT_SHA} (${aboutScreenState.buildTime})"
                            }

                            else -> {
                                "Stable ${BuildConfig.VERSION_NAME} (${aboutScreenState.buildTime})"
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
                            notOnlineSnackbar()
                        } else {
                            checkForUpdate()
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
        onClick = { uriHandler.openUri(url) },
    )
}
