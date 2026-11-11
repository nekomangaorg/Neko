package org.nekomanga.presentation.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.theme.ThemeFollowSystemSwitch
import org.nekomanga.presentation.components.theme.ThemeSelector
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.Themes

internal class ThemeStep(
    private val uiState: ThemeStepUiState,
    private val onThemeSelected: (Themes, Boolean) -> Unit,
    private val onFollowSystemToggle: (Boolean) -> Unit,
) : OnboardingStep {
    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        ThemeStepContent(
            uiState = uiState,
            onThemeSelected = onThemeSelected,
            onFollowSystemToggle = onFollowSystemToggle,
        )
    }
}

@Composable
fun ThemeStepContent(
    uiState: ThemeStepUiState,
    onThemeSelected: (Themes, Boolean) -> Unit,
    onFollowSystemToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(Size.medium)) {
        ThemeSelector(
            currentTheme = uiState.lightTheme,
            darkThemeSelector = false,
            onThemeChange = { onThemeSelected(it, false) },
        )
        Gap(Size.small)
        ThemeSelector(
            currentTheme = uiState.darkTheme,
            darkThemeSelector = true,
            onThemeChange = { onThemeSelected(it, true) },
        )
        ThemeFollowSystemSwitch(
            modifier = Modifier.padding(vertical = Size.small),
            nightMode = uiState.nightMode,
            onToggle = onFollowSystemToggle,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeStepContentPreview() {
    ThemeStepContent(
        uiState =
            ThemeStepUiState(
                lightTheme = Themes.Neko,
                darkTheme = Themes.Neko,
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                followSystemTheme = true,
            ),
        onThemeSelected = { _, _ -> },
        onFollowSystemToggle = {},
    )
}
