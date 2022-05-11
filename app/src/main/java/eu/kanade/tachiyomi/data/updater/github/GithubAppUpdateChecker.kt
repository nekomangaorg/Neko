package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import io.github.g00fy2.versioncompare.Version

class GithubAppUpdateChecker : AppUpdateChecker() {

    private val service: GithubService = GithubService.create()

    override suspend fun checkForUpdate(): AppUpdateResult {

        if (BuildConfig.DEBUG) {
            return GithubAppUpdateResult.NoNewUpdate()
        }

        val release = service.getLatestVersion()

        // Check if latest version is different from current version
        return if (Version(release.version).isHigherThan(BuildConfig.VERSION_NAME)) {
            GithubAppUpdateResult.NewUpdate(release)
        } else {
            GithubAppUpdateResult.NoNewUpdate()
        }
    }
}
