package eu.kanade.tachiyomi.ui.more.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.getSystemService
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.AppUpdateService
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.setting.LicensesController
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.AboutScreen

class AboutController(bundle: Bundle? = null) : BaseComposeController<AboutPresenter>(bundle) {

    override val presenter = AboutPresenter()

    @Composable
    override fun ScreenContent() {
        AboutScreen(
            aboutScreenState = presenter.aboutScreenState.collectAsState(),
            checkForUpdate = presenter::checkForUpdate,
            onDownloadClicked = { url ->
                presenter.hideUpdateDialog()
                this.downloadApp(url)
            },
            dismissDialog = presenter::hideUpdateDialog,
            snackbar = presenter.snackBarState,
            onVersionClicked = { context ->
                presenter.copyToClipboard()
                copyVersionInfo(context)
            },
            onClickLicenses = { router.pushController(LicensesController().withFadeTransaction()) },
            onBackPressed = { activity?.onBackPressed() },
        )
    }

    /**
     * Copy the Device and App info to clipboard
     */
    private fun copyVersionInfo(context: Context) {
        val deviceInfo = CrashLogUtil(context).getDebugInfo()
        val clipboard = context.getSystemService<ClipboardManager>()!!
        val appInfo = context.getString(R.string.app_info)
        clipboard.setPrimaryClip(ClipData.newPlainText(appInfo, deviceInfo))
    }

    /**
     * Start the process to download the update
     */
    private fun downloadApp(url: String) {
        applicationContext ?: return
        AppUpdateService.start(applicationContext!!, url, true)
    }
}
