package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.PreferenceScreen
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateNotifier
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.AppUpdateService
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.lang.toTimestampString
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.openInBrowser
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AboutController : SettingsController() {

    /**
     * Checks for new releases
     */
    private val updateChecker by lazy { AppUpdateChecker.getUpdateChecker() }

    private val userPreferences: PreferencesHelper by injectLazy()

    private val dateFormat: DateFormat by lazy {
        preferences.dateFormat()
    }

    private val isUpdaterEnabled = BuildConfig.INCLUDE_UPDATER

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.about

        preference {
            key = "pref_whats_new"
            titleRes = R.string.whats_new_this_release
            onClick {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    if (BuildConfig.DEBUG) {
                        "https://github.com/CarlosEsco/Neko/commits/master"
                    } else {
                        "https://github.com/CarlosEsco/Neko/releases/tag/${BuildConfig.VERSION_NAME}"
                    }.toUri()
                )
                startActivity(intent)
            }
        }
        if (isUpdaterEnabled) {
            preference {
                key = "pref_check_for_updates"
                titleRes = R.string.check_for_updates
                onClick {
                    if (activity!!.isOnline()) {
                        checkVersion()
                    } else {
                        activity!!.toast(R.string.no_network_connection)
                    }
                }
            }
        }
        preference {
            key = "pref_version"
            titleRes = R.string.version
            summary = if (BuildConfig.DEBUG) "r" + BuildConfig.COMMIT_COUNT
            else BuildConfig.VERSION_NAME
        }
        preference {
            key = "pref_build_time"
            titleRes = R.string.build_time
            summary = getFormattedBuildTime()
        }

        preferenceCategory {
            preference {
                key = "pref_about_website"
                titleRes = R.string.website
                "https://tachiyomi.org".also {
                    summary = it
                    onClick { openInBrowser(it) }
                }
            }

            preference {
                key = "pref_about_discord"
                title = "Discord"
                "https://discord.gg/tachiyomi".also {
                    summary = it
                    onClick { openInBrowser(it) }
                }
            }

            preference {
                key = "pref_about_github"
                title = "Github"
                "https://github.com/CarlosEsco/Neko".also {
                    summary = it
                    onClick { openInBrowser(it) }
                }
            }

            preference {
                key = "pref_about_twitter"
                title = "Twitter"
                "https://twitter.com/tachiyomiorg".also {
                    summary = it
                    onClick { openInBrowser(it) }
                }
            }

            preference {
                titleRes = R.string.similar_credit_title
                val url = "https://github.com/similar-manga/similar"
                summary = context.resources.getString(R.string.similar_credit_message, url)
                onClick {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    startActivity(intent)
                }
                isIconSpaceReserved = true
            }

            preference {
                titleRes = R.string.open_source_licenses

                onClick {
                    router.pushController(LicensesController().withFadeTransaction())
                }
            }
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private fun checkVersion() {
        if (activity == null) return

        activity?.toast(R.string.searching_for_updates)
        viewScope.launch {
            val result = try {
                updateChecker.checkForUpdate()
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    activity?.toast(error.message)
                    XLog.e(error)
                }
            }
            when (result) {
                is AppUpdateResult.NewUpdate<*> -> {
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
                        activity?.toast(R.string.no_new_updates_available)
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
            }
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
}
