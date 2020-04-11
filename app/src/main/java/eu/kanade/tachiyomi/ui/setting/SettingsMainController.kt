package eu.kanade.tachiyomi.ui.setting

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.Controller
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser

class SettingsMainController : SettingsController() {

    init {
        setHasOptionsMenu(true)
    }

    private val size = 18

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.settings

        val tintColor = context.getResourceColor(R.attr.colorAccent)

        preference {
            iconRes = R.drawable.ic_tune_white_24dp
            iconTint = tintColor
            titleRes = R.string.general
            onClick { navigateTo(SettingsGeneralController()) }
        }
        preference {
            iconRes = R.drawable.ic_book_black_24dp
            iconTint = tintColor
            titleRes = R.string.library
            onClick { navigateTo(SettingsLibraryController()) }
        }
        preference {
            iconDrawable =
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_google_chrome).apply {
                    colorInt = tintColor
                    sizeDp = size
                }

            titleRes = R.string.site_specific_settings
            onClick { navigateTo(SettingsSiteController()) }
        }
        preference {
            iconRes = R.drawable.ic_read_24dp
            iconTint = tintColor
            titleRes = R.string.reader
            onClick { navigateTo(SettingsReaderController()) }
        }
        preference {
            iconRes = R.drawable.ic_file_download_black_24dp
            iconTint = tintColor
            titleRes = R.string.downloads
            onClick { navigateTo(SettingsDownloadController()) }
        }

        preference {
            iconRes = R.drawable.ic_sync_black_24dp
            iconTint = tintColor
            titleRes = R.string.tracking
            onClick { navigateTo(SettingsTrackingController()) }
        }
        preference {
            iconDrawable =
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_chart_histogram).apply {
                    colorInt = tintColor
                    sizeDp = size
                }

            titleRes = R.string.similar
            onClick { navigateTo(SettingsSimilarController()) }
        }
        preference {
            iconRes = R.drawable.ic_backup_black_24dp
            iconTint = tintColor
            titleRes = R.string.backup
            onClick { navigateTo(SettingsBackupController()) }
        }
        preference {
            iconRes = R.drawable.ic_code_black_24dp
            iconTint = tintColor
            titleRes = R.string.advanced
            onClick { navigateTo(SettingsAdvancedController()) }
        }
        preference {
            iconRes = R.drawable.ic_info_black_24dp
            iconTint = tintColor
            titleRes = R.string.about
            onClick { navigateTo(SettingsAboutController()) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_main, menu)
        menu.findItem(R.id.action_bug_report).isVisible = BuildConfig.DEBUG
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_help -> activity?.openInBrowser(URL_HELP)
            R.id.action_bug_report -> activity?.openInBrowser(URL_BUG_REPORT)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun navigateTo(controller: Controller) {
        router.pushController(controller.withFadeTransaction())
    }

    private companion object {
        private const val URL_HELP = "https://tachiyomi.org/help/"
        private const val URL_BUG_REPORT = "https://github.com/CarlosEsco/Neko/issues"
    }
}
