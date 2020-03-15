package eu.kanade.tachiyomi.data.updater

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.github.GithubUpdateChecker
import rx.Observable

abstract class UpdateChecker {

    companion object {
        fun getUpdateChecker(): UpdateChecker =  GithubUpdateChecker()
    }

    /**
     * Returns observable containing release information
     */
    abstract fun checkForUpdate(): Observable<UpdateResult>

}
