package eu.kanade.tachiyomi.ui.more

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateNotifier
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.AppUpdateService
import eu.kanade.tachiyomi.ui.base.controller.BasicComposeController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.setting.LicensesController
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.lang.toTimestampString
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nekomanga.presentation.screens.AboutScreen
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AboutController : BasicComposeController() {

    private val preferences: PreferencesHelper by injectLazy()
    private val updateChecker by lazy { AppUpdateChecker() }

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    @Composable
    override fun ScreenContent() {
        val context = LocalContext.current
        AboutScreen(
            getFormattedBuildTime = this::getFormattedBuildTime,
            checkVersion = this::checkVersion,
            onVersionClicked = { this.copyVersionInfo(context) },
            onClickLicenses = { router.pushController(LicensesController().withFadeTransaction()) },
            onBackPressed = { activity?.onBackPressed() },
        )
    }

    private fun copyVersionInfo(context: Context) {
        val deviceInfo = CrashLogUtil(context).getDebugInfo()
        val clipboard = context.getSystemService<ClipboardManager>()!!
        val appInfo = context.getString(R.string.app_info)
        clipboard.setPrimaryClip(ClipData.newPlainText(appInfo, deviceInfo))
    }

    private fun getFormattedBuildTime(): String {
        try {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            inputDf.timeZone = TimeZone.getTimeZone("UTC")
            val buildTime = inputDf.parse(BuildConfig.BUILD_TIME) ?: return BuildConfig.BUILD_TIME

            return buildTime.toTimestampString(dateFormat)
        } catch (e: ParseException) {
            return BuildConfig.BUILD_TIME
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private fun checkVersion() {
        val activity = activity ?: return
        if (activity.isOnline().not()) {
            activity.toast(R.string.no_network_connection)
            return
        }

        activity.toast(R.string.searching_for_updates)
        viewScope.launch {
            val result = try {

                updateChecker.checkForUpdate(activity, true)
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    activity.toast(error.message)
                    XLog.e(error)
                }
            }
            when (result) {
                is AppUpdateResult.NewUpdate -> {
                    val body = result.release.info
                    val url = result.release.downloadLink

                    // Create confirmation window
                    withContext(Dispatchers.Main) {
                        AppUpdateNotifier.releasePageUrl = result.release.releaseLink
                        NewUpdateDialogController(body, url).showDialog(router)
                    }
                }
                is AppUpdateResult.NoNewUpdate -> {
                    withContext(Dispatchers.Main) {
                        activity.toast(R.string.no_new_updates_available)
                    }
                }
            }
        }
    }

    class NewUpdateDialogController(bundle: Bundle? = null) : DialogController(bundle) {

        constructor(body: String, url: String) : this(
            Bundle().apply {
                putString(BODY_KEY, body)
                putString(URL_KEY, url)
            },
        )

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val isOnA12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            return activity!!.materialAlertDialog()
                .setTitle(R.string.new_version_available)
                .setMessage(args.getString(BODY_KEY) ?: "")
                .setPositiveButton(if (isOnA12) R.string.update else R.string.download) { _, _ ->
                    val appContext = applicationContext
                    if (appContext != null) {
                        // Start download
                        val url = args.getString(URL_KEY) ?: ""
                        AppUpdateService.start(appContext, url, true)
                    }
                }
                .setNegativeButton(R.string.ignore, null)
                .create()
        }

        companion object {
            const val BODY_KEY = "NewUpdateDialogController.body"
            const val URL_KEY = "NewUpdateDialogController.key"
        }
    }
}


