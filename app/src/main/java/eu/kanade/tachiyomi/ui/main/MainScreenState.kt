package eu.kanade.tachiyomi.ui.main

import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.ui.main.states.SideNavAlignment

data class MainScreenState(
    val incognitoMode: Boolean = false,
    val appUpdateResult: AppUpdateResult.NewUpdate? = null,
    val sideNavAlignment: SideNavAlignment = SideNavAlignment.Center,
)
