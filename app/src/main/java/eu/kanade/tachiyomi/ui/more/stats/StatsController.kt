package eu.kanade.tachiyomi.ui.more.stats

import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import org.nekomanga.presentation.screens.StatsScreen

class StatsController : BaseComposeController<StatsPresenter>() {

    override val presenter = StatsPresenter()

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)

        StatsScreen(
            statsState = presenter.simpleState.collectAsState(),
            detailedState = presenter.detailState.collectAsState(),
            windowSizeClass = windowSizeClass,
            onBackPressed = { activity?.onBackPressed() },
            onSwitchClick = presenter::switchState,
        )
    }
}
