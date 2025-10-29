package eu.kanade.tachiyomi.ui.more.about

import eu.kanade.tachiyomi.data.updater.AppUpdateResult

data class AboutScreenState(
    val incognitoMode: Boolean = false,
    val checkingForUpdates: Boolean = false,
    val updateResult: AppUpdateResult = AppUpdateResult.NoNewUpdate,
    val shouldShowUpdateDialog: Boolean = false,
    val buildTime: String,
)
