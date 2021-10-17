package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.data.updater.AppUpdateResult

sealed class GithubAppUpdateResult : AppUpdateResult() {

    class NewUpdate(release: GithubRelease) : AppUpdateResult.NewUpdate<GithubRelease>(release)
    class NoNewUpdate : AppUpdateResult.NoNewUpdate()
}
