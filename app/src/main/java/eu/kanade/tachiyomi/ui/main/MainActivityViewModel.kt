package eu.kanade.tachiyomi.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.SimpleGithubRelease
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MainActivityViewModel : ViewModel() {

    private val updateChecker by lazy { AppUpdateChecker() }
    private val mangaShortcutManager: MangaShortcutManager by injectLazy()

    private val _deepLinkScreen = MutableStateFlow<List<NavKey>?>(null)
    val deepLinkScreen: StateFlow<List<NavKey>?> = _deepLinkScreen.asStateFlow()

    fun setDeepLink(screens: List<NavKey>) {
        _deepLinkScreen.value = screens
    }

    fun consumeDeepLink() {
        _deepLinkScreen.value = null
    }

    fun consumeAppUpdateResult() {
        _mainScreenState.update { it.copy(appUpdateResult = null) }
    }

    fun addAppUpdateResult(url: String, notes: String, releaseLink: String) {
        _mainScreenState.update {
            it.copy(
                appUpdateResult =
                    AppUpdateResult.NewUpdate(
                        SimpleGithubRelease(
                            downloadLink = url,
                            info = notes,
                            releaseLink = releaseLink,
                        )
                    )
            )
        }
    }

    val appSnackbarManager: AppSnackbarManager = Injekt.get()
    val securityPreferences: SecurityPreferences = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()

    private val _mainScreenState = MutableStateFlow(MainScreenState())
    val mainScreenState: StateFlow<MainScreenState> = _mainScreenState.asStateFlow()

    init {
        viewModelScope.launchIO {
            securityPreferences.incognitoMode().changes().collect { incognitoMode ->
                _mainScreenState.update { it.copy(incognitoMode = incognitoMode) }
            }
        }
        viewModelScope.launchIO {
            val update =
                runCatching { updateChecker.checkForUpdate() }
                    .getOrElse { error ->
                        AppUpdateResult.CantCheckForUpdate(error.message ?: "Error")
                    }

            if (update is AppUpdateResult.NewUpdate) {
                _mainScreenState.update { it.copy(appUpdateResult = update) }
            }
        }
    }

    fun toggleIncoginito() {
        viewModelScope.launch { securityPreferences.incognitoMode().toggle() }
    }

    fun saveExtras(currentTabIsLibrary: Boolean) {
        viewModelScope.launch {
            TimberKt.d { "Starting tab currentTabIsLibrary $currentTabIsLibrary" }
            val startingTab = if (currentTabIsLibrary) 0 else 1
            TimberKt.d { "Starting tab set to $startingTab" }
            preferences.lastUsedStartingTab().set(startingTab)
        }

        viewModelScope.launch {
            mangaShortcutManager.updateShortcuts()
            MangaCoverMetadata.savePrefs()
        }
    }
}
