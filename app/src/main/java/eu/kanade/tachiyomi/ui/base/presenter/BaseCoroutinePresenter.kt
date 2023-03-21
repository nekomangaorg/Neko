package eu.kanade.tachiyomi.ui.base.presenter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.lang.ref.WeakReference

open class BaseCoroutinePresenter<T> {
    lateinit var presenterScope: CoroutineScope
    val isScopeInitialized get() = this::presenterScope.isInitialized
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
    }

    open fun onCreate() {
        if (!isScopeInitialized) {
            presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    }

    open fun onDestroy() {
        presenterScope.cancel()
        weakView = null
    }
}
