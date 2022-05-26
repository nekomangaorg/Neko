package eu.kanade.tachiyomi.ui.more

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.core.content.getSystemService
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppUpdateService
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.setting.LicensesController
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.AboutScreen
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat

class AboutController(bundle: Bundle? = null) : BaseComposeController<AboutPresenter>(bundle) {

    private val preferences: PreferencesHelper by injectLazy()
    override val presenter = AboutPresenter()

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    @Composable
    override fun ScreenContent() {
        AboutScreen(
            getFormattedBuildTime = { this.presenter.getFormattedBuildTime(dateFormat) },
            checkForUpdate = { context -> presenter.checkForUpdate(context) },
            onDownloadClicked = this::downloadApp,
            onVersionClicked = { context -> this.copyVersionInfo(context) },
            onClickLicenses = { router.pushController(LicensesController().withFadeTransaction()) },
        ) { activity?.onBackPressed() }
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
    fun downloadApp(url: String) {
        applicationContext ?: return
        AppUpdateService.start(applicationContext!!, url, true)
    }
}


