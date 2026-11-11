package org.nekomanga.presentation.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import org.nekomanga.presentation.theme.Themes

@Immutable
data class StorageStepUiState(
    val currentStoragePath: String = "",
    val isStorageConfigured: Boolean = false,
)

@Immutable
data class ThemeStepUiState(
    val lightTheme: Themes = Themes.Neko,
    val darkTheme: Themes = Themes.Neko,
    val nightMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val followSystemTheme: Boolean = true,
)

@Immutable
data class OnboardingUiState(
    val storageStepUiState: StorageStepUiState = StorageStepUiState(),
    val themeStepUiState: ThemeStepUiState = ThemeStepUiState(),
)

sealed interface OnboardingEvent {
    data object RecreateActivity : OnboardingEvent
}
