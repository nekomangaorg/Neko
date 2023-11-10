package eu.kanade.tachiyomi.ui.more.stats

import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import org.nekomanga.presentation.screens.StatsScreen

class StatsController : BaseComposeController<StatsPresenter>() {

    override val presenter = StatsPresenter()

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)

        StatsScreen(
            statsState = presenter.simpleState.collectAsStateWithLifecycle(),
            detailedState = presenter.detailState.collectAsStateWithLifecycle(),
            windowSizeClass = windowSizeClass,
            onBackPressed = router::handleBack,
            onSwitchClick = presenter::switchState,
        )
    }
}
