package eu.kanade.tachiyomi.ui.setting

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi
import eu.kanade.tachiyomi.util.system.launchIO
import kotlin.getValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toTrackServiceItem
import uy.kohesive.injekt.injectLazy

class TrackingSettingsViewModel : ViewModel() {

    val preferences: PreferencesHelper by injectLazy()

    private val trackManager: TrackManager by injectLazy()

    private val _loginEvent = MutableSharedFlow<MergeLoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    private val _state =
        MutableStateFlow(
            TrackingSettingsState(
                anilist = trackManager.aniList.toTrackServiceItem(),
                kitsu = trackManager.kitsu.toTrackServiceItem(),
                mal = trackManager.myAnimeList.toTrackServiceItem(),
                mangaUpdates = trackManager.mangaUpdates.toTrackServiceItem(),
            )
        )

    val state = _state.asStateFlow()

    init {
        viewModelScope.launchIO {
            preferences.autoAddTracker().changes().distinctUntilChanged().collectLatest { set ->
                _state.update {
                    it.copy(
                        aniListAutoAddTrack = set.contains(it.anilist.id.toString()),
                        kitsuAutoAddTrack = set.contains(it.kitsu.id.toString()),
                        malAutoAddTrack = set.contains(it.mal.id.toString()),
                        mangaUpdatesAutoAddTrack = set.contains(it.mangaUpdates.id.toString()),
                    )
                }
            }
        }

        launchTrackerUpdates(
            tracker = trackManager.aniList,
            updateUsername = { username -> _state.update { it.copy(anilistUsername = username) } },
            updateLoggedIn = { loggedIn -> _state.update { it.copy(aniListIsLoggedIn = loggedIn) } },
        )

        launchTrackerUpdates(
            tracker = trackManager.kitsu,
            updateUsername = { username -> _state.update { it.copy(kitsuUsername = username) } },
            updateLoggedIn = { loggedIn -> _state.update { it.copy(kitsuIsLoggedIn = loggedIn) } },
        )

        launchTrackerUpdates(
            tracker = trackManager.myAnimeList,
            updateUsername = { username -> _state.update { it.copy(malUsername = username) } },
            updateLoggedIn = { loggedIn -> _state.update { it.copy(malIsLoggedIn = loggedIn) } },
        )

        launchTrackerUpdates(
            tracker = trackManager.mangaUpdates,
            updateUsername = { username ->
                _state.update { it.copy(mangaUpdatesUsername = username) }
            },
            updateLoggedIn = { loggedIn ->
                _state.update { it.copy(mangaUpdatesIsLoggedIn = loggedIn) }
            },
        )
    }

    fun launchTrackerUpdates(
        tracker: TrackService,
        updateUsername: (String) -> Unit,
        updateLoggedIn: (Boolean) -> Unit,
    ) {
        viewModelScope.launchIO {
            tracker.getUsername().changes().distinctUntilChanged().collectLatest { username ->
                updateUsername(username)
            }
        }
        viewModelScope.launchIO {
            tracker.isLoggedInFlow().collectLatest { loggedIn -> updateLoggedIn(loggedIn) }
        }
    }

    fun login(trackServiceItem: TrackServiceItem, username: String, password: String) {
        viewModelScope.launchIO {
            val loginSuccessful =
                trackManager.getService(trackServiceItem.id)?.login(username, password) ?: false

            when (loginSuccessful) {
                true -> {
                    _loginEvent.emit(MergeLoginEvent.Success)
                }
                false -> {
                    _loginEvent.emit(MergeLoginEvent.Error)
                }
            }
        }
    }

    fun logout(trackServiceItem: TrackServiceItem) {
        viewModelScope.launchIO { trackManager.getService(trackServiceItem.id)?.logout() }
    }

    fun updateAutoAddTrack(enabled: Boolean, trackServiceItem: TrackServiceItem) {
        viewModelScope.launchIO {
            val autoAddTracker = preferences.autoAddTracker().get().toMutableSet()
            when (enabled) {
                true -> autoAddTracker.add(trackServiceItem.id.toString())
                false -> autoAddTracker.remove(trackServiceItem.id.toString())
            }
            preferences.autoAddTracker().set(autoAddTracker)
        }
    }

    data class TrackingSettingsState(
        val anilist: TrackServiceItem,
        val anilistUsername: String = "",
        val aniListIsLoggedIn: Boolean = false,
        val aniListAutoAddTrack: Boolean = false,
        val aniListAuthUrl: Uri = AnilistApi.authUrl(),
        val kitsu: TrackServiceItem,
        val kitsuUsername: String = "",
        val kitsuIsLoggedIn: Boolean = false,
        val kitsuAutoAddTrack: Boolean = false,
        val mal: TrackServiceItem,
        val malUsername: String = "",
        val malIsLoggedIn: Boolean = false,
        val malAutoAddTrack: Boolean = false,
        val malAuthUrl: Uri = MyAnimeListApi.authUrl(),
        val mangaUpdates: TrackServiceItem,
        val mangaUpdatesUsername: String = "",
        val mangaUpdatesIsLoggedIn: Boolean = false,
        val mangaUpdatesAutoAddTrack: Boolean = false,
    )
}
