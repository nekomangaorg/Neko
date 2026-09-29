package eu.kanade.tachiyomi.ui.main

import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.ui.main.states.SideNavAlignment
import eu.kanade.tachiyomi.ui.main.states.SideNavMode
import java.io.File

data class MainScreenState(
    val incognitoMode: Boolean = false,
    val appUpdateResult: AppUpdateResult.NewUpdate? = null,
    val sideNavAlignment: SideNavAlignment = SideNavAlignment.Center,
    val sideNavMode: SideNavMode = SideNavMode.Default,
    val showWhatsNewDialog: Boolean = false,
    val pendingCrashReport: File? = null,
)
