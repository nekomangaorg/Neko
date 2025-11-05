package eu.kanade.tachiyomi.ui.main

import eu.kanade.tachiyomi.data.updater.AppUpdateResult

data class MainScreenState(
    val incognitoMode: Boolean = false,
    val appUpdateResult: AppUpdateResult.NewUpdate? = null,
)
