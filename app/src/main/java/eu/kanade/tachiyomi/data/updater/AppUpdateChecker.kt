package eu.kanade.tachiyomi.data.updater

import eu.kanade.tachiyomi.data.updater.github.GithubAppUpdateChecker

abstract class AppUpdateChecker {

    companion object {
        fun getUpdateChecker(): AppUpdateChecker = GithubAppUpdateChecker()
    }

    /**
     * Returns suspended result containing release information
     */
    abstract suspend fun checkForUpdate(): AppUpdateResult
}
