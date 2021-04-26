package eu.kanade.tachiyomi.ui.base.presenter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

open class BaseCoroutinePresenter {
    var presenterScope = CoroutineScope(Job() + Dispatchers.Default)

    open fun onCreate() {
        presenterScope = CoroutineScope(Job() + Dispatchers.Default)
    }

    open fun onDestroy() {
        presenterScope.cancel()
    }
}
