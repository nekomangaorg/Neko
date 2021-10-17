package eu.kanade.tachiyomi.data.updater.github

import com.g00fy2.versioncompare.Version
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult

class GithubAppUpdateChecker : AppUpdateChecker() {

    private val service: GithubService = GithubService.create()

    override suspend fun checkForUpdate(): AppUpdateResult {

        if (BuildConfig.DEBUG) {
            return GithubUpdateResult.NoNewUpdate()
        }

        val release = service.getLatestVersion()

        // Check if latest version is different from current version
        return if (newVersion != BuildConfig.VERSION_NAME) {
            GithubAppUpdateResult.NewUpdate(release)
        return if (Version(release.version).isHigherThan(BuildConfig.VERSION_NAME)) {
            GithubUpdateResult.NewUpdate(release)
        } else {
            GithubAppUpdateResult.NoNewUpdate()
        }
    }
}
