package eu.kanade.tachiyomi.ui.base.presenter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

open class BaseCoroutinePresenter<T> {
    lateinit var presenterScope: CoroutineScope
    protected var controller: T? = null

    /**
     * Attaches a view to the presenter.
     *
     * @param view a view to attach.
     */
    open fun attachView(view: T?) {
        controller = view
    }

    open fun onCreate() {
        presenterScope = CoroutineScope(Job() + Dispatchers.Default)
    }

    open fun onDestroy() {
        presenterScope.cancel()
        controller = null
    }
}
