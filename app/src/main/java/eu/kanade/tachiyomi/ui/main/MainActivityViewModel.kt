package eu.kanade.tachiyomi.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MainActivityViewModel : ViewModel() {

    private val _deepLinkScreen = MutableStateFlow<List<NavKey>?>(null)
    val deepLinkScreen: StateFlow<List<NavKey>?> = _deepLinkScreen.asStateFlow()

    fun setDeepLink(screens: List<NavKey>) {
        _deepLinkScreen.value = screens
    }

    fun consumeDeepLink() {
        _deepLinkScreen.value = null
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
    }

    fun toggleIncoginito() {
        viewModelScope.launch { securityPreferences.incognitoMode().toggle() }
    }
}
