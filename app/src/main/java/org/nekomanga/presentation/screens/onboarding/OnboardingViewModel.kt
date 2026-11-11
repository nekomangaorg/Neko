package org.nekomanga.presentation.screens.onboarding

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.theme.Themes
import tachiyomi.core.preference.Preference
import tachiyomi.core.util.storage.displayablePath
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class OnboardingViewModel(
    private val app: Application = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    storagePreferences: StoragePreferences = Injekt.get(),
) : ViewModel() {

    private val _eventChannel = Channel<OnboardingEvent>(Channel.BUFFERED)
    val eventChannel = _eventChannel.receiveAsFlow()

    val baseStorageDirectory: Preference<String> = storagePreferences.baseStorageDirectory()

    private val storageStepUiStateFlow =
        baseStorageDirectory
            .changes()
            .map { uriString ->
                withContext(Dispatchers.IO) {
                    StorageStepUiState(
                        currentStoragePath = getDisplayableStoragePath(uriString),
                        isStorageConfigured = baseStorageDirectory.isSet(),
                    )
                }
            }
            .flowOn(Dispatchers.IO)

    private val themeStepUiStateFlow =
        combine(
            preferences.lightTheme().changes(),
            preferences.darkTheme().changes(),
            preferences.nightMode().changes(),
        ) { lightTheme, darkTheme, nightMode ->
            ThemeStepUiState(
                lightTheme = lightTheme,
                darkTheme = darkTheme,
                nightMode = nightMode,
                followSystemTheme = nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            )
        }

    val uiState: StateFlow<OnboardingUiState> =
        combine(
                storageStepUiStateFlow,
                themeStepUiStateFlow,
            ) { storageState, themeState ->
                OnboardingUiState(
                    storageStepUiState = storageState,
                    themeStepUiState = themeState,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    OnboardingUiState(
                        storageStepUiState =
                            StorageStepUiState(
                                currentStoragePath =
                                    getDisplayableStoragePath(baseStorageDirectory.get()),
                                isStorageConfigured = baseStorageDirectory.isSet(),
                            ),
                        themeStepUiState =
                            ThemeStepUiState(
                                lightTheme = preferences.lightTheme().get(),
                                darkTheme = preferences.darkTheme().get(),
                                nightMode = preferences.nightMode().get(),
                                followSystemTheme =
                                    preferences.nightMode().get() ==
                                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                            ),
                    ),
            )

    fun onThemeSelected(theme: Themes, isDark: Boolean) {
        val currentTheme =
            if (isDark) preferences.darkTheme().get() else preferences.lightTheme().get()
        val isAlreadySelected = currentTheme == theme
        val followingSystem =
            preferences.nightMode().get() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        val nightMode =
            if (isDark) {
                preferences.darkTheme().set(theme)
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                preferences.lightTheme().set(theme)
                AppCompatDelegate.MODE_NIGHT_NO
            }

        if (followingSystem && isAlreadySelected) {
            preferences.nightMode().set(nightMode)
        } else if (!followingSystem) {
            preferences.nightMode().set(nightMode)
        }

        viewModelScope.launch { _eventChannel.send(OnboardingEvent.RecreateActivity) }
    }

    fun onFollowSystemToggle(follow: Boolean) {
        if (follow) {
            preferences.nightMode().set(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else {
            preferences.nightMode().set(app.appDelegateNightMode())
        }
        viewModelScope.launch { _eventChannel.send(OnboardingEvent.RecreateActivity) }
    }

    private fun getDisplayableStoragePath(uriString: String): String {
        if (uriString == baseStorageDirectory.defaultValue() || uriString.isBlank()) {
            return app.getString(R.string.no_location_set)
        }
        return try {
            UniFile.fromUri(app, uriString.toUri())?.displayablePath
                ?: app.getString(R.string.invalid_location, uriString)
        } catch (e: Exception) {
            TimberKt.e(e) { "Failed to get displayable storage path" }
            app.getString(R.string.invalid_location, uriString)
        }
    }
}
