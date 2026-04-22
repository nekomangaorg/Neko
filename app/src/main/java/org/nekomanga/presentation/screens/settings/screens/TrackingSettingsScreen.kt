package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.jobs.tracking.TrackingSyncJob
import eu.kanade.tachiyomi.ui.setting.MergeLoginEvent
import eu.kanade.tachiyomi.ui.setting.TrackingSettingsViewModel
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.components.dialog.LoginDialog
import org.nekomanga.presentation.components.dialog.LogoutDialog
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class TrackingSettingsScreen(
    incognitoMode: Boolean,
    val preferences: PreferencesHelper,
    val trackingScreenState: TrackingSettingsViewModel.TrackingSettingsState,
    val updateAutoAddTrack: (Boolean, TrackServiceItem) -> Unit,
    val logout: (TrackServiceItem) -> Unit,
    val login: (TrackServiceItem, String, String) -> Unit,
    val loginEvent: SharedFlow<MergeLoginEvent>,
    onNavigationIconClick: (() -> Unit)?,
) : SearchableSettings(onNavigationIconClick, incognitoMode) {
    override fun getTitleRes(): Int = R.string.tracking

    @Composable
    override fun getPreferences(): PersistentList<Preference> {

        val context = LocalContext.current

        return (listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = preferences.autoUpdateTrack(),
                    title = stringResource(R.string.update_tracking_after_reading),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = preferences.trackMarkedAsRead(),
                    title = stringResource(R.string.update_tracking_marked_read),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = preferences.syncChaptersWithTracker(),
                    title = stringResource(R.string.mark_chapters_from_tracker),
                    subtitle = stringResource(R.string.mark_chapters_from_tracker_subtitle),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = preferences.autoTrackContentRatingSelections(),
                    title = stringResource(R.string.auto_track_content_rating_title),
                    subtitle = stringResource(R.string.auto_track_content_rating_summary),
                    entries =
                        mapOf(
                                MdConstants.ContentRating.safe to
                                    stringResource(R.string.content_rating_safe),
                                MdConstants.ContentRating.suggestive to
                                    stringResource(R.string.content_rating_suggestive),
                                MdConstants.ContentRating.erotica to
                                    stringResource(R.string.content_rating_erotica),
                                MdConstants.ContentRating.pornographic to
                                    stringResource(R.string.content_rating_pornographic),
                            )
                            .toPersistentMap(),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.refresh_tracking_metadata),
                    subtitle = stringResource(R.string.updates_tracking_details),
                    onClick = { TrackingSyncJob.doWorkNow(context) },
                ),
            ) + servicesGroup(context))
            .toPersistentList()
    }

    @Composable
    fun servicesGroup(context: Context): List<Preference.PreferenceGroup> {
        var trackServiceIdForLoginLogout by rememberSaveable { mutableStateOf<Int?>(null) }
        var trackServiceForLoginLogout =
            remember(trackServiceIdForLoginLogout, trackingScreenState) {
                when (trackServiceIdForLoginLogout) {
                    trackingScreenState.anilist.id -> trackingScreenState.anilist
                    trackingScreenState.kitsu.id -> trackingScreenState.kitsu
                    trackingScreenState.mal.id -> trackingScreenState.mal
                    trackingScreenState.mangaUpdates.id -> trackingScreenState.mangaUpdates
                    trackingScreenState.mangaBaka.id -> trackingScreenState.mangaBaka
                    else -> null
                }
            }
        var showLoginDialog by rememberSaveable { mutableStateOf(false) }

        var loginDialogUsernameLabel by rememberSaveable { mutableStateOf("") }

        val email = stringResource(R.string.email)
        val username = stringResource(R.string.username)

        if (showLoginDialog) {
            if (trackServiceForLoginLogout != null) {
                LoginDialog(
                    sourceName = stringResource(trackServiceForLoginLogout.nameRes),
                    loginEvent = loginEvent,
                    usernameLabel = loginDialogUsernameLabel,
                    onDismiss = {
                        trackServiceIdForLoginLogout = null
                        showLoginDialog = false
                    },
                    onConfirm = { username, password, _ ->
                        trackServiceForLoginLogout?.let { login(it, username, password) }
                    },
                )
            }
        }

        var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

        if (showLogoutDialog) {
            if (trackServiceForLoginLogout != null) {
                LogoutDialog(
                    sourceName = stringResource(trackServiceForLoginLogout.nameRes),
                    onDismiss = {
                        trackServiceIdForLoginLogout = null
                        showLogoutDialog = false
                    },
                    onConfirm = { trackServiceForLoginLogout?.let { logout(it) } },
                )
            }
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.anilist),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.TrackerPreference(
                            tracker = trackingScreenState.anilist,
                            title =
                                if (trackingScreenState.aniListIsLoggedIn)
                                    stringResource(R.string.sign_out)
                                else stringResource(R.string.sign_in),
                            subtitle =
                                if (trackingScreenState.aniListIsLoggedIn)
                                    trackingScreenState.anilistUsername
                                else null,
                            isLoggedIn = trackingScreenState.aniListIsLoggedIn,
                            login = { context.openInBrowser(trackingScreenState.aniListAuthUrl) },
                            logout = {
                                trackServiceIdForLoginLogout = trackingScreenState.anilist.id
                                showLogoutDialog = true
                            },
                        ),
                        Preference.PreferenceItem.BasicSwitchPreference(
                            title = stringResource(R.string.auto_track),
                            enabled = trackingScreenState.aniListIsLoggedIn,
                            checked = trackingScreenState.aniListAutoAddTrack,
                            onValueChanged = {
                                updateAutoAddTrack(it, trackingScreenState.anilist)
                                true
                            },
                        ),
                    ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.kitsu),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.TrackerPreference(
                            tracker = trackingScreenState.kitsu,
                            title =
                                if (trackingScreenState.kitsuIsLoggedIn)
                                    stringResource(R.string.sign_out)
                                else stringResource(R.string.sign_in),
                            subtitle =
                                if (trackingScreenState.kitsuIsLoggedIn)
                                    trackingScreenState.kitsuUsername
                                else null,
                            isLoggedIn = trackingScreenState.kitsuIsLoggedIn,
                            login = {
                                loginDialogUsernameLabel = email
                                trackServiceIdForLoginLogout = trackingScreenState.kitsu.id
                                showLoginDialog = true
                            },
                            logout = {
                                trackServiceIdForLoginLogout = trackingScreenState.kitsu.id
                                showLogoutDialog = true
                            },
                        ),
                        Preference.PreferenceItem.BasicSwitchPreference(
                            title = stringResource(R.string.auto_track),
                            enabled = trackingScreenState.kitsuIsLoggedIn,
                            checked = trackingScreenState.kitsuAutoAddTrack,
                            onValueChanged = {
                                updateAutoAddTrack(it, trackingScreenState.kitsu)
                                true
                            },
                        ),
                    ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.myanimelist),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.TrackerPreference(
                            tracker = trackingScreenState.mal,
                            title =
                                if (trackingScreenState.malIsLoggedIn)
                                    stringResource(R.string.sign_out)
                                else stringResource(R.string.sign_in),
                            subtitle =
                                if (trackingScreenState.malIsLoggedIn)
                                    trackingScreenState.malUsername
                                else null,
                            isLoggedIn = trackingScreenState.malIsLoggedIn,
                            login = { context.openInBrowser(trackingScreenState.malAuthUrl) },
                            logout = {
                                trackServiceIdForLoginLogout = trackingScreenState.mal.id
                                showLogoutDialog = true
                            },
                        ),
                        Preference.PreferenceItem.BasicSwitchPreference(
                            title = stringResource(R.string.auto_track),
                            enabled = trackingScreenState.malIsLoggedIn,
                            checked = trackingScreenState.malAutoAddTrack,
                            onValueChanged = {
                                updateAutoAddTrack(it, trackingScreenState.mal)
                                true
                            },
                        ),
                    ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.manga_updates),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.TrackerPreference(
                            tracker = trackingScreenState.mangaUpdates,
                            title =
                                if (trackingScreenState.mangaUpdatesIsLoggedIn)
                                    stringResource(R.string.sign_out)
                                else stringResource(R.string.sign_in),
                            subtitle =
                                if (trackingScreenState.mangaUpdatesIsLoggedIn)
                                    trackingScreenState.mangaUpdatesUsername
                                else null,
                            isLoggedIn = trackingScreenState.mangaUpdatesIsLoggedIn,
                            login = {
                                loginDialogUsernameLabel = username
                                trackServiceIdForLoginLogout = trackingScreenState.mangaUpdates.id
                                showLoginDialog = true
                            },
                            logout = {
                                trackServiceIdForLoginLogout = trackingScreenState.mangaUpdates.id
                                showLogoutDialog = true
                            },
                        ),
                        Preference.PreferenceItem.BasicSwitchPreference(
                            title = stringResource(R.string.auto_track),
                            enabled = trackingScreenState.mangaUpdatesIsLoggedIn,
                            checked = trackingScreenState.mangaUpdatesAutoAddTrack,
                            onValueChanged = {
                                updateAutoAddTrack(it, trackingScreenState.mangaUpdates)
                                true
                            },
                        ),
                    ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.mangabaka),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.TrackerPreference(
                            tracker = trackingScreenState.mangaBaka,
                            title =
                                if (trackingScreenState.mangaBakaIsLoggedIn)
                                    stringResource(R.string.sign_out)
                                else stringResource(R.string.sign_in),
                            subtitle =
                                if (trackingScreenState.mangaBakaIsLoggedIn)
                                    trackingScreenState.mangaBakaUsername
                                else null,
                            isLoggedIn = trackingScreenState.mangaBakaIsLoggedIn,
                            login = { context.openInBrowser(trackingScreenState.mangaBakaAuthUrl) },
                            logout = {
                                trackServiceIdForLoginLogout = trackingScreenState.mangaBaka.id
                                showLogoutDialog = true
                            },
                        )
                    ),
            ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(title = stringResource(R.string.update_tracking_after_reading)),
                SearchTerm(title = stringResource(R.string.update_tracking_marked_read)),
                SearchTerm(
                    title = stringResource(R.string.mark_chapters_from_tracker),
                    subtitle = stringResource(R.string.mark_chapters_from_tracker_subtitle),
                ),
                SearchTerm(
                    title = stringResource(R.string.auto_track_content_rating_title),
                    subtitle = stringResource(R.string.auto_track_content_rating_summary),
                ),
                SearchTerm(
                    title = stringResource(R.string.refresh_tracking_metadata),
                    subtitle = stringResource(R.string.updates_tracking_details),
                ),
                SearchTerm(title = stringResource(R.string.auto_track)),
                SearchTerm(title = stringResource(R.string.anilist)),
                SearchTerm(title = stringResource(R.string.kitsu)),
                SearchTerm(title = stringResource(R.string.myanimelist)),
                SearchTerm(title = stringResource(R.string.manga_updates)),
                SearchTerm(title = stringResource(R.string.mangabaka)),
            )
        }
    }
}
