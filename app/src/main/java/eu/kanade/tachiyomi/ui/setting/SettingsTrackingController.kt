package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.asImmediateFlowIn
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.widget.preference.TrackLoginDialog
import eu.kanade.tachiyomi.widget.preference.TrackLogoutDialog
import eu.kanade.tachiyomi.widget.preference.TrackerPreference
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
            titleRes = R.string.update_tracking_after_reading
            defaultValue = true
        }
        switchPreference {
            key = Keys.trackMarkedAsRead
            titleRes = R.string.update_tracking_marked_read
            defaultValue = false
        }
        preferenceCategory {
            titleRes = R.string.services

            trackPreference(trackManager.myAnimeList) {
                activity?.openInBrowser(MyAnimeListApi.authUrl())
            }
            trackPreference(trackManager.aniList) {
                activity?.openInBrowser(AnilistApi.authUrl())
            }
            preference {
                key = "update_anilist_scoring"
                isPersistent = false
                isIconSpaceReserved = true
                title = context.getString(
                    R.string.update_tracking_scoring_type,
                    context.getString(R.string.anilist),
                )

                preferences.getStringPref(Keys.trackUsername(trackManager.aniList.id))
                    .asImmediateFlowIn(viewScope) {
                        isVisible = it.isNotEmpty()
                    }

                onClick {
                    viewScope.launchIO {
                        val (result, error) = trackManager.aniList.updatingScoring()
                        if (result) {
                            view?.snack(R.string.scoring_type_updated)
                        } else {
                            view?.snack(
                                context.getString(
                                    R.string.could_not_update_scoring_,
                                    error?.localizedMessage.orEmpty(),
                                ),
                            )
                        }
                    }
                }
            }
            trackPreference(trackManager.kitsu) {
                val dialog = TrackLoginDialog(trackManager.kitsu, R.string.email)
                dialog.targetController = this@SettingsTrackingController
                dialog.showDialog(router)
            }

            trackPreference(trackManager.mangaUpdates) {
                val dialog = TrackLoginDialog(trackManager.mangaUpdates, R.string.username)
                dialog.targetController = this@SettingsTrackingController
                dialog.showDialog(router)
            }
        }
    }

    private inline fun PreferenceGroup.trackPreference(
        service: TrackService,
        crossinline login: () -> Unit,
    ): TrackerPreference {
        return add(
            TrackerPreference(context).apply {
                key = Keys.trackUsername(service.id)
                title = context.getString(service.nameRes())
                iconRes = service.getLogo()
                iconColor = service.getLogoColor()
                onClick {
                    if (service.isLogged()) {
                        val dialog = TrackLogoutDialog(service)
                        dialog.targetController = this@SettingsTrackingController
                        dialog.showDialog(router)
                    } else {
                        login()
                    }
                }
            },
        )
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        updatePreference(trackManager.myAnimeList.id)
        updatePreference(trackManager.aniList.id)
    }

    private fun updatePreference(id: Int) {
        val pref = findPreference(Keys.trackUsername(id)) as? TrackerPreference
        pref?.notifyChanged()
    }

    override fun trackLoginDialogClosed(service: TrackService) {
        updatePreference(service.id)
    }

    override fun trackLogoutDialogClosed(service: TrackService) {
        updatePreference(service.id)
    }
}
