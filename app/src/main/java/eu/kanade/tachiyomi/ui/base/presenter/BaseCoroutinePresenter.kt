package eu.kanade.tachiyomi.ui.base.presenter

import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

open class BaseCoroutinePresenter<T> {
    var presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    var pausablePresenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var weakView: WeakReference<T>? = null
    protected val view: T?
        get() = weakView?.get()

    /**
     * Attaches a view to the presenter.
     *
     * @param view a view to attach.
     */
    open fun attachView(view: T?) {
        weakView = WeakReference(view)
        if (!presenterScope.isActive) {
            presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
        if (!pausablePresenterScope.isActive) {
            pausablePresenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    }

    open fun onCreate() {}

    open fun onPause() {
        pausablePresenterScope.cancel()
    }

    open fun onResume() {
        if (!pausablePresenterScope.isActive) {
            pausablePresenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    }

    open fun onDestroy() {
        presenterScope.cancel()
        pausablePresenterScope.cancel()
        weakView = null
    }
}
