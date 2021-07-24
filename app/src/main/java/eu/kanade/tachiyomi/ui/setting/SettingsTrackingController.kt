package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.widget.preference.LoginPreference
import eu.kanade.tachiyomi.widget.preference.TrackLoginDialog
import eu.kanade.tachiyomi.widget.preference.TrackLogoutDialog
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsTrackingController :
    SettingsController(),
    TrackLoginDialog.Listener,
    TrackLogoutDialog.Listener {

    private val trackManager: TrackManager by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.tracking

        switchPreference {
            key = Keys.autoUpdateTrack
            titleRes = R.string.sync_chapters_after_reading
            defaultValue = true
        }
        preferenceCategory {
            titleRes = R.string.services

            trackPreference(trackManager.myAnimeList) {
                activity?.openInBrowser(MyAnimeListApi.authUrl(),
                    trackManager.myAnimeList.getLogoColor())
            }
            trackPreference(trackManager.aniList) {
                activity?.openInBrowser(AnilistApi.authUrl(), trackManager.aniList.getLogoColor())
            }
            trackPreference(trackManager.kitsu) {
                val dialog = TrackLoginDialog(trackManager.kitsu, R.string.email)
                dialog.targetController = this@SettingsTrackingController
                dialog.showDialog(router)
            }
        }
    }

    private inline fun PreferenceScreen.trackPreference(
        service: TrackService,
        crossinline login: () -> Unit,
    ): LoginPreference {
        return initThenAdd(
            LoginPreference(context).apply {
                key = Keys.trackUsername(service.id)
                title = context.getString(service.nameRes())
            },
            {
                onClick {
                    if (service.isLogged()) {
                        val dialog = TrackLogoutDialog(service)
                        dialog.targetController = this@SettingsTrackingController
                        dialog.showDialog(router)
                    } else {
                        login()
                    }
                }
            }
        )
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        updatePreference(trackManager.myAnimeList.id)
        updatePreference(trackManager.aniList.id)
    }

    private fun updatePreference(id: Int) {
        val pref = findPreference(Keys.trackUsername(id)) as? LoginPreference
        pref?.notifyChanged()
    }

    override fun trackLoginDialogClosed(service: TrackService) {
        updatePreference(service.id)
    }

    override fun trackLogoutDialogClosed(service: TrackService) {
        updatePreference(service.id)
    }
}
