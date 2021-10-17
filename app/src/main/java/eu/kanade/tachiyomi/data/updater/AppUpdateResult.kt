package eu.kanade.tachiyomi.data.updater

abstract class AppUpdateResult {

    open class NewUpdate<T : Release>(val release: T) : AppUpdateResult()
    open class NoNewUpdate : AppUpdateResult()
}
