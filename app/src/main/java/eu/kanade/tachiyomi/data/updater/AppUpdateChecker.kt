package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.system.withIOContext
import io.github.g00fy2.versioncompare.Version
import java.util.Date
import java.util.concurrent.TimeUnit
import uy.kohesive.injekt.injectLazy

class AppUpdateChecker {

    private val networkService: NetworkHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    suspend fun checkForUpdate(
        context: Context,
        isUserPrompt: Boolean = false,
        doExtrasAfterNewUpdate: Boolean = true,
    ): AppUpdateResult {
        // Limit checks to once a day at most
        if (!isUserPrompt && Date().time < preferences.lastAppCheck()
            .get() + TimeUnit.DAYS.toMillis(1)
        ) {
            return AppUpdateResult.NoNewUpdate
        }

        return withIOContext {
            val result = networkService.client
                .newCall(GET(LATEST_RELEASE_URL))
                .await()
                .parseAs<GithubRelease>()
                .let {
                    preferences.lastAppCheck().set(Date().time)

                    // Check if latest version is different from current version
                    if (Version(it.version).isHigherThan(BuildConfig.VERSION_NAME)) {
                        AppUpdateResult.NewUpdate(it)
                    } else {
                        AppUpdateResult.NoNewUpdate
                    }
                }
            if (doExtrasAfterNewUpdate && result is AppUpdateResult.NewUpdate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    preferences.appShouldAutoUpdate() != AutoAppUpdaterJob.NEVER
                ) {
                    AutoAppUpdaterJob.setupTask(context)
                }
                AppUpdateNotifier(context).promptUpdate(
                    result.release.info,
                    result.release.downloadLink,
                    result.release.releaseLink,
                )
            }

            result
        }
    }
}

const val GITHUB_REPO: String = "CarlosEsco/Neko"
const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
const val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/${BuildConfig.VERSION_NAME}"
