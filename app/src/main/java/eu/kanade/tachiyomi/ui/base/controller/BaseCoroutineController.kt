package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter

abstract class BaseCoroutineController<VB : ViewBinding, PS : BaseCoroutinePresenter<*>>(bundle: Bundle? = null) :
    BaseController<VB>(bundle) {

    abstract val presenter: PS
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        showToolbar()
        return super.onCreateView(inflater, container, savedViewState)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.takeView(this)
        presenter.onCreate()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <View> BaseCoroutinePresenter<View>.takeView(view: Any) = attachView(view as? View)

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        presenter.onDestroy()
    }
}
