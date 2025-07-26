package eu.kanade.tachiyomi.ui.setting

import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import org.nekomanga.presentation.screens.SettingsScreen

class SettingsController : BaseComposeController<SettingsPresenter>() {
    override val presenter = SettingsPresenter()

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)
        SettingsScreen(
            preferencesHelper = presenter.preferencesHelper,
            mangaDetailsPreferences = presenter.mangaDetailsPreferences,
            storagePreferences = presenter.storagePreferences,
            windowSizeClass = windowSizeClass,
            onBackPressed = { router.handleBack() },
        )
    }
}
