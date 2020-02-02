package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.ui.extension.ExtensionController
import uy.kohesive.injekt.injectLazy

class SettingsMainController : SettingsController() {
    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.label_settings

        val tintColor = context.getResourceColor(R.attr.colorAccent)

        extensionPreference {
            iconRes = R.drawable.ic_extension_black_24dp
            iconTint = tintColor
            titleRes = R.string.label_extensions
            onClick { navigateTo(ExtensionController()) }
        }

        preference {
            iconRes = R.drawable.ic_tune_black_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_general
            onClick { navigateTo(SettingsGeneralController()) }
        }
        preference {
            iconRes = R.drawable.ic_book_black_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_library
            onClick { navigateTo(SettingsLibraryController()) }
        }
        preference {
            iconRes = R.drawable.ic_chrome_reader_mode_black_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_reader
            onClick { navigateTo(SettingsReaderController()) }
        }
        preference {
            iconRes = R.drawable.ic_file_download_black_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_downloads
            onClick { navigateTo(SettingsDownloadController()) }
        }
        preference {
            iconRes = R.drawable.ic_sync_black_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_tracking
            onClick { navigateTo(SettingsTrackingController()) }
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
            titleRes = R.string.pref_category_advanced
            onClick { navigateTo(SettingsAdvancedController()) }
        }
        preference {
            iconRes = R.drawable.ic_help_black_24dp
            iconTint = tintColor
            titleRes = R.string.pref_category_about
            onClick { navigateTo(SettingsAboutController()) }
        }
    }

    private fun navigateTo(controller: Controller) {
        router.pushController(controller.withFadeTransaction())
    }
}
