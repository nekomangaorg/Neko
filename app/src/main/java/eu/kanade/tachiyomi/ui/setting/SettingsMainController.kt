package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.net.toUri
import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.googlematerial.OutlinedGoogleMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.FloatingSearchInterface
import eu.kanade.tachiyomi.ui.setting.search.SettingsSearchController
import eu.kanade.tachiyomi.util.system.create
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.withFadeTransaction

class SettingsMainController : SettingsController(), FloatingSearchInterface {

    init {
        setHasOptionsMenu(true)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.settings

        preference {
            iconDrawable =
                MaterialDesignDx.Icon.gmf_tune.create(context, colorAttr = R.attr.colorOnSurface)
            titleRes = R.string.general
            onClick { navigateTo(SettingsGeneralController()) }
        }

        preference {
            iconDrawable =
                OutlinedGoogleMaterial.Icon.gmo_palette.create(
                    context,
                    colorAttr = R.attr.colorOnSurface,
                )
            titleRes = R.string.appearance
            onClick { navigateTo(SettingsAppearanceController()) }
        }

        preference {
            iconDrawable =
                OutlinedGoogleMaterial.Icon.gmo_collections_bookmark.create(
                    context,
                    colorAttr = R.attr.colorOnSurface,
                )
            titleRes = R.string.library
            onClick { navigateTo(SettingsLibraryController()) }
        }
        preference {
            iconDrawable = CommunityMaterial.Icon2.cmd_google_chrome.create(
                context,
                colorAttr = R.attr.colorOnSurface,
            )
            titleRes = R.string.site_specific_settings
            onClick { navigateTo(SettingsSiteController()) }
        }

        preference {
            iconDrawable = CommunityMaterial.Icon3.cmd_source_merge.create(
                context,
                colorAttr = R.attr.colorOnSurface,
            )
            titleRes = R.string.merge_source_settings
            onClick { navigateTo(SettingsMergeController()) }
        }

        preference {
            iconDrawable =
                OutlinedGoogleMaterial.Icon.gmo_chrome_reader_mode.create(
                    context,
                    colorAttr = R.attr.colorOnSurface,
                )
            titleRes = R.string.reader
            onClick { navigateTo(SettingsReaderController()) }
        }
        preference {
            iconDrawable = OutlinedGoogleMaterial.Icon.gmo_download.create(
                context,
                colorAttr = R.attr.colorOnSurface,
            )
            titleRes = R.string.downloads
            onClick { navigateTo(SettingsDownloadController()) }
        }
        preference {
            iconDrawable =
                MaterialDesignDx.Icon.gmf_sync.create(context, colorAttr = R.attr.colorOnSurface)
            titleRes = R.string.tracking
            onClick { navigateTo(SettingsTrackingController()) }
        }
        preference {
            iconDrawable = MaterialDesignDx.Icon.gmf_settings_backup_restore.create(
                context,
                colorAttr = R.attr.colorOnSurface,
            )
            titleRes = R.string.backup_and_restore
            onClick { navigateTo(SettingsBackupController()) }
        }
        preference {
            iconDrawable = MaterialDesignDx.Icon.gmf_security.create(
                context,
                colorAttr = R.attr.colorOnSurface,
            )
            titleRes = R.string.security
            onClick { navigateTo(SettingsSecurityController()) }
        }
        preference {
            iconDrawable =
                MaterialDesignDx.Icon.gmf_code.create(context, colorAttr = R.attr.colorOnSurface)
            titleRes = R.string.advanced
            onClick { navigateTo(SettingsAdvancedController()) }
        }
        preference {
            iconDrawable =
                MaterialDesignDx.Icon.gmf_volunteer_activism.create(
                    context,
                    colorAttr = R.attr.colorOnSurface,
                )
            titleRes = R.string.dex_loot
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, "https://loot.moe/".toUri())
                startActivity(intent)
            }
        }
        this
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_main, menu)
        // Change hint to show global search.
        activityBinding?.searchToolbar?.searchQueryHint = applicationContext?.getString(R.string.search_settings)
    }

    override fun onActionViewExpand(item: MenuItem?) {
        SettingsSearchController.lastSearch = "" // reset saved search query
        router.pushController(
            RouterTransaction.with(SettingsSearchController()),
        )
    }

    private fun navigateTo(controller: Controller) {
        router.pushController(controller.withFadeTransaction())
    }
}
