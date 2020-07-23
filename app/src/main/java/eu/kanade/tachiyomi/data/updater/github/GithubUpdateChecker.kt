package eu.kanade.tachiyomi.data.updater.github

import com.g00fy2.versioncompare.Version
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.UpdateChecker
import eu.kanade.tachiyomi.data.updater.UpdateResult

class GithubUpdateChecker : UpdateChecker() {

    private val service: GithubService = GithubService.create()

    override suspend fun checkForUpdate(): UpdateResult {
        if (BuildConfig.DEBUG) {
            return GithubUpdateResult.NoNewUpdate()
        }
        val release = service.getLatestVersion()
        // Check if latest version is different from current version
        return if (Version(release.version).isHigherThan(BuildConfig.VERSION_NAME)) {
            GithubUpdateResult.NewUpdate(release)
        } else {
            GithubUpdateResult.NoNewUpdate()
        }
    }
}
