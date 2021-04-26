package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter

abstract class BaseCoroutineController<VB : ViewBinding, PS : BaseCoroutinePresenter>(bundle: Bundle? = null) :
    BaseController<VB>(bundle) {

    abstract val presenter: PS
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.onCreate()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        presenter.onDestroy()
    }
}
